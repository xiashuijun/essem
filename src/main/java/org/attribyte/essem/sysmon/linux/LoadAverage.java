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
import java.util.Iterator;
import java.util.Map;

/**
 * Expose values supplied by <code>/proc/loadavg</code> as gauges.
 * <p>
 *    Load averages are, "the number of jobs in the run queue (state R) or
 *    waiting for disk I/O (state D) averaged over 1, 5, and 15
 *    minutes." - http://man7.org/linux/man-pages/man5/proc.5.html
 * </p>
 */
public class LoadAverage implements MetricSet {

    /*
     /proc/loadavg
              The first three fields in this file are load average figures
              giving the number of jobs in the run queue (state R) or
              waiting for disk I/O (state D) averaged over 1, 5, and 15
              minutes.  They are the same as the load average numbers given
              by uptime(1) and other programs.  The fourth field consists of
              two numbers separated by a slash (/).  The first of these is
              the number of currently runnable kernel scheduling entities
              (processes, threads).  The value after the slash is the number
              of kernel scheduling entities that currently exist on the
              system.  The fifth field is the PID of the process that was
              most recently created on the system.
    */

   public static class CurrentLoadValues {

      private CurrentLoadValues() {
         File pathFile = new File(PATH);
         if(pathFile.exists()) {

            double _oneMinuteAverage = 0.0;
            double _fiveMinuteAverage = 0.0;
            double _fifteenMinuteAverage = 0.0;

            try {
               Iterator<String> strValues = tokenSplitter.split(Files.toString(pathFile, Charsets.US_ASCII)).iterator();
               _oneMinuteAverage = strValues.hasNext() ? Double.parseDouble(strValues.next()) : 0.0;
               _fiveMinuteAverage = strValues.hasNext() ? Double.parseDouble(strValues.next()) : 0.0;
               _fifteenMinuteAverage = strValues.hasNext() ? Double.parseDouble(strValues.next()) : 0.0;
            } catch(IOException|NumberFormatException e) {
               _oneMinuteAverage = 0.0;
               _fiveMinuteAverage = 0.0;
               _fifteenMinuteAverage = 0.0;
            }
            this.oneMinuteAverage = _oneMinuteAverage;
            this.fiveMinuteAverage = _fiveMinuteAverage;
            this.fifteenMinuteAverage = _fifteenMinuteAverage;
         } else {
            this.oneMinuteAverage = 0.0;
            this.fiveMinuteAverage = 0.0;
            this.fifteenMinuteAverage = 0.0;
         }
      }

      @Override
      public String toString() {
         return oneMinuteAverage + ", " + fiveMinuteAverage + ", " + fifteenMinuteAverage;
      }

      /**
       * The number of jobs in the run queue (state R) or
       * waiting for disk I/O (state D) averaged over one minute.
       */
      public final double oneMinuteAverage;

      /**
       * The number of jobs in the run queue (state R) or
       * waiting for disk I/O (state D) averaged over five minutes.
       */
      public final double fiveMinuteAverage;

      /**
       * The number of jobs in the run queue (state R) or
       * waiting for disk I/O (state D) averaged over fifteen minutes.
       */
      public final double fifteenMinuteAverage;

      private static final String PATH = "/proc/loadavg";
      private static final Splitter tokenSplitter = Splitter.on(CharMatcher.WHITESPACE).trimResults().omitEmptyStrings();
   }

   /**
    * Gets the current load values.
    * @return The load values.
    */
   public CurrentLoadValues getCurrentValues() {
      return new CurrentLoadValues();
   }

   @Override
   public Map<String, Metric> getMetrics() {
      return metrics;
   }

   private static final Gauge<Double> oneMinuteAverage = new Gauge<Double>() {
      @Override
      public Double getValue() {
         return gaugeLoad().oneMinuteAverage;
      }
   };

   private static final Gauge<Double> fiveMinuteAverage = new Gauge<Double>() {
      @Override
      public Double getValue() {
         return gaugeLoad().fiveMinuteAverage;
      }
   };

   private static final Gauge<Double> fifteenMinuteAverage = new Gauge<Double>() {
      @Override
      public Double getValue() {
         return gaugeLoad().fifteenMinuteAverage;
      }
   };

   private static final ImmutableMap<String, Metric> metrics = ImmutableMap.of(
           "one-minute-load-avg", (Metric)oneMinuteAverage,
           "five-minute-load-avg", (Metric)fiveMinuteAverage,
           "fifteen-minute-load-avg", (Metric)fifteenMinuteAverage
   );

   private static CurrentLoadValues gaugeLoad() {
      long currTime = System.currentTimeMillis();
      synchronized(loadLock) {
         if(lastValues == null || currTime - lastUpdateMillis > MIN_AGE_MILLIS) {
            lastUpdateMillis = currTime;
            lastValues = new CurrentLoadValues();
         }
         return lastValues;
      }
   }

   private static long lastUpdateMillis = 0L;
   private static CurrentLoadValues lastValues;

   private static final Object loadLock = new Object();
   private static final long MIN_AGE_MILLIS = 10000L;
}