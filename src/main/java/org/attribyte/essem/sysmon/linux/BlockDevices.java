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
import com.google.common.collect.Maps;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Establish metrics for block devices like disks.
 */
public class BlockDevices implements Runnable {

   /**
    * Metrics for a block device.
    */
   public static class BlockDevice implements MetricSet {

      private static final int NAME_INDEX = 2;

      private static final int PHYSICAL_READS_INDEX = 3;
      private static final int READ_BYTES_INDEX = 5;

      private static final int PHYSICAL_WRITES_INDEX = 7;
      private static final int WRITE_BYTES_INDEX = 9;

      private static final int MIN_EXPECTED_TOKENS = 10;

      private BlockDevice(final List<String> tokens, final int blockSize) {

         this.name = tokens.get(NAME_INDEX).trim();
         this.blockSize = blockSize;

         lastPhysicalReads = getValue(tokens, PHYSICAL_READS_INDEX);
         lastReadBytes = getValue(tokens, READ_BYTES_INDEX) * blockSize;

         lastPhysicalWrites = getValue(tokens, PHYSICAL_WRITES_INDEX);
         lastWriteBytes = getValue(tokens, WRITE_BYTES_INDEX) * blockSize;
      }

      private void mark(final List<String> tokens) {

         long physicalReads = getValue(tokens, PHYSICAL_READS_INDEX);
         long readBytes = getValue(tokens, READ_BYTES_INDEX) * blockSize;

         long physicalWrites = getValue(tokens, PHYSICAL_WRITES_INDEX);
         long writeBytes = getValue(tokens, WRITE_BYTES_INDEX) * blockSize;

         physicalReadsMeter.mark(physicalReads - this.lastPhysicalReads);
         readBytesMeter.mark(readBytes - this.lastReadBytes);

         physicalWritesMeter.mark(physicalWrites - this.lastPhysicalWrites);
         writeBytesMeter.mark(writeBytes - this.lastWriteBytes);

         this.lastPhysicalReads = physicalReads;
         this.lastReadBytes = readBytes;
         this.lastPhysicalWrites = physicalWrites;
         this.lastWriteBytes = writeBytes;
      }

      private long getValue(final List<String> tokens, final int index) {
         try {
            return Long.parseLong(tokens.get(index));
         } catch(NumberFormatException nfe) {
            return 0L;
         }
      }

      /**
       * The device name.
       */
      public final String name;

      /**
       * The block size.
       */
      public final long blockSize;

      @Override
      public Map<String, Metric> getMetrics() {
         return meters;
      }

      private final Meter physicalReadsMeter = new Meter();
      private final Meter readBytesMeter = new Meter();

      private final Meter physicalWritesMeter = new Meter();
      private final Meter writeBytesMeter = new Meter();

      private final ImmutableMap<String, Metric> meters =
              ImmutableMap.of("read-ops", (Metric)physicalReadsMeter,
                      "bytes-read", (Metric)readBytesMeter,
                      "write-ops", (Metric)physicalWritesMeter,
                      "bytes-written", (Metric)writeBytesMeter);

      private long lastPhysicalReads;
      private long lastReadBytes;

      private long lastPhysicalWrites;
      private long lastWriteBytes;

