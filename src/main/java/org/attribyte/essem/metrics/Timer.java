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
import com.google.protobuf.ByteString;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramIterationValue;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * A timer that uses a high dynamic range histogram by default and allows the histogram to be
 * reset when a snapshot is reported.
 * @see <a href="https://github.com/HdrHistogram/HdrHistogram">HdrHistogram</a>.
 */
public class Timer extends com.codahale.metrics.Timer {

   public static void main(String[] args) throws Exception {

      System.out.println(TimeUnit.NANOSECONDS.convert(1, TimeUnit.MILLISECONDS));
      System.out.println(TimeUnit.NANOSECONDS.convert(1, TimeUnit.MICROSECONDS));


      Histogram histogram = new Histogram(1, 5001, 2);
      SecureRandom rnd = new SecureRandom();
      for(int i = 0; i < 100000; i++) {
         if(i %10 != 0) {
            histogram.recordValue(rnd.nextInt(5000));
         } else {
            histogram.recordValue(900);
         }
      }

      int estimatedBytes = histogram.getNeededByteBufferCapacity();
      System.out.println("estimated capacity is " + estimatedBytes);

      ByteBuffer buf = ByteBuffer.allocate(histogram.getNeededByteBufferCapacity());
      int compressedSize = histogram.encodeIntoCompressedByteBuffer(buf);
      System.out.println("compressed is " + compressedSize);

      ByteString str = ByteString.copyFrom(buf, compressedSize);
      buf.rewind();
      System.out.println("str length is " + str.size());

      histogram = Histogram.decodeFromCompressedByteBuffer(buf, 5001);

      /*
      ReportProtos.EssemReport.Histogram.Builder builder = ReportProtos.EssemReport.Histogram.newBuilder();
      EssemReporter.buildHistogram(new HDRHistogram.HDRSnapshot(histogram, histogram), builder);
      String protoResponse = TextFormat.printToString(builder.build());
      System.out.println(protoResponse);
      */

      System.out.println("percentiles...");


      Iterator<HistogramIterationValue> values = histogram.percentiles(5).iterator();
      while(values.hasNext()) {
         System.out.println(values.next());
      }

      System.out.println();
      System.out.println();

      int linearBucketUnits = (int)(histogram.getMaxValue() - histogram.getMinNonZeroValue())/10;
      values = histogram.linearBucketValues(linearBucketUnits).iterator();
      System.out.println("linear...");
      while(values.hasNext()) {
         System.out.println(values.next());
      }

      histogram.outputPercentileDistribution(System.out, 1.0);

   }

   /**
    * Creates a timer with no limit on trackable values and 2 significant value digits.
    */
   public Timer() {
      this(2, Clock.defaultClock());
   }

   /**
    * Creates a timer with highest trackable value and significant digits in the value specified.
    * @param highestTrackableValue The highest value tracked. Anything larger will be set to the maximum.
    * @param numberOfSignificantValueDigits The number of significant digits in the value.
    */
   public Timer(final long highestTrackableValue,
                final int numberOfSignificantValueDigits) {
      this(highestTrackableValue, numberOfSignificantValueDigits, Clock.defaultClock());
   }

   /**
    * Creates a timer with significant digits in the value specified.
    * @param numberOfSignificantValueDigits The number of significant digits in the value.
    */
   public Timer(final int numberOfSignificantValueDigits) {
      super(new HDRReservoir(numberOfSignificantValueDigits), Clock.defaultClock());
   }

   /**
    * Creates a timer with highest trackable value, significant digits in the value, and a clock specified.
    * @param highestTrackableValue The highest value tracked. Anything larger will be set to the maximum.
    * @param numberOfSignificantValueDigits The number of significant digits in the value.
    * @param clock The clock.
    */
   public Timer(final long highestTrackableValue,
                final int numberOfSignificantValueDigits,
                final Clock clock) {
      super(new HDRReservoir(highestTrackableValue, numberOfSignificantValueDigits), clock);
   }

   /**
    * Creates a timer with significant digits in the value and a clock specified.
    * @param numberOfSignificantValueDigits The number of significant digits in the value.
    * @param clock The clock.
    */
   public Timer(final int numberOfSignificantValueDigits,
                final Clock clock) {
      super(new HDRReservoir(numberOfSignificantValueDigits), clock);
   }
}