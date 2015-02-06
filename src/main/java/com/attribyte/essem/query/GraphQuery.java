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

import com.attribyte.essem.Util;
import com.attribyte.essem.es.Aggregation;
import com.attribyte.essem.es.AvgAggregation;
import com.attribyte.essem.es.BooleanQuery;
import com.attribyte.essem.es.BucketAggregation;
import com.attribyte.essem.es.DateHistogramAggregation;
import com.attribyte.essem.es.ExtendedStatsAggregation;
import com.attribyte.essem.es.IntRangeQuery;
import com.attribyte.essem.es.MaxAggregation;
import com.attribyte.essem.es.MetricAggregation;
import com.attribyte.essem.es.MinAggregation;
import com.attribyte.essem.es.MinMaxQuery;
import com.attribyte.essem.es.SearchRequest;
import com.attribyte.essem.es.Sort;
import com.attribyte.essem.es.StatsAggregation;
import com.attribyte.essem.es.SumAggregation;
import com.attribyte.essem.es.TermsAggregation;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class GraphQuery extends QueryBase {

   /**
    * Creates a graph query from an HTTP request.
    * @param request The HTTP request.
    * @param defaultRange The default range expression.
    */
   public GraphQuery(final HttpServletRequest request, final String defaultRange) {

      SearchRequest.Builder requestBuilder = SearchRequest.builder();
      BooleanQuery.Builder queryBuilder = BooleanQuery.builder();
      matchAnyOf(request, "host", Fields.HOST_FIELD, queryBuilder);
      matchAnyOf(request, "app", Fields.APPLICATION_FIELD, queryBuilder);
      matchAnyOf(request, "application", Fields.APPLICATION_FIELD, queryBuilder);
      matchAnyOf(request, "instance", Fields.INSTANCE_FIELD, queryBuilder);
      matchAnyOf(request, "name", Fields.NAME_FIELD, queryBuilder);
      matchAnyOf(request, "metric", Fields.TYPE_FIELD, queryBuilder);
      parseMinMax(request, queryBuilder);

      IntRangeQuery rangeQuery = parseRange(request, defaultRange);
      queryBuilder.mustMatch(rangeQuery);
      String rangeStr = Strings.nullToEmpty(request.getParameter(RANGE_PARAMETER)).trim();
      if(rangeStr.length() == 0) {
         rangeStr = defaultRange;
      }
      this.range = new Range(rangeStr, rangeQuery.minValue, rangeQuery.maxValue);

      requestBuilder.setQuery(queryBuilder.build());

      String[] fields = request.getParameterValues("field");
      if(fields == null || fields.length == 0) {
         requestBuilder.addFields(numericFields);
         fields = numericFields.toArray(new String[numericFields.size()]);
      } else {
         for(String field : fields) {
            requestBuilder.addField(field);
         }
      }

      List<String> aggregateOn = parseAggregate(request, nonNumericFields);

      if(aggregateOn == INVALID_AGGREGATE) {
         this.error = "Only 'name', 'host', 'application', 'instance' are valid for 'aggregateOn'";
         this.isAggregation = false;
         this.searchRequest = null;
         this.downsampleFunction = null;
         this.downsampleInterval = null;
      } else if(aggregateOn.size() == 0) {
         requestBuilder.addField(Fields.NAME_FIELD);
         requestBuilder.addField(Fields.APPLICATION_FIELD);
         requestBuilder.addField(Fields.HOST_FIELD);
         requestBuilder.addField(Fields.INSTANCE_FIELD);
         requestBuilder.addField(Fields.TIMESTAMP_FIELD);
         String sortStr = getParameter(request, "sort", "asc").toLowerCase();
         Sort sort = sortStr.equals("desc") ? TS_DESC : TS_ASC;
         requestBuilder.setSort(sort);
         requestBuilder.setStart(Util.getParameter(request, START_INDEX_PARAMETER, 0));
         requestBuilder.setLimit(Util.getParameter(request, LIMIT_PARAMETER, DEFAULT_LIMIT));
         this.isAggregation = false;
         this.searchRequest = requestBuilder.build();
         this.downsampleInterval = null;
         this.downsampleFunction = null;
         this.error = null;
      } else {
         DateHistogramAggregation.Interval aggregationInterval = parseResolution(request);
         if(aggregationInterval != null) {
            this.downsampleInterval = aggregationInterval.name().toLowerCase();
            BucketAggregation.Order order = BucketAggregation.Order.fromString(request.getParameter("sort"), BucketAggregation.Order.KEY_ASC);

            List<String> numericFields = Lists.newArrayListWithExpectedSize(fields.length);
            for(String field : fields) {
               if(!nonNumericFields.containsKey(field)) {
                  numericFields.add(field);
               }
            }

            if(numericFields.size() > 0) {
               List<Aggregation> fieldAggregations = Lists.newArrayListWithExpectedSize(numericFields.size());
               this.downsampleFunction = getParameter(request, DOWNSAMPLE_FN_PARAMETER, DEFAULT_DOWNSAMPLE_FN);

               MetricAggregation protoAggregation = metricAggregationProtos.get(this.downsampleFunction.toLowerCase().trim());
               if(protoAggregation == null) {
                  protoAggregation = metricAggregationProtos.get(DEFAULT_DOWNSAMPLE_FN);
               }

               for(String field : numericFields) {
                  fieldAggregations.add(protoAggregation.newInstance(field, field));
               }

               DateHistogramAggregation histogramAggregation =
                       new DateHistogramAggregation(protoAggregation.getType(),
                               Fields.TIMESTAMP_FIELD, aggregationInterval, order, fieldAggregations);

               if(aggregateOn.size() == 1) {
                  requestBuilder.addAggregation(new TermsAggregation(aggregateOn.get(0), aggregateOn.get(0), MAX_AGGREGATION_SIZE,
                                  Collections.<Aggregation>singletonList(histogramAggregation))
                  );
               } else {
                  Collections.reverse(aggregateOn);
                  Iterator<String> aggregateFieldIter = aggregateOn.iterator();
                  String currField = aggregateFieldIter.next();
                  TermsAggregation currAggregation = new TermsAggregation(currField, currField, MAX_AGGREGATION_SIZE, Collections.<Aggregation>singletonList(histogramAggregation));
                  while(aggregateFieldIter.hasNext()) {
                     currField = aggregateFieldIter.next();
                     currAggregation = new TermsAggregation(currField, currField, MAX_AGGREGATION_SIZE, Collections.<Aggregation>singletonList(currAggregation));
                  }
                  requestBuilder.addAggregation(currAggregation);
               }

               //We never want aggregation query hits...

               requestBuilder.setStart(0);
               requestBuilder.setLimit(0);

               this.isAggregation = true;
               this.error = null;
               this.searchRequest = requestBuilder.build();

            } else {
               this.isAggregation = false;
               this.downsampleFunction = null;
               this.searchRequest = null;
               this.error = "At least one numeric field must be specified";
            }

         } else {
            this.error = "A valid '" + RESOLUTION_PARAMETER + "' must be specified";
            this.searchRequest = null;
            this.downsampleInterval = null;
            this.downsampleFunction = null;
            this.isAggregation = false;
         }
      }
   }

   /**
    * The downsample resolution parameter ('downsampleTo').
    */
   public static final String RESOLUTION_PARAMETER = "downsampleTo";

   /**
    * The downsample function parameter ('downsampleFn').
    */
   public static final String DOWNSAMPLE_FN_PARAMETER = "downsampleFn";

   /**
    * The default downsample function ('stats').
    */
   public static final String DEFAULT_DOWNSAMPLE_FN = "stats";

   /**
    * The maximum number of values returned.
    */
   public static final int DEFAULT_LIMIT = 10000;

   /**
    * Sort by timestamp ascending.
    */
   private static final Sort TS_ASC = new Sort(new Sort.SingleFieldSort("ts", Sort.Direction.ASC));

   /**
    * Sort by timestamp descending.
    */
   private static final Sort TS_DESC = new Sort(new Sort.SingleFieldSort("ts", Sort.Direction.DESC));

   /**
    * Split term:value.
    */
   private static final Splitter minMaxSplitter =
           Splitter.on(':').omitEmptyStrings().trimResults().limit(2);

   /**
    * Check for parameters of the format: gt=std:1.5, gte=count:1, lt=p95:1.0, lte=count:5
    */
   private static void parseMinMax(final HttpServletRequest request,
                                   final BooleanQuery.Builder builder) {
      for(MinMaxQuery.Mode mode : MinMaxQuery.Mode.modeMap.values()) {
         String[] termValues = request.getParameterValues(mode.op);
         if(termValues != null && termValues.length > 0) {
            for(String termValueStr : termValues) {
               Iterator<String> termValue = minMaxSplitter.split(termValueStr).iterator();
               if(termValue.hasNext()) {
                  String term = termValue.next();
                  if(termValue.hasNext()) {
                     String value = termValue.next();
                     if(Util.isInteger(value)) {
                        builder.mustMatch(new MinMaxQuery(term, mode, Integer.parseInt(value)));
                     } else {
                        try {
                           builder.mustMatch(new MinMaxQuery(term, mode, Double.parseDouble(value)));
                        } catch(NumberFormatException nfe) {
                           //Ignore...
                        }
                     }
                  }
               }
            }
         }
      }
   }

   /**
    * The search request.
    */
   public final SearchRequest searchRequest;

   /**
    * The downsample interval, if any.
    */
   public final String downsampleInterval;

   /**
    * The downsample function, if any.
    */
   public final String downsampleFunction;

   /**
    * The graph range.
    */
   public final Range range;

   /**
    * An error, or <code>null</code>.
    */
   public final String error;

   /**
    * Is the request/response expected to be an aggregation?
    */
   public final boolean isAggregation;

   /**
    * Parses the resolution parameter to the appropriate histogram interval.
    * @param request The request.
    * @return The interval or <code>null</code>.
    */
   public static DateHistogramAggregation.Interval parseResolution(final HttpServletRequest request) {
      String resolutionStr = request.getParameter(RESOLUTION_PARAMETER);
      if(resolutionStr != null) {
         return DateHistogramAggregation.Interval.intervalMap.get(resolutionStr.toLowerCase());
      } else {
         return null;
      }
   }

   /**
    * A map of prototypes used to create metric aggregations.
    */
   private static final ImmutableMap<String, MetricAggregation> metricAggregationProtos =
           ImmutableMap.<String, MetricAggregation>builder()
                   .put("avg", new AvgAggregation("", ""))
                   .put("sum", new SumAggregation("", ""))
                   .put("min", new MinAggregation("", ""))
                   .put("max", new MaxAggregation("", ""))
                   .put("stats", new StatsAggregation("", ""))
                   .put("estats", new ExtendedStatsAggregation("", ""))
                   .build();
}
