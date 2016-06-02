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

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.google.common.base.Charsets;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramIterationValue;
import org.HdrHistogram.Recorder;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import static java.lang.Math.toIntExact;

/**
 * A High Dynamic Range (HDR) histogram implementation.
 * @see <a href="https://github.com/HdrHistogram/HdrHistogram">HdrHistogram</a>.
 */
public class HDRReservoir implements Reservoir {


   /**
    * A specialized snapshot that contains both the "total" histogram (since start),
    * and the one collected since {@code getSnapshot} was last called.
    */
   public static final class HDRSnapshot extends Snapshot {

      /**
       * Creates the snapshot.
       * @param totalHistogram The total histogram.
       * @param lastSnapshotHistogram The histogram since the last snapshot.
       */
      HDRSnapshot(final Histogram totalHistogram,
                  final Histogram lastSnapshotHistogram) {
         this.totalHistogram = totalHistogram;
         this.lastSnapshotHistogram = lastSnapshotHistogram;
         this.histogram = totalHistogram;
      }

      private HDRSnapshot(final Histogram histogram, final Histogram totalHistogram,
                          final Histogram lastSnapshotHistogram) {
         this.histogram = histogram;
         this.totalHistogram = totalHistogram;
         this.lastSnapshotHistogram = lastSnapshotHistogram;
      }

      /**
       * Creates a snapshot from a HDR histogram.
       * @param histogram The histogram.
       * @return The snapshot.
       */
      public HDRSnapshot fromHistogram(final Histogram histogram) {
         return new HDRSnapshot(histogram, histogram, histogram);
      }

      /**
       * Gets the snapshot of the total histogram.
       * @return The snapshot.
       */
      public HDRSnapshot totalSnapshot() {
         return this.histogram == this.totalHistogram ? this :
                 new HDRSnapshot(this.totalHistogram, this.lastSnapshotHistogram, this.totalHistogram);
      }

      /**
       * Gets the snapshot that includes only values recorded since the last snapshot.
       * @return The snapshot.
       */
      public HDRSnapshot sinceLastSnapshot() {
         return this.histogram == this.lastSnapshotHistogram ? this :
                 new HDRSnapshot(this.totalHistogram, this.lastSnapshotHistogram, this.lastSnapshotHistogram);
      }

      /**
       * Gets the HDR histogram.
       * @return The histogram.
       */
      public Histogram getHistogram() {
         return histogram;
      }

      @Override
      public double getValue(double quantile) {
         return histogram.getValueAtPercentile(quantile * 100.0);
      }

      @Override
      public long[] getValues() {
         final int size = toIntExact(histogram.getTotalCount());
         final long[] values = new long[size];
         int pos = 0;
         for(HistogramIterationValue value : histogram.recordedValues()) {
            long recordedValue = value.getValueIteratedTo();
            long count = value.getCountAddedInThisIterationStep();
            for(int i = 0; i < count; i++) {
               values[pos++] = recordedValue;
            }
         }
         return values;
      }

      @Override
      public int size() {
         return toIntExact(histogram.getTotalCount());
      }

      @Override
      public long getMax() {
         return histogram.getMaxValue();
      }

      @Override
      public double getMean() {
         return histogram.getMean();
      }

      @Override
      public long getMin() {
         return histogram.getMinValue();
      }

      @Override
      public double getStdDev() {
         return histogram.getStdDeviation();
      }

      @Override
      public void dump(OutputStream output) {
         try(PrintWriter out = new PrintWriter(new OutputStreamWriter(output, Charsets.UTF_8))) {
            for(long value : getValues()) {
               out.printf("%d%n", value);
            }
         }
      }

      private final Histogram histogram;
      private final Histogram totalHistogram;
      private final Histogram lastSnapshotHistogram;
   }


   /**
    * Creates a HDR histogram.
    * @param numberOfSignificantValueDigits The number of significant digits in the value.
    * @param reportTotalHistogram If {@code true}, the long-running histogram will be reported. Otherwise, the histogram
    * collected since the last snapshot was acquired will be reported.
    */
   public HDRReservoir(final int numberOfSignificantValueDigits, final boolean reportTotalHistogram) {
      this.highestTrackableValue = Long.MAX_VALUE;
      this.recorder = new Recorder(numberOfSignificantValueDigits);
      this.totalHistogram = new Histogram(numberOfSignificantValueDigits);
      this.reportTotalHistogram = reportTotalHistogram;
   }

   /**
    * Creates a HDR histogram.
    * @param highestTrackableValue The highest value tracked. Anything larger will be set to the maximum.
    * @param numberOfSignificantValueDigits The number of significant digits in the value.
    * @param reportTotalHistogram If {@code true}, the long-running histogram will be reported. Otherwise, the histogram
    * collected since the last snapshot was acquired will be reported.
    */
   public HDRReservoir(final long highestTrackableValue, final int numberOfSignificantValueDigits,
                       final boolean reportTotalHistogram) {
      this.highestTrackableValue = highestTrackableValue;
      this.recorder = new Recorder(highestTrackableValue, numberOfSignificantValueDigits);
      this.totalHistogram = new Histogram(highestTrackableValue, numberOfSignificantValueDigits);
      this.reportTotalHistogram = reportTotalHistogram;
   }

   @Override
   public int size() {
      return getSnapshot().size();
   }

   @Override
   public void update(long value) {
      recorder.recordValue(value < this.highestTrackableValue ? value : this.highestTrackableValue);
   }

   @Override
   public synchronized Snapshot getSnapshot() {
      lastSnapshotHistogram = recorder.getIntervalHistogram(lastSnapshotHistogram);
      totalHistogram.add(lastSnapshotHistogram);
      HDRSnapshot snapshot = new HDRSnapshot(totalHistogram.copy(), lastSnapshotHistogram.copy());
      return reportTotalHistogram ? snapshot.totalSnapshot() : snapshot.sinceLastSnapshot();
   }

   /**
    * The recorder.
    */
   private final Recorder recorder;

   /**
    * The last reported histogram.
    */
   private Histogram lastSnapshotHistogram = null;

   /**
    * The totalHistogram.
    */
   private final Histogram totalHistogram;

   /**
    * The highest value tracked.
    */
   private final long highestTrackableValue;

   /**
    * If {@code true}, when {@code getSnapshot} is called, the total histogram (for all time) will be reported.
    * Otherwise, the histogram since the last call will be reported.
    */
   private final boolean reportTotalHistogram;

   /**
    * Indicates that the total histogram should be reported.
    */
   public static final boolean REPORT_TOTAL_HISTOGRAM = true;

   /**
    * Indicates that the snapshot histogram should be reported.
    */
   public static final boolean REPORT_SNAPSHOT_HISTOGRAM = false;
}