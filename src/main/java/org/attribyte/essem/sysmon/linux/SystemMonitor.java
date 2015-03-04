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


import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Expose system load, memory, network and storage stage as metrics.
 */
public class SystemMonitor implements MetricSet {

   /**
    * Creates a system monitor with default values.
    * @param pollFrequencySeconds The poll frequency in seconds.
    */
   public SystemMonitor(final int pollFrequencySeconds) {
      this(pollFrequencySeconds, DEFAULT_MEMINFO_KEYS, DEFAULT_STORAGE_TYPES, defaultInterfaceFilter, null);
   }

   /**
    * Creates a system monitor with default values.
    * @param pollFrequencySeconds The poll frequency in seconds.
    * @param logger A logger. May be <code>null</code>.
    */
   public SystemMonitor(final int pollFrequencySeconds,
                        final Logger logger) {
      this(pollFrequencySeconds, DEFAULT_MEMINFO_KEYS, DEFAULT_STORAGE_TYPES, defaultInterfaceFilter, logger);
   }

   /**
    * Creates a system monitor.
    * @param pollFrequencySeconds The poll frequency in seconds.
    * @param meminfoKeys The instrumented keys for memory info.
    * @param storageTypeFilter Filters only instrumented storage types.
    * @param networkDeviceFilter Filters only instrumented network devices.
    * @param logger A logger. May be <code>null</code>.
    */
   public SystemMonitor(final int pollFrequencySeconds,
                        final Collection<String> meminfoKeys,
                        final KeyFilter storageTypeFilter,
                        final KeyFilter networkDeviceFilter,
                        final Logger logger) {

      ImmutableMap.Builder<String, Metric> builder = ImmutableMap.builder();

      try {
         LoadAverage loadAverage = new LoadAverage();
         builder.putAll(loadAverage.getMetrics());
         scheduler.scheduleAtFixedRate(loadAverage, 0, pollFrequencySeconds, TimeUnit.SECONDS);
      } catch(IOException ioe) {
         if(logger != null) logger.error("Unable to instrument load averages", ioe);
      }

      try {
         MemoryInfo meminfo = new MemoryInfo(meminfoKeys);
         builder.put("memory", meminfo);
         scheduler.scheduleAtFixedRate(meminfo, 0, pollFrequencySeconds, TimeUnit.SECONDS);
      } catch(IOException ioe) {
         if(logger != null) logger.error("Unable to instrument memory", ioe);
      }

      try {
         NetworkDevices networkDevices = new NetworkDevices();
         Set<String> names = Sets.newHashSet();
         for(NetworkDevices.Interface iface : networkDevices.interfaces.values()) {
            if(networkDeviceFilter.accept(iface.name)) {
               if(!names.contains(iface.name)) {
                  builder.put("iface." + iface.name, iface);
                  names.add(iface.name);
               } else if(logger != null) {
                  logger.warn("Duplicate interface name: '" + iface.name + "'");
               }
            }
         }
         scheduler.scheduleAtFixedRate(networkDevices, 0, pollFrequencySeconds, TimeUnit.SECONDS);
      } catch(IOException ioe) {
         if(logger != null) logger.error("Unable to instrument network devices", ioe);
      }

      Set<String> acceptedDevices = Sets.newHashSet();

      try {
         Storage storage = new Storage();
         Set<String> names = Sets.newHashSet();
         for(Storage.Filesystem filesystem : storage.filesystems) {
            if(storageTypeFilter.accept(filesystem.type)) {
               if(!names.contains(filesystem.name)) {
                  builder.put(filesystem.name, filesystem);
                  names.add(filesystem.name);
                  String[] dev_name = filesystem.name.split("/");
                  if(dev_name.length == 2) {
                     acceptedDevices.add(dev_name[1]);
                  } else {
                     acceptedDevices.add(filesystem.name);
                  }
               } else if(logger != null) {
                  logger.warn("Duplicate filesystem name: '" + filesystem.name+"'");
               }
            }
         }
      } catch(IOException ioe) {
         if(logger != null) logger.error("Unable to instrument storage devices", ioe);
      }

      try {
         BlockDevices blockDevices = new BlockDevices();
         scheduler.scheduleAtFixedRate(blockDevices, 0, pollFrequencySeconds, TimeUnit.SECONDS);
         for(BlockDevices.BlockDevice device : blockDevices.devices) {
            if(acceptedDevices.contains(device.name)) {
               builder.put("blockdev."+device.name, device);
            }
         }
      } catch(IOException ioe) {
         if(logger != null) logger.error("Unable to instrument block devices", ioe);
      }

      this.metrics = builder.build();
   }

   @Override
   public Map<String, Metric> getMetrics() {
      return metrics;
   }

   /**
    * Shutdown the monitor.
    */
   public void shutdown() {
      scheduler.shutdown();
   }

   private final ImmutableMap<String, Metric> metrics;

   private final ScheduledExecutorService scheduler =
           MoreExecutors.getExitingScheduledExecutorService(
                   new ScheduledThreadPoolExecutor(1,
                   new ThreadFactoryBuilder().setNameFormat("essem-system-monitor-%d").build())
           );

   /**
    * The default keys instrumented for memory information.
    */
   public static final ImmutableList<String> DEFAULT_MEMINFO_KEYS =
           ImmutableList.of("MemTotal", "MemFree", "Buffers", "Cached", "SwapCached", "Active", "Inactive", "SwapTotal", "SwapFree");

   /**
    * The filesystem storage types instrumented by default.
    */
   public static final KeyFilter DEFAULT_STORAGE_TYPES =
           new KeyFilter.AcceptSet(
                   ImmutableSet.of("minix", "ext", "ext2", "ext3", "ext4",
                   "Reiserfs", "XFS", "JFS",  "xia",  "msdos",  "umsdos",  "vfat",  "ntfs",  "nfs",
                   "iso9660", "hpfs", "sysv", "smb", "ncpfs")
           );

   /**
    * The default interface filter.
    * <p>
    *    Includes 'eth*', 'lo', 'bond*', 'wlan*'
    * </p>
    */
   static final KeyFilter defaultInterfaceFilter = new KeyFilter() {
      @Override
      public boolean accept(final String str) {

         if(str.equals("lo")) {
            return true;
         } else if(str.startsWith("eth") ||
                 str.startsWith("bond") ||
                 str.startsWith("wlan")) {
            return true;
         } else {
            return false;
         }
      }
   };
}