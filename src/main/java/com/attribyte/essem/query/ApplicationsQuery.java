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
import com.attribyte.essem.es.TermsAggregation;
import com.google.common.collect.ImmutableList;

/**
 * Creates a request that returns an aggregation of application, host, instance.
 */
public class ApplicationsQuery extends QueryBase {

   /**
    * The default range (one year).
    */
   public static final String DEFAULT_RANGE = "week";

   /**
    * The aggregation for application, host, instance.
    */
   private static final TermsAggregation AGGREGATION = buildAggregation();

   /**
    * The default query.
    */
   public static final ApplicationsQuery DEFAULT_QUERY = new ApplicationsQuery();

   /**
    * Creates a query that returns all applications along with their hosts and instances
    * that have reported any metrics in the past year.
    */
   public ApplicationsQuery() {
      this(DEFAULT_RANGE);
   }

   /**
    * Creates a query that returns all applications along with their hosts and instances
    * that have any reported metrics in the specified range.
    * @param range The range string ('forever', 'year', 'month', 'week', 'day', 'hour', 'minute').
    */
   public ApplicationsQuery(final String range) {
      SearchRequest.Builder requestBuilder = SearchRequest.builder();
      requestBuilder.setStart(0).setLimit(0);
      BooleanQuery.Builder queryBuilder = BooleanQuery.builder();
      queryBuilder.mustMatch(parseRange(range, DEFAULT_RANGE));
      requestBuilder.addAggregation(AGGREGATION);
      this.searchRequest = requestBuilder.build();
   }

   private static TermsAggregation buildAggregation() {
      TermsAggregation instanceAggregation = new TermsAggregation("instance", Fields.INSTANCE_FIELD, MAX_AGGREGATION_SIZE);
      TermsAggregation hostAggregation = new TermsAggregation("host", Fields.HOST_FIELD, MAX_AGGREGATION_SIZE,
              ImmutableList.<Aggregation>of(instanceAggregation));
      return new TermsAggregation("application", Fields.APPLICATION_FIELD, MAX_AGGREGATION_SIZE,
              ImmutableList.<Aggregation>of(hostAggregation));
   }

   /**
    * The search request.
    */
   public final SearchRequest searchRequest;
}
