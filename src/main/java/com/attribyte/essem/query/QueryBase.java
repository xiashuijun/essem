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

import com.attribyte.essem.es.BooleanQuery;
import com.attribyte.essem.es.IntRangeQuery;
import com.attribyte.essem.es.Query;
import com.attribyte.essem.es.StringPrefixQuery;
import com.attribyte.essem.es.StringTermQuery;
import com.attribyte.essem.model.graph.MetricKey;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class QueryBase {

   /**
    * The range parameter ('range').
    */
   public static final String RANGE_PARAMETER = "range";

   /**
    * The parameter that specifies fields for aggregation ('aggregateOn').
    */
   public static final String AGGREGATE_ON_PARAMETER = "aggregateOn";

   /**
    * The starting index parameter ('start').
    */
   public static final String START_INDEX_PARAMETER = "start";

   /**
    * The parameter for the range start in milliseconds ('startTimestamp').
    */
   public static final String START_TIMESTAMP_PARAMETER = "rangeStart";

   /**
    * The parameter for the range end in milliseconds ('endTimestamp').
    */
   public static final String END_TIMESTAMP_PARAMETER = "rangeEnd";

   /**
    * The limit parameter ('limit').
    */
   public static final String LIMIT_PARAMETER = "limit";

   /**
    * An immutable map containing expected non-numeric field keys and their
    * associated name in the index.
    */
   public static final ImmutableMap<String, String> nonNumericFields =
           ImmutableMap.<String, String>builder()
                   .put("host", Fields.HOST_FIELD)
                   .put("application", Fields.APPLICATION_FIELD)
                   .put("instance", Fields.INSTANCE_FIELD)
                   .put("name", Fields.NAME_FIELD)
                   .put("comment", Fields.COMMENT_FIELD)
                   .put("type", Fields.TYPE_FIELD)
                   .put("metric", Fields.TYPE_FIELD)
                   .put("unit", "unit").build();

   /**
    * An immutable set containing all numeric fields.
    */
   public static final ImmutableSet<String> numericFields =
           ImmutableSet.<String>builder()
                   .add(Fields.COUNT_FIELD)
                   .add(Fields.VALUE_FIELD)
                   .add(Fields.MAX_FIELD).add(Fields.MEAN_FIELD).add(Fields.MIN_FIELD).add(Fields.STD_FIELD)
                   .add("median").add(Fields.P50_FIELD).add(Fields.P75_FIELD)
                   .add(Fields.P95_FIELD).add(Fields.P98_FIELD).add(Fields.P99_FIELD).add(Fields.P999_FIELD)
                   .add(Fields.ONE_MINUTE_RATE_FIELD).add(Fields.FIVE_MINUTE_RATE_FIELD)
                   .add(Fields.FIFTEEN_MINUTE_RATE_FIELD).add(Fields.MEAN_RATE_FIELD)
                   .add("name").build(); //?

   /**
    * Indicates an invalid 'aggregateOn' was supplied.
    */
   protected static final ImmutableList<String> INVALID_AGGREGATE = ImmutableList.of("[invalid]");

   /**
    * The graph time range.
    */
   public static class Range {

      /**
       * Indicates no specified range.
       */
      public static Range NONE = new Range(null, 0L, 0L);

      Range(final String expression, final long startTimestamp, final long endTimestamp) {
         this.expression = expression;
         this.startTimestamp = startTimestamp;
         this.endTimestamp = endTimestamp;
      }

      /**
       * The expression like 'day' or 'month'.
       */
      public final String expression;

      /**
       * The start timestamp.
       */
      public final long startTimestamp;

      /**
       * The end timestamp.
       */
      public final long endTimestamp;
   }


   /**
    * Match one or more specified parameters.
    * @param request The request.
    * @param parameterName The parameter name.
    * @param termName The term associated with the parameter.
    * @param builder The query builder.
    */
   protected static void matchAnyOf(final HttpServletRequest request,
                                    final String parameterName, final String termName,
                                    final BooleanQuery.Builder builder) {
      matchAnyOf(request, parameterName, termName, (String)null, builder);
   }

   /**
    * Match one or more specified parameters.
    * @param request The request.
    * @param parameterName The parameter name.
    * @param termName The term associated with the parameter.
    * @param defaultValue The default value if parameter is not specified.
    * @param builder The query builder.
    */
   protected static void matchAnyOf(final HttpServletRequest request,
                                    final String parameterName, final String termName,
                                    final String defaultValue,
                                    final BooleanQuery.Builder builder) {
      String[] parameters = request.getParameterValues(parameterName);
      if(parameters == null || parameters.length == 0) {
         if(defaultValue != null) {
            builder.mustMatch(new StringTermQuery(termName, defaultValue));
         }
      } else if(parameters.length == 1) {
         Query termOrPrefixQuery = buildTermOrPrefixQuery(termName, parameters[0]);
         if(termOrPrefixQuery != null) {
            builder.mustMatch(termOrPrefixQuery);
         }
      } else {
         BooleanQuery.Builder parameterBuilder = BooleanQuery.builder();
         int expressionCount = 0;
         for(String parameter : parameters) {
            Query termOrPrefixQuery = buildTermOrPrefixQuery(termName, parameter);
            if(termOrPrefixQuery != null) {
               parameterBuilder.shouldMatch(termOrPrefixQuery);
               expressionCount++;
            }
         }
         if(expressionCount > 0) {
            builder.mustMatch(parameterBuilder.build());
         }
      }
   }

   /**
    * Match one or more specified parameters.
    * @param request The request.
    * @param parameterName The parameter name.
    * @param termName The term associated with the parameter.
    * @param additionalValues Additional values to be added in addition to any found in parameters.
    * @param builder The query builder.
    */
   protected static void matchAnyOf(final HttpServletRequest request,
                                    final String parameterName, final String termName,
                                    final Collection<String> additionalValues,
                                    final BooleanQuery.Builder builder) {

      if(additionalValues == null || additionalValues.size() == 0) {
         matchAnyOf(request, parameterName, termName, (String)null, builder);
         return;
      }

      final Collection<String> values;

      String[] parameters = request.getParameterValues(parameterName);
      if(parameters != null && parameters.length > 0) {
         values = Lists.newArrayListWithCapacity(parameters.length + additionalValues.size());
         values.addAll(additionalValues);
         Collections.addAll(values, parameters);
      } else {
         values = additionalValues;
      }

      if(values.size() == 1) {
         Query termOrPrefixQuery = buildTermOrPrefixQuery(termName, values.iterator().next());
         if(termOrPrefixQuery != null) {
            builder.mustMatch(termOrPrefixQuery);
         }
      } else {
         BooleanQuery.Builder parameterBuilder = BooleanQuery.builder();
         int expressionCount = 0;
         for(String value : values) {
            Query termOrPrefixQuery = buildTermOrPrefixQuery(termName, value);
            if(termOrPrefixQuery != null) {
               parameterBuilder.shouldMatch(termOrPrefixQuery);
               expressionCount++;
            }
         }
         if(expressionCount > 0) {
            builder.mustMatch(parameterBuilder.build());
         }
      }
   }

   /**
    * Builds either a term or prefix query.
    * @param termName The term name.
    * @param value The value.
    * @return The query or <code>null</code> if no query should be added.
    */
   private static Query buildTermOrPrefixQuery(final String termName, String value) {
      value = Strings.nullToEmpty(value).trim();
      if(value.length() == 0) {
         return null;
      } else if(value.endsWith("*")) {
         if(value.length() > 1) {
            return new StringPrefixQuery(termName, value.substring(0, value.length() - 1));
         } else {
            return null;
         }
      } else {
         return new StringTermQuery(termName, value);
      }
   }

   /**
    * Parses the range query from request parameters.
    * @param request The request.
    * @param defaultRange The default range used if 'range' is unspecified.
    * @return The range query.
    */
   public static IntRangeQuery parseRange(final HttpServletRequest request, final String defaultRange) {
      String rangeStartTimestamp = Strings.nullToEmpty(request.getParameter(START_TIMESTAMP_PARAMETER)).trim();
      String rangeEndTimestamp = Strings.nullToEmpty(request.getParameter(END_TIMESTAMP_PARAMETER)).trim();
      if(rangeStartTimestamp.length() > 0 || rangeEndTimestamp.length() > 0) {
         long startTimestamp = rangeStartTimestamp.length() > 0 ? Long.parseLong(rangeStartTimestamp) : 0L;
         long endTimestamp = rangeEndTimestamp.length() > 0 ? Long.parseLong(rangeEndTimestamp) : System.currentTimeMillis();
         return new IntRangeQuery(Fields.TIMESTAMP_FIELD, startTimestamp, endTimestamp);
      } else {
         return parseRange(request.getParameter(RANGE_PARAMETER), defaultRange);
      }
   }

   /**
    * Parses the range query from request parameters.
    * @param rangeStr The range.
    * @param defaultRange The default range used if 'range' is unspecified.
    * @return The range query.
    */
   public static IntRangeQuery parseRange(String rangeStr, final String defaultRange) {

      rangeStr = Strings.nullToEmpty(rangeStr).trim();
      if(rangeStr.length() == 0) {
         rangeStr = defaultRange;
      }

      Long rangeMillis = rangeMillisMap.get(rangeStr);
      if(rangeMillis != null) {
         long currTimeMillis = System.currentTimeMillis();
         long min = currTimeMillis - rangeMillis;
         return new IntRangeQuery(Fields.TIMESTAMP_FIELD, min, currTimeMillis);
      } else {
         Parser nattyParser = new Parser();
         List<DateGroup> dateGroups = nattyParser.parse(rangeStr);
         if(dateGroups.size() == 0) {
            return getDefaultRangeQuery(defaultRange);
         } else {
            List<Date> dates = dateGroups.get(0).getDates();
            if(dates.size() == 0) {
               return getDefaultRangeQuery(defaultRange);
            } else if(dates.size() == 1) {
               return new IntRangeQuery(Fields.TIMESTAMP_FIELD, dates.get(0).getTime(), System.currentTimeMillis());
            } else {
               Collections.sort(dates);
               return new IntRangeQuery(Fields.TIMESTAMP_FIELD, dates.get(0).getTime(), dates.get(1).getTime());
            }
         }
      }
   }

   /**
    * Matches a key ('application', 'name', 'host', 'instance', but excluding field).
    * @param key The key.
    * @param builder The query builder.
    */
   protected static void matchKey(final MetricKey key, final BooleanQuery.Builder builder) {
      Query termOrPrefixQuery = buildTermOrPrefixQuery(Fields.APPLICATION_FIELD, key.application);
      if(termOrPrefixQuery != null) {
         builder.mustMatch(termOrPrefixQuery);
      }

      termOrPrefixQuery = buildTermOrPrefixQuery(Fields.NAME_FIELD, key.name);
      if(termOrPrefixQuery != null) {
         builder.mustMatch(termOrPrefixQuery);
      }

      termOrPrefixQuery = buildTermOrPrefixQuery(Fields.HOST_FIELD, key.host);
      if(termOrPrefixQuery != null) {
         builder.mustMatch(termOrPrefixQuery);
      }

      termOrPrefixQuery = buildTermOrPrefixQuery(Fields.INSTANCE_FIELD, key.instance);
      if(termOrPrefixQuery != null) {
         builder.mustMatch(termOrPrefixQuery);
      }
   }

   /**
    * Parses the components of a key from request parameters.
    * @param request The request.
    * @return The parsed key.
    */
   protected static MetricKey parseKey(final HttpServletRequest request) {
      return parseKey(request, null);
   }

   /**
    * Parses the components of a key from request parameters.
    * @param request The request.
    * @param app The application name to use (overrides any parameter).
    * @return The parsed key.
    */
   protected static MetricKey parseKey(final HttpServletRequest request, String app) {

      if(app == null) {
         app = request.getParameter("app");
         if(app == null) app = request.getParameter("application");
      }

      return new MetricKey(request.getParameter("name"), app,
              request.getParameter("host"), request.getParameter("instance"),
              request.getParameter("field"));
   }

   /**
    * Builds a range query for the default value.
    * @param defaultRange The default range.
    * @return The range query.
    */
   protected static IntRangeQuery getDefaultRangeQuery(final String defaultRange) {
      Long rangeMillis = rangeMillisMap.get(defaultRange);
      if(rangeMillis == null) rangeMillis = rangeMillisMap.get(FAILSAFE_RANGE);
      long currTimeMillis = System.currentTimeMillis();
      long min = currTimeMillis - rangeMillis;
      return new IntRangeQuery(Fields.TIMESTAMP_FIELD, min, currTimeMillis);
   }

   /**
    * Maps the number of milliseconds for a range.
    */
   protected static ImmutableMap<String, Long> rangeMillisMap =
           ImmutableMap.<String, Long>builder()
                   .put("minute", 60L * 1000L)
                   .put("1m", 60L * 1000L)
                   .put("2m", 60L * 1000L * 2L)
                   .put("3m", 60L * 1000L * 3L)
                   .put("4m", 60L * 1000L * 4L)
                   .put("5m", 60L * 1000L * 5L)
                   .put("10m", 60L * 1000L * 10L)
                   .put("15m", 60L * 1000L * 15L)
                   .put("20m", 60L * 1000L * 20L)
                   .put("30m", 60L * 1000L * 30L)
                   .put("45m", 60L * 1000L * 45L)
                   .put("hour", 3600L * 1000L)
                   .put("1h", 3600L * 1000L)
                   .put("2h", 3600L * 1000L * 2L)
                   .put("3h", 3600L * 1000L * 3L)
                   .put("4h", 3600L * 1000L * 4L)
                   .put("5h", 3600L * 1000L * 5L)
                   .put("6h", 3600L * 1000L * 6L)
                   .put("7h", 3600L * 1000L * 7L)
                   .put("8h", 3600L * 1000L * 8L)
                   .put("9h", 3600L * 1000L * 9L)
                   .put("10h", 3600L * 1000L * 10L)
                   .put("11h", 3600L * 1000L * 11L)
                   .put("12h", 3600L * 1000L * 12L)
                   .put("13h", 3600L * 1000L * 13L)
                   .put("14h", 3600L * 1000L * 14L)
                   .put("15h", 3600L * 1000L * 15L)
                   .put("16h", 3600L * 1000L * 16L)
                   .put("17h", 3600L * 1000L * 17L)
                   .put("18h", 3600L * 1000L * 18L)
                   .put("19h", 3600L * 1000L * 19L)
                   .put("20h", 3600L * 1000L * 20L)
                   .put("21h", 3600L * 1000L * 21L)
                   .put("22h", 3600L * 1000L * 22L)
                   .put("23h", 3600L * 1000L * 23L)
                   .put("24h", 3600L * 1000L * 24L)
                   .put("day", 3600L * 1000L * 24L)
                   .put("1d", 3600L * 1000L * 24L)
                   .put("2d", 3600L * 1000L * 24L * 2L)
                   .put("3d", 3600L * 1000L * 24L * 3L)
                   .put("4d", 3600L * 1000L * 24L * 4L)
                   .put("5d", 3600L * 1000L * 24L * 5L)
                   .put("6d", 3600L * 1000L * 24L * 6L)
                   .put("7d", 3600L * 1000L * 24L * 7L)
                   .put("14d", 3600L * 1000L * 24L * 14L)
                   .put("30d", 3600L * 1000L * 24L * 30L)
                   .put("week", 3600L * 1000L * 24L * 7L)
                   .put("month", 3600L * 1000L * 24L * 30L)
                   .put("year", 3600L * 1000L * 24L * 365L)
                   .put("forever", 3600L * 1000L * 24L * 365L * 20L)
                   .build();

   /**
    * The range used if none is specified.
    */
   private static final String FAILSAFE_RANGE = "hour";


   /**
    * The maximum aggregation size.
    */
   protected static final int MAX_AGGREGATION_SIZE = 16384;

   /**
    * Splits on ',' omitting empty strings and trimming results.
    */
   private static final Splitter commaSplitter =
           Splitter.on(',').omitEmptyStrings().trimResults().limit(8);

   /**
    * Parses the 'aggregateOn' field.
    * @param request The request.
    * @return The list of aggregate field names, or <code>INVALID_AGGREGATE</code> if an invalid field was supplied.
    */
   protected static List<String> parseAggregate(final HttpServletRequest request, final Map<String, String> nameFields) {
      String aggregateOnStr = request.getParameter(AGGREGATE_ON_PARAMETER);
      if(aggregateOnStr == null) aggregateOnStr = request.getParameter("aggOn");

      if(Strings.nullToEmpty(aggregateOnStr).trim().length() > 0) {
         List<String> aggregates = Lists.newArrayListWithCapacity(8);
         for(String field : commaSplitter.split(aggregateOnStr)) {
            field = field.toLowerCase();
            if(nameFields.containsKey(field)) {
               String translateField = nameFields.get(field);
               if(!aggregates.contains(translateField)) {
                  aggregates.add(translateField);
               }
            } else {
               return INVALID_AGGREGATE;
            }
         }
         return aggregates.size() > 0 ? aggregates : INVALID_AGGREGATE;
      } else {
         return ImmutableList.of();
      }
   }

   /**
    * Gets a parameter with a default value.
    * @param request The request.
    * @param name The parameter name.
    * @param defaultValue The default value.
    * @return The request or default value.
    */
   protected static String getParameter(final HttpServletRequest request,
                                        final String name, final String defaultValue) {
      String val = Strings.nullToEmpty(request.getParameter(name)).trim();
      return val.length() > 0 ? val : defaultValue;
   }
}
