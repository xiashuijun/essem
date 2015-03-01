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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.Map;

/**
 * Establish metrics for filesystems.
 */
public class Storage {

   /**
    * A filesystem: '/dev/sd1', 'tmpfs' etc.
    */
   public static class Filesystem implements MetricSet {

      private Filesystem(final FileStore store) {
         this.name = cleanName(store.name());
         this.type = store.type();
         ImmutableMap.Builder<String, Metric> builder = ImmutableMap.builder();

         builder.put("total", new Gauge<Long>() {
            public Long getValue() {
               try {
                  return store.getTotalSpace();
               } catch(IOException ioe) {
                  return 0L;
               }
            }
         });

         builder.put("usable", new Gauge<Long>() {
            public Long getValue() {
               try {
                  return store.getUsableSpace();
               } catch(IOException ioe) {
                  return 0L;
               }
            }
         });

         builder.put("unallocated", new Gauge<Long>() {
            public Long getValue() {
               try {
                  return store.getUnallocatedSpace();
               } catch(IOException ioe) {
                  return 0L;
               }
            }
         });

         this.metrics = builder.build();
      }

      @Override
      public Map<String, Metric> getMetrics() {
         return metrics;
      }

      /**
       * The filesystem name.
       */
      public final String name;

      /**
       * The filesystem type (ext4, tmpfs, etc).
       */
      public final String type;

      private final ImmutableMap<String, Metric> metrics;

      private String cleanName(final String name) {
         return name.startsWith("/") ? name.substring(1) : name;
      }
   }

   /**
    * Enumerate all filesystems.
    * @throws IOException
    */
   public Storage() throws IOException {

      FileSystem fileSystem = FileSystems.getDefault();
      ImmutableList.Builder<Filesystem> builder = ImmutableList.builder();
      for(FileStore store : fileSystem.getFileStores()) {
         Filesystem system = new Filesystem(store);
         builder.add(system);
      }

      this.filesystems = builder.build();
   }

   /**
    * An immutable map of all filesystems (vs name).
    */
   public final ImmutableList<Filesystem> filesystems;
}