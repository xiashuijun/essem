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

package com.attribyte.essem.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.google.common.cache.Cache;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Provide metrics for some cache stats.
 */
public class GuavaCacheMetrics implements MetricSet {

   /**
    * Create metrics for a cache.
    * @param cache The cache.
    */
   public GuavaCacheMetrics(final Cache cache) {

      final Gauge<Long> sizeGauge = new Gauge<Long>() {
         @Override
         public Long getValue() {
            return cache.size();
         }
      };

      final Gauge<Double> hitRatioGauge = new Gauge<Double>() {
         @Override
         public Double getValue() {
            return cache.stats().hitRate();
         }
      };

      final Gauge<Long> loadExceptions = new Gauge<Long>() {
         @Override
         public Long getValue() {
            return cache.stats().loadExceptionCount();
         }
      };

      final Gauge<Double> loadPenaltyGauge = new Gauge<Double>() {
         @Override
         public Double getValue() {
            return cache.stats().averageLoadPenalty();
         }
      };

      this.metrics = ImmutableMap.<String, Metric>builder()
              .put("size", sizeGauge)
              .put("hit-ratio", hitRatioGauge)
              .put("load-exceptions", loadExceptions)
              .put("load-penalty", loadPenaltyGauge)
              .build();
   }

   /**
    * The metrics.
    */
   private final ImmutableMap<String, Metric> metrics;

   @Override
   public final Map<String, Metric> getMetrics() {
      return metrics;
   }
}
