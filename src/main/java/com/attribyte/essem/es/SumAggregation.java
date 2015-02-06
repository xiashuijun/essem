/*
 * Copyright 2014 Attribyte, LLC
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

package com.attribyte.essem.es;

/**
 * Extracts the sum of values from aggregated results.
 * See <a href="http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-aggregations-metrics-sum-aggregation.html">Sum Aggregation</a>
 */
public class SumAggregation extends FieldAggregation implements MetricAggregation {

   public SumAggregation(final String name, final String field) {
      super("sum", name, field);
   }

   @Override
   public MetricAggregation newInstance(final String name, final String field) {
      return new SumAggregation(name, field);
   }
}
