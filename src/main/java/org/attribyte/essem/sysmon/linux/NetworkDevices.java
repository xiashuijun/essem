/*
 * Copyright 2015 Attribyte, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package org.attribyte.essem.sysmon.linux;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Establish metrics for all network interfaces by parsing <code>/proc/net/dev</code>.
 * <p>
 *    Schedule an instance to run periodically to gather statistics for each interface.
 * </p>
 * @see <a href="http://man7.org/linux/man-pages/man5/proc.5.html">proc.5</a>
 */
public class NetworkDevices implements Runnable {

   /**
    * A network interface.
    */
   public static class Interface implements MetricSet {

      private Interface(final String name, final Iterable<String> keys, Iterator<String> lastRecordedValues) {
         this.name = name;
         ImmutableList.Builder<Meter> metersBuilder = ImmutableList.builder();
         ImmutableMap.Builder<String, Metric> mapBuilder = ImmutableMap.builder();
         for(String key : keys) {
            Meter meter = new Meter();
            metersBuilder.add(meter);
            mapBuilder.put(key, meter);
         }
         this.meters = metersBuilder.build();
         this.metricMap = mapBuilder.build();
         this.lastRecordedValues = Lists.newArrayListWithCapacity(meters.size());
         while(lastRecordedValues.hasNext()) {
            this.lastRecordedValues.add(new BigInteger(lastRecordedValues.next()));
         }
      }

      @Override
      public int hashCode() {
         return name.hashCode();
      }

      @Override
      public boolean equals(final Object other) {
         return other instanceof Interface && name.equals(other);
      }

      @Override
      public Map<String, Metric> getMetrics() {
         return metricMap;
      }

      private void mark(final Iterator<String> newValues) {
         List<BigInteger> nextRecordedValues = Lists.newArrayListWithCapacity(lastRecordedValues.size());
         Iterator<BigInteger> oldValIter = lastRecordedValues.iterator();
         Iterator<Meter> meterIter = meters.iterator();
         while(newValues.hasNext()) {
            BigInteger oldVal = oldValIter.next();
            BigInteger newVal = new BigInteger(newValues.next());
            nextRecordedValues.add(newVal);
            meterIter.next().mark(newVal.subtract(oldVal).longValue());
         }
         this.lastRecordedValues = nextRecordedValues;
      }

      /**
       * The interface name.
       */
      public final String name;

      private final ImmutableMap<String, Metric> metricMap;
      private final ImmutableList<Meter> meters;
      private List<BigInteger> lastRecordedValues;
   }

   /*
Inter-|   Receive                                                |  Transmit
 face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed

    */

   /*
   Inter-|   Receive                                                |  Transmit
    face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
       lo:  456216    3221    0    0    0     0          0         0   456216    3221    0    0    0     0       0          0
    wlan0:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0
     eth0: 33000242   44804    0    0    0     0          0      1111  5700898   26543    0    0    0     0       0          0
    */

   /**
    * Enumerate all network devices.
    * @throws IOException if network device metrics are unavailable.
    */
   public NetworkDevices() throws IOException {

      List<String> lines = Files.asCharSource(new File(PATH), Charsets.US_ASCII).readLines();

      if(lines.size() < 2) throw new IOException("Unknown format for '" + PATH + "'");

      Iterator<String> linesIter = lines.iterator();
      linesIter.next(); //Ignore

      Iterator<String> sections = sectionSplitter.split(linesIter.next()).iterator();

      if(!sections.hasNext()) throw new IOException("Unknown format for '" + PATH + "'");
      sections.next(); //Ignore 'face'

      if(!sections.hasNext()) throw new IOException("Unknown format for '" + PATH + "'");
      List<String> receiveKeys = Lists.newArrayList(tokenSplitter.split(sections.next()));

      if(!sections.hasNext()) throw new IOException("Unknown format for '" + PATH + "'");
      List<String> transmitKeys = Lists.newArrayList(tokenSplitter.split(sections.next()));

      List<String> keys = Lists.newArrayListWithCapacity(receiveKeys.size() + transmitKeys.size());
      for(String key : receiveKeys) keys.add("receive." + key);
      for(String key : transmitKeys) keys.add("transmit." + key);

      ImmutableMap.Builder<String, Interface> interfacesBuilder = ImmutableMap.builder();

      while(linesIter.hasNext()) {
         Iterator<String> values = tokenSplitter.split(linesIter.next()).iterator();
         String name = cleanName(values.next());
         interfacesBuilder.put(name, new Interface(name, keys, values));
      }
      this.interfaces = interfacesBuilder.build();
   }

   @Override
   public void run() {
      try {
         File pathFile = new File(PATH);
         if(!pathFile.exists()) return;

         Iterator<String> linesIter = Files.asCharSource(new File(PATH), Charsets.US_ASCII).readLines().iterator();
         linesIter.next();
         linesIter.next();
         while(linesIter.hasNext()) {
            Iterator<String> values = tokenSplitter.split(linesIter.next()).iterator();
            String name = cleanName(values.next());
            Interface iface = interfaces.get(name);
            if(iface != null) {
               iface.mark(values);
            }
         }
      } catch(IOException ioe) {
         ioe.printStackTrace();
      }
   }

   /**
    * An immutable map of all interfaces (vs name).
    */
   public final ImmutableMap<String, Interface> interfaces;

   private String cleanName(final String name) {
      if(name.endsWith(":")) {
         return name.substring(0, name.length() - 1).replace(':', '.');
      } else {
         return name.replace(':', '.');
      }
   }

   private static final Splitter sectionSplitter = Splitter.on('|').trimResults();
   private static final Splitter tokenSplitter = Splitter.on(CharMatcher.WHITESPACE).trimResults().omitEmptyStrings();
   private static final String PATH = "/proc/net/dev";
}
