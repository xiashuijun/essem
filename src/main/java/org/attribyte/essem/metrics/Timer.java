/*
 * Copyright 2016 Attribyte, LLC
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
 */

package org.attribyte.essem.metrics;

import com.codahale.metrics.Clock;

/**
 * A timer that uses a high dynamic range histogram by default.
 * @see <a href="https://github.com/HdrHistogram/HdrHistogram">HdrHistogram</a>.
 */
public class Timer extends com.codahale.metrics.Timer {

   /**
    * Creates a timer that reports the histogram since last snapshot,
    * with no limit on trackable values and 2 significant value digits.
    */
   public Timer() {
      this(2, Clock.defaultClock(), HDRReservoir.REPORT_SNAPSHOT_HISTOGRAM);
   }

   /**
    * Creates a timer that reports the histogram since last snapshot,
    * with highest trackable value and significant digits in the value specified.
    * @param highestTrackableValue The highest value tracked. Anything larger will be set to the maximum.
    * @param numberOfSignificantValueDigits The number of significant digits in the value.
    */
   public Timer(final long highestTrackableValue,
                final int numberOfSignificantValueDigits) {
      this(highestTrackableValue, numberOfSignificantValueDigits, Clock.defaultClock(), HDRReservoir.REPORT_SNAPSHOT_HISTOGRAM);
   }

   /**
    * Creates a timer that reports the histogram since last snapshot, with significant digits in the value specified.
    * @param numberOfSignificantValueDigits The number of significant digits in the value.
    */
   public Timer(final int numberOfSignificantValueDigits) {
      super(new HDRReservoir(numberOfSignificantValueDigits, HDRReservoir.REPORT_SNAPSHOT_HISTOGRAM), Clock.defaultClock());
   }

   /**
    * Creates a timer with highest trackable value, significant digits in the value, a clock specified,
    * and if the total histogram should be reported instead of the one collected since last snapshot.
    * @param highestTrackableValue The highest value tracked. Anything larger will be set to the maximum.
    * @param numberOfSignificantValueDigits The number of significant digits in the value.
    * @param clock The clock.
    * @param reportTotalHistogram Should the total histogram (since start) be reported by {@code getSnapshot}?
    */
   public Timer(final long highestTrackableValue,
                final int numberOfSignificantValueDigits,
                final Clock clock,
                final boolean reportTotalHistogram) {
      super(new HDRReservoir(highestTrackableValue, numberOfSignificantValueDigits, reportTotalHistogram), clock);
   }

   /**
    * Creates a timer with significant digits in the value, a clock specified
    * and if the total histogram should be reported instead of the one collected since last snapshot.
    * @param numberOfSignificantValueDigits The number of significant digits in the value.
    * @param clock The clock.
    * @param reportTotalHistogram Should the total histogram (since start) be reported by {@code getSnapshot}?
    */
   public Timer(final int numberOfSignificantValueDigits,
                final Clock clock,
                final boolean reportTotalHistogram) {
      super(new HDRReservoir(numberOfSignificantValueDigits, reportTotalHistogram), clock);
   }
}