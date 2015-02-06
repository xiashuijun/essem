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

package com.attribyte.essem.query;

import com.attribyte.essem.es.Aggregation;
import com.attribyte.essem.es.BooleanQuery;
import com.attribyte.essem.es.SearchRequest;
import com.attribyte.essem.es.StringTermQuery;
import com.attribyte.essem.es.TermsAggregation;
import com.google.common.collect.ImmutableList;

/**
 * Creates a request that returns an aggregation of _type,name for an application.
 */
public class ApplicationMetricsQuery extends QueryBase {

   /**
    * The default range (one year).
    */
   private static final String DEFAULT_RANGE = "year";

   /**
    * The aggregation for _type, name.
    */
   private static final TermsAggregation AGGREGATION = buildAggregation();

   /**
    * Creates a query that returns all metrics reported in the past year for an application.
    * @param appName The application name.
    */
   public ApplicationMetricsQuery(final String appName) {
      this(appName, DEFAULT_RANGE);
   }

   /**
    * Creates a query that returns all metrics for an application
    * that have reported values in the specified range.
    * @param appName The application name.
    * @param range The range string ('forever', 'year', 'month', 'week', 'day', 'hour', 'minute').
    */
   public ApplicationMetricsQuery(final String appName, final String range) {
      SearchRequest.Builder requestBuilder = SearchRequest.builder();
      requestBuilder.setStart(0).setLimit(0);
      BooleanQuery.Builder queryBuilder = BooleanQuery.builder();
      queryBuilder.mustMatch(new StringTermQuery(Fields.APPLICATION_FIELD, appName));
      queryBuilder.mustMatch(parseRange(range, DEFAULT_RANGE));
      requestBuilder.setQuery(queryBuilder.build());
      requestBuilder.addAggregation(AGGREGATION);
      this.searchRequest = requestBuilder.build();
   }

   private static TermsAggregation buildAggregation() {
      TermsAggregation nameAggregation = new TermsAggregation("name", Fields.NAME_FIELD, MAX_AGGREGATION_SIZE);
      return new TermsAggregation("_type", "_type", MAX_AGGREGATION_SIZE, ImmutableList.<Aggregation>of(nameAggregation));
   }

   /**
    * The search request.
    */
   public final SearchRequest searchRequest;
}