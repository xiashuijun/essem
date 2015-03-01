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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Expose values supplied by <code>/proc/meminfo</code> as gauges.
 * <p>
 *    Schedule an instance to run periodically to gather statistics.
 * </p>
 * @see <a href="http://man7.org/linux/man-pages/man5/proc.5.html"></a>
 */
public class MemoryInfo implements MetricSet, Runnable {

    /*
     /proc/meminfo
        e.g.
        MemTotal:        8055664 kB
        MemFree:          740628 kB
    */

   /**
    * Holds a map of name, value pairs that describe the current memory state of the
    * system. The units are system-specific (?), but seems always to be 'kB'.
    */
   public static class CurrentMemValues {

      private CurrentMemValues() {
         File pathFile = new File(PATH);
         if(pathFile.exists()) {
            ImmutableMap<String, Long> _fields;
            try {
               List<String> lines = Files.asCharSource(new File(PATH), Charsets.US_ASCII).readLines();
               ImmutableMap.Builder<String, Long> builder = ImmutableMap.builder();
               for(String line : lines) {
                  Iterator<String> tok = tokenSplitter.split(line).iterator();
                  if(tok.hasNext()) {
                     String key = tok.next();
                     if(tok.hasNext()) {
                        long val = Long.parseLong(tok.next());
                        builder.put(key, val);
                     }
                  }
               }
               _fields = builder.build();
            } catch(IOException|NumberFormatException e) {
               _fields = ImmutableMap.of();

            }
            this.fields = _fields;
         } else {
            this.fields = ImmutableMap.of();
         }
      }

      public final ImmutableMap<String, Long> fields;
      private static final String PATH = "/proc/meminfo";
      private static final Splitter tokenSplitter = Splitter.on(CharMatcher.anyOf(": \t")).trimResults().omitEmptyStrings();
   }

   /**
    * Creates memory information with arbitrary keys.
    * @param registerKeys The names of the keys for which gauges are created.
    */
   public MemoryInfo(final Collection<String> registerKeys) {

      ImmutableMap.Builder<String, Metric> builder = ImmutableMap.builder();
      for(final String key : registerKeys) {
         builder.put(filterKey(key), new Gauge<Long>() {
            public Long getValue() {
               Long val = currValues.fields.get(key);
               return val != null ? val : 0L;
            }
         });
      }
      this.metrics = builder.build();
   }

   private String filterKey(final String key) {
      return key.toLowerCase();
   }

   @Override
   public void run() {
      this.currValues = new CurrentMemValues();
   }

   /**
    * Gets the current values.
    * @return The values.
    */
   public CurrentMemValues values() {
      return new CurrentMemValues();
   }

   @Override
   public Map<String, Metric> getMetrics() {
      return metrics;
   }

   private final ImmutableMap<String, Metric> metrics;

   private volatile CurrentMemValues currValues = new CurrentMemValues();


}