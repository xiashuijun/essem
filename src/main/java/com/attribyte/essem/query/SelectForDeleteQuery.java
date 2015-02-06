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
import com.attribyte.essem.es.LessThanIntQuery;
import com.attribyte.essem.es.SearchRequest;
import com.attribyte.essem.model.graph.MetricKey;

/**
 * Creates a query used to select old records for delete.
 */
public class SelectForDeleteQuery extends QueryBase {

   /**
    * Creates a query that selects metrics for delete based on key/timestamp.
    * @param key The metric key to match.
    * @param retainAgeDays The maximum number of days matching metrics will be retained.
    */
   public SelectForDeleteQuery(final MetricKey key, final int retainAgeDays) {
      this.key = key;
      SearchRequest.Builder requestBuilder = SearchRequest.builder();
      requestBuilder.enableSourceReturn(); //Prevents invalid _source
      requestBuilder.disablePaging(); //Suppress invalid paging variables
      BooleanQuery.Builder queryBuilder = BooleanQuery.builder();
      matchKey(key, queryBuilder);

      this.maxAgeMillis = System.currentTimeMillis() - retainAgeDays * 24L * 3600L * 1000L;

      LessThanIntQuery rangeQuery = new LessThanIntQuery(Fields.TIMESTAMP_FIELD, this.maxAgeMillis, false);
      queryBuilder.mustMatch(rangeQuery);
      requestBuilder.setQuery(queryBuilder.build());
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
    * The maximum age (kept) in milliseconds.
    */
   public final long maxAgeMillis;
}
