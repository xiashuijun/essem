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

package com.attribyte.essem.model.index;

import com.codahale.metrics.RatioGauge;
import com.fasterxml.jackson.databind.JsonNode;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.text.NumberFormat;

public class IndexStats {

   /**
    * Creates index stats from JSON.
    * @param statsObj The object containing index stats.
    */
   public IndexStats(final JsonNode statsObj, final JsonNode clusterStatsObj) {

      JsonNode docsObj = statsObj.get("docs");
      this.docCount = docsObj != null && docsObj.has("count") ? docsObj.get("count").asLong() : 0L;
      this.deletedCount = docsObj != null && docsObj.has("deleted") ? docsObj.get("deleted").asLong() : 0L;

      JsonNode storeObj = statsObj.get("store");
      this.storedSizeBytes = storeObj != null && storeObj.has("size_in_bytes") ? storeObj.get("size_in_bytes").asLong() : 0L;

      JsonNode fielddataObj = statsObj.get("fielddata");
      this.fieldDataSizeBytes = fielddataObj != null && fielddataObj.has("memory_size_in_bytes") ? fielddataObj.get("memory_size_in_bytes").asLong() : 0L;

      JsonNode segmentsObj = statsObj.get("segments");
      this.segmentCount = segmentsObj != null && segmentsObj.has("count") ? segmentsObj.get("count").asLong() : 0L;
      this.segmentMemorySizeBytes = segmentsObj != null && segmentsObj.has("memory_in_bytes") ? segmentsObj.get("memory_in_bytes").asLong() : 0L;

      JsonNode memObj = clusterStatsObj.path("nodes").path("jvm").path("mem");
      if(!memObj.isMissingNode()) {
         this.esHeapUsedBytes = memObj.get("heap_used_in_bytes").asLong();
         this.esMaxHeapBytes = memObj.get("heap_max_in_bytes").asLong();
      } else {
         this.esHeapUsedBytes = 0L;
         this.esMaxHeapBytes = 0L;
      }
   }

   /**
    * Gets the stored size as MB (formatted).
    * @return The stored size as MB.
    */
   public String getStoredMB() {
      return asMB(storedSizeBytes);
   }

   /**
    * Gets the stored size as KB (formatted).
    * @return The stored size as KB.
    */
   public String getStoredKB() {
      return asKB(storedSizeBytes);
   }

   /**
    * Gets the field data size as KB (formatted).
    * @return The field data size as KB.
    */
   public String getFieldDataKB() {
      return asKB(fieldDataSizeBytes);
   }

   /**
    * Gets the segment memory use as KB (formatted).
    * @return The segment memory used.
    */
   public String getSegmentMemoryKB() {
      return asKB(segmentMemorySizeBytes);
   }

   /**
    * Gets the heap memory currently used by the ES cluster.
    * @return The memory used.
    */
   public String getClusterHeapUsedMB() {
      return asMB(esHeapUsedBytes);
   }

   /**
    * Gets the percent of heap currently used by the ES cluster.
    * @return The percent used.
    */
   public String getClusterPercentHeapUsed() {
      return asPercent(RatioGauge.Ratio.of(esHeapUsedBytes, esMaxHeapBytes).getValue());
   }

   /**
    * Gets the maximum heap available to the ES cluster.
    * @return The max memory available.
    */
   public String getClusterHeapMaxMB() {
      return asMB(esMaxHeapBytes);
   }

   public String getHeapUsedMB() {
      return asMB(mxBean.getHeapMemoryUsage().getUsed());
   }

   public String getHeapCommittedMB() {
      return asMB(mxBean.getHeapMemoryUsage().getCommitted());
   }

   public String getHeapMaxMB() {
      return asMB(mxBean.getHeapMemoryUsage().getMax());
   }

   public String getPercentHeapUsed() {
      MemoryUsage usage = mxBean.getHeapMemoryUsage();
      return asPercent(RatioGauge.Ratio.of(usage.getUsed(), usage.getMax()).getValue());
   }

   public String getNonHeapUsedMB() {
      return asMB(mxBean.getNonHeapMemoryUsage().getUsed());
   }

   public String getNonHeapCommittedMB() {
      return asMB(mxBean.getNonHeapMemoryUsage().getCommitted());
   }

   public String getNonHeapMaxMB() {
      return asMB(mxBean.getNonHeapMemoryUsage().getMax());
   }

   public String getPercentNonHeapUsed() {
      MemoryUsage usage = mxBean.getNonHeapMemoryUsage();
      return asPercent(RatioGauge.Ratio.of(usage.getUsed(), usage.getMax()).getValue());
   }

   private String asCount(final long val) {
      NumberFormat nf = NumberFormat.getNumberInstance();
      nf.setMinimumFractionDigits(0);
      nf.setMaximumFractionDigits(0);
      nf.setMinimumIntegerDigits(1);
      return nf.format(val);
   }

   private String asPercent(final double val) {
      NumberFormat nf = NumberFormat.getNumberInstance();
      nf.setMinimumFractionDigits(2);
      nf.setMaximumFractionDigits(2);
      nf.setMinimumIntegerDigits(1);
      return nf.format(val * 100.0);
   }

   /**
    * Gets a value as KB.
    * @param val The value.
    * @return The value as KB.
    */
   private String asKB(final long val) {
      double valBytes = (double)val;
      NumberFormat nf = NumberFormat.getNumberInstance();
      nf.setMinimumFractionDigits(0);
      nf.setMaximumFractionDigits(0);
      nf.setMinimumIntegerDigits(1);
      return nf.format(valBytes / 1024.0);
   }

   /**
    * Gets a value as MB.
    * @param val The value.
    * @return The value as MB.
    */
   private String asMB(final long val) {
      double valBytes = (double)val;
      NumberFormat nf = NumberFormat.getNumberInstance();
      nf.setMinimumFractionDigits(0);
      nf.setMaximumFractionDigits(0);
      nf.setMinimumIntegerDigits(1);
      return nf.format(valBytes / 1000 / 1000);
   }

   public String getDocCount() {
      return asCount(docCount);
   }

   public String getSegmentCount() {
      return asCount(segmentCount);
   }

   private final long docCount;
   public final long deletedCount;
   public final long storedSizeBytes;
   public final long fieldDataSizeBytes;
   private final long segmentCount;
   public final long segmentMemorySizeBytes;

   private final long esHeapUsedBytes;
   private final long esMaxHeapBytes;

   private final MemoryMXBean mxBean = ManagementFactory.getMemoryMXBean();

}