      /*

      https://www.kernel.org/doc/Documentation/iostats.txt
      http://www.percona.com/doc/percona-toolkit/2.1/pt-diskstats.html

      8 1 sda1 426 243 3386 2056 3 0 18 87 0 2135 2142

      The fields in this sample are as follows. The first three fields are the major and minor device numbers (8, 1),
      and the device name (sda1). They are followed by 11 fields of statistics:

      3 The number of reads completed. This is the number of physical reads done by the underlying disk, not the number
      of reads that applications made from the block device. This means that 426 actual reads have completed
      successfully to the disk on which /dev/sda1 resides. Reads are not counted until they complete.

      4 The number of reads merged because they were adjacent. In the sample, 243 reads were merged.
      This means that /dev/sda1 actually received 869 logical reads, but sent only 426 physical
      reads to the underlying physical device.

      5 The number of sectors read successfully. The 426 physical reads to the disk read 3386 sectors.
      Sectors are 512 bytes, so a total of about 1.65MB have been read from /dev/sda1.

      6 The number of milliseconds spent reading. This counts only reads that have completed,
      not reads that are in progress. It counts the time spent from when requests are placed on the queue
      until they complete, not the time that the underlying disk spends servicing the requests.
      That is, it measures the total response time seen by applications, not disk response times.

      7 Ditto for field 1, but for writes.

      8 Ditto for field 2, but for writes.

      9 Ditto for field 3, but for writes.

      10 Ditto for field 4, but for writes.

      11 The number of I/Os currently in progress, that is, they’ve been scheduled by the queue scheduler and issued to
      the disk (submitted to the underlying disk’s queue), but not yet completed.
      There are bugs in some kernels that cause this number, and thus fields 10 and 11, to be wrong sometimes.

      12 The total number of milliseconds spent doing I/Os. This is not the total response time seen by the applications;
      it is the total amount of time during which at least one I/O was in progress. I
      f one I/O is issued at time 100, another comes in at 101, and both of them complete at 102,
      then this field increments by 2, not 3.

      13 This field counts the total response time of all I/Os. In contrast to field 10, it counts double when two I/Os overlap.
      In our previous example, this field would increment by 3, not 2.

      cat /sys/block/sda/queue/physical_block_size 512
          /sys/block/sda/queue/logical_block_size 512
      */
   }

   /**
    * Creates block device metrics.
    * @throws IOException If block device metrics are unavailable.
    */
   public BlockDevices() throws IOException {

      List<String> lines = Files.asCharSource(new File(PATH), Charsets.US_ASCII).readLines();
      ImmutableList.Builder<BlockDevice> builder = ImmutableList.builder();
      for(String line : lines) {
         List<String> tokens = Lists.newArrayList(lineSplitter.split(line));
         if(tokens.size() >= BlockDevice.MIN_EXPECTED_TOKENS) {
            String name = tokens.get(BlockDevice.NAME_INDEX);
            builder.add(new BlockDevice(tokens, getBlockSize(name)));
         }
      }
      devices = builder.build();
   }

   @Override
   public void run() {
      try {
         List<String> lines = Files.asCharSource(new File(PATH), Charsets.US_ASCII).readLines();
         Map<String, List<String>> tokenMap = Maps.newHashMapWithExpectedSize(lines.size());
         for(String line : lines) {
            List<String> tokens = Lists.newArrayList(lineSplitter.split(line));
            String name = tokens.get(BlockDevice.NAME_INDEX);
            tokenMap.put(name, tokens);
         }

         for(BlockDevice device : devices) {
            List<String> tokens = tokenMap.get(device.name);
            if(tokens != null) {
               device.mark(tokens);
            }
         }
      } catch(IOException ioe) {
         ioe.printStackTrace();
      }
   }

   private static final String PATH = "/proc/diskstats";

   private static final Splitter lineSplitter = Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings().trimResults();

   /**
    * Gets the block size for a device.
    * @param name The device name.
    * @return The block size.
    */
   private static int getBlockSize(final String name) {

      File file = new File("/sys/block/" + name + "/queue/physical_block_size");
      if(!file.exists()) {
         int end = name.length() - 1;
         while(end > 0 && Character.isDigit(name.charAt(end))) end--;
         file = new File("/sys/block/" + name.substring(0, end + 1) +
                 "/queue/physical_block_size");
      }

      if(!file.exists()) {
         return 0;
      }

      try {
         return Integer.parseInt(Files.toString(file, Charsets.US_ASCII).trim());
      } catch(IOException ioe) {
         return 0;
      } catch(NumberFormatException nfe) {
         nfe.printStackTrace();
         return 0;
      }
   }

   /**
    * The list of block devices.
    */
   public final ImmutableList<BlockDevice> devices;
}
