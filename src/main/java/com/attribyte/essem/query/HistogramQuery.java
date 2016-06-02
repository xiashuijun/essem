/*
 * Copyright 2016 Attribyte, LLC
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
 */

package com.attribyte.essem.query;

import com.attribyte.essem.es.BooleanQuery;
import com.attribyte.essem.es.IntRangeQuery;
import com.attribyte.essem.es.SearchRequest;
import com.attribyte.essem.es.Sort;
import com.attribyte.essem.util.Util;
import com.google.common.base.Strings;

import javax.servlet.http.HttpServletRequest;

/**
 * Creates a query for building a HDR histogram over a specified range.
 */
public class HistogramQuery extends QueryBase {

   /**
    * Creates a graph query from an HTTP request.
    * @param request The HTTP request.
    * @param defaultRange The default range expression.
    */
   public HistogramQuery(final HttpServletRequest request, final String defaultRange) {

      SearchRequest.Builder requestBuilder = SearchRequest.builder();
      BooleanQuery.Builder queryBuilder = BooleanQuery.builder();
      matchAnyOf(request, "host", Fields.HOST_FIELD, queryBuilder);
      matchAnyOf(request, "app", Fields.APPLICATION_FIELD, queryBuilder);
      matchAnyOf(request, "application", Fields.APPLICATION_FIELD, queryBuilder);
      matchAnyOf(request, "instance", Fields.INSTANCE_FIELD, queryBuilder);
      matchAnyOf(request, "name", Fields.NAME_FIELD, queryBuilder);
      matchAnyOf(request, "metric", Fields.TYPE_FIELD, queryBuilder);

      IntRangeQuery rangeQuery = parseRange(request, defaultRange);
      queryBuilder.mustMatch(rangeQuery);
      String rangeStr = Strings.nullToEmpty(request.getParameter(RANGE_PARAMETER)).trim();
      if(rangeStr.length() == 0) {
         rangeStr = defaultRange;
      }
      this.range = new Range(rangeStr, rangeQuery.minValue, rangeQuery.maxValue);

      requestBuilder.setQuery(queryBuilder.build());
      requestBuilder.addField(Fields.HDR_HISTOGRAM_FIELD);

      requestBuilder.addField(Fields.NAME_FIELD);
      requestBuilder.addField(Fields.APPLICATION_FIELD);
      requestBuilder.addField(Fields.HOST_FIELD);
      requestBuilder.addField(Fields.INSTANCE_FIELD);
      requestBuilder.addField(Fields.TIMESTAMP_FIELD);
      requestBuilder.setSort(TS_ASC);
      requestBuilder.setStart(Util.getParameter(request, START_INDEX_PARAMETER, 0));
      requestBuilder.setLimit(Util.getParameter(request, LIMIT_PARAMETER, DEFAULT_LIMIT));
      this.searchRequest = requestBuilder.build();
      this.error = null;
      this.units = request.getParameter("units");
   }

   /**
    * The maximum number of values returned.
    */
   public static final int DEFAULT_LIMIT = 10000;

   /**
    * Sort by timestamp ascending.
    */
   private static final Sort TS_ASC = new Sort(new Sort.SingleFieldSort("ts", Sort.Direction.ASC));

   /**
    * The search request.
    */
   public final SearchRequest searchRequest;

   /**
    * The graph range.
    */
   public final Range range;

   /**
    * An error, or <code>null</code>.
    */
   public final String error;

   /**
    * The requested units for values.
    */
   public final String units;
}
