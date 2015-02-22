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

import com.attribyte.essem.util.Util;
import com.attribyte.essem.es.Aggregation;
import com.attribyte.essem.es.BooleanQuery;
import com.attribyte.essem.es.BucketAggregation;
import com.attribyte.essem.es.IntRangeQuery;
import com.attribyte.essem.es.SearchRequest;
import com.attribyte.essem.es.TermsAggregation;
import com.google.common.collect.ImmutableMap;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Builds a query that returns unique metric names for key criteria.
 */
public class NameQuery extends QueryBase {

   /**
    * Cretes the query.
    * @param request The servlet request.
    * @param defaultRange The default range used if range is unspecified.
    */
   public NameQuery(final HttpServletRequest request, final String defaultRange) {

      SearchRequest.Builder requestBuilder = SearchRequest.builder();
      BooleanQuery.Builder queryBuilder = BooleanQuery.builder();
      matchAnyOf(request, "host", Fields.HOST_FIELD, queryBuilder);
      matchAnyOf(request, "app", Fields.APPLICATION_FIELD, queryBuilder);
      matchAnyOf(request, "application", Fields.APPLICATION_FIELD, queryBuilder);
      matchAnyOf(request, "instance", Fields.INSTANCE_FIELD, queryBuilder);
      matchAnyOf(request, "name", Fields.NAME_FIELD, queryBuilder);
      matchAnyOf(request, "metric", Fields.TYPE_FIELD, queryBuilder);


      IntRangeQuery rangeQuery = parseRange(request, defaultRange);
      if(rangeQuery != null) {
         queryBuilder.mustMatch(rangeQuery);
      }

      requestBuilder.setQuery(queryBuilder.build());

      List<String> aggregateOn = parseAggregate(request, nameFields);
      if(aggregateOn == INVALID_AGGREGATE) {
         this.error = "Only 'name', 'host', 'application', 'instance', 'metric' are valid for 'aggregateOn'";
         this.searchRequest = null;
      } else if(aggregateOn.size() > 0) {
         BucketAggregation.Order order = BucketAggregation.Order.fromString(request.getParameter("sort"));
         if(aggregateOn.size() == 1) {
            requestBuilder.addAggregation(new TermsAggregation(aggregateOn.get(0), aggregateOn.get(0), MAX_AGGREGATION_SIZE, order));
         } else {
            Collections.reverse(aggregateOn);
            Iterator<String> aggregateFieldIter = aggregateOn.iterator();
            String currField = aggregateFieldIter.next();
            TermsAggregation currAggregation = new TermsAggregation(currField, currField, MAX_AGGREGATION_SIZE);
            while(aggregateFieldIter.hasNext()) {
               currField = aggregateFieldIter.next();
               currAggregation = new TermsAggregation(currField, currField, MAX_AGGREGATION_SIZE, order, Collections.<Aggregation>singletonList(currAggregation));
            }
            requestBuilder.addAggregation(currAggregation);
         }

         requestBuilder.setStart(Util.getParameter(request, START_INDEX_PARAMETER, 0));
         requestBuilder.setLimit(Util.getParameter(request, LIMIT_PARAMETER, 0));

         this.error = null;
         this.searchRequest = requestBuilder.build();
      } else {
         this.error = null;
         this.searchRequest = requestBuilder.build();
      }
   }

   /**
    * The maximum number of values returned.
    */
   public static final int DEFAULT_LIMIT = 100;

   /**
    * An immutable set containing all fields that form a "name."
    */
   public static final ImmutableMap<String, String> nameFields =
           ImmutableMap.<String, String>builder()
                   .put("host", Fields.HOST_FIELD)
                   .put("application", Fields.APPLICATION_FIELD)
                   .put("instance", Fields.INSTANCE_FIELD)
                   .put("metric", Fields.TYPE_FIELD)
                   .put("type", Fields.TYPE_FIELD)
                   .put("name", Fields.NAME_FIELD).build();

   /**
    * The search request.
    */
   public final SearchRequest searchRequest;

   /**
    * An error message or <code>null</code>.
    */
   public final String error;
}