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

package com.attribyte.essem.query;

import com.attribyte.essem.es.BooleanQuery;
import com.attribyte.essem.es.ExtendedStatsAggregation;
import com.attribyte.essem.es.IntRangeQuery;
import com.attribyte.essem.es.SearchRequest;
import com.attribyte.essem.model.graph.MetricKey;

/**
 * Creates an ES query for field stats over a range.
 */
public class StatsQuery extends QueryBase {

   /**
    * Creates a query that returns statistics for a field for an exact range.
    * @param key The metric key. The 'field' must be defined.
    * @param rangeExpression An expression that describes the range.
    * @param startTimestamp The range start.
    * @param endTimestamp The range end.
    */
   public StatsQuery(final MetricKey key,
                     final String rangeExpression,
                     final long startTimestamp, final long endTimestamp) {
      this.key = key;

      SearchRequest.Builder requestBuilder = SearchRequest.builder();
      BooleanQuery.Builder queryBuilder = BooleanQuery.builder();
      matchKey(key, queryBuilder);

      IntRangeQuery rangeQuery = (startTimestamp > 0L && endTimestamp > 0L) ?
              new IntRangeQuery(Fields.TIMESTAMP_FIELD, startTimestamp, endTimestamp) : parseRange(rangeExpression, "month");
      queryBuilder.mustMatch(rangeQuery);

      this.range = new Range(rangeExpression, rangeQuery.minValue, rangeQuery.maxValue);

      requestBuilder.setQuery(queryBuilder.build());
      requestBuilder.addAggregation(new ExtendedStatsAggregation("stats", key.field));
      requestBuilder.setStart(0).setLimit(0);

      this.searchRequest = requestBuilder.build();
   }

   /**
    * The metric key, including field.
    */
   public final MetricKey key;

   /**
    * The search request.
    */
   public final SearchRequest searchRequest;

   /**
    * The graph range.
    */
   public final Range range;
}
