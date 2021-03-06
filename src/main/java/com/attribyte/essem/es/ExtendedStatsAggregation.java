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

public class ExtendedStatsAggregation extends FieldAggregation implements MetricAggregation {
   public ExtendedStatsAggregation(final String name, final String field) {
      super("extended_stats", name, field);
   }

   @Override
   public MetricAggregation newInstance(final String name, final String field) {
      return new ExtendedStatsAggregation(name, field);
   }
}