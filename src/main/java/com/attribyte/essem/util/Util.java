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

package com.attribyte.essem.util;

import com.attribyte.essem.es.DateHistogramAggregation;
import com.attribyte.essem.model.graph.MetricKey;
import com.attribyte.essem.query.Fields;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import javax.servlet.http.HttpServletRequest;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * Utility functions and constants.
 */
public class Util {

   /**
    * The single instance of object mapper.
    */
   public static final ObjectMapper mapper = new ObjectMapper();

   /**
    * The single instance of parser factory.
    */
   public static final JsonFactory parserFactory = new JsonFactory().enable(JsonParser.Feature.ALLOW_COMMENTS);

   /**
    * The content type sent with HTTP responses ('application/json').
    */
   public static final String JSON_CONTENT_TYPE_HEADER = "application/json";

   /**
    * The default fork-join concurrency.
    */
   static final int DEFAULT_FORK_JOIN_CONCURRENCY = 4;

   /**
    * The shared fork-join executor.
    */
   public static final Executor forkJoinPool = new ForkJoinPool(DEFAULT_FORK_JOIN_CONCURRENCY);

   /**
    * Splits the path into an iterable.
    * @param request The request.
    * @return The components, or empty iterable if none.
    */
   public static final Iterable<String> splitPath(final HttpServletRequest request) {

      String pathInfo = request.getPathInfo();
      if(pathInfo == null || pathInfo.length() == 0 || pathInfo.equals("/")) {
         return Collections.emptyList();
      } else {
         return pathSplitter.split(pathInfo);
      }
   }

   /**
    * Split with '/'.
    */
   public static final Splitter pathSplitter = Splitter.on('/').omitEmptyStrings().trimResults();

   /**
    * Split with ','.
    */
   public static final Splitter csvSplitter = Splitter.on(',').omitEmptyStrings().trimResults();

   /**
    * Gets an integer parameter from the request with a default value.
    * @param request The request.
    * @param name The parameter name.
    * @param defaultValue The default value.
    * @return The parameter or default value.
    */
   public static final int getParameter(final HttpServletRequest request, final String name, final int defaultValue) {
      String strVal = Strings.nullToEmpty(request.getParameter(name)).trim();
      if(strVal.length() == 0) return defaultValue;
      try {
         return Integer.parseInt(strVal);
      } catch(NumberFormatException nfe) {
         return defaultValue;
      }
   }

   /**
    * Gets a long parameter from the request with a default value.
    * @param request The request.
    * @param name The parameter name.
    * @param defaultValue The default value.
    * @return The parameter or default value.
    */
   public static final long getLongParameter(final HttpServletRequest request, final String name, final long defaultValue) {
      String strVal = Strings.nullToEmpty(request.getParameter(name)).trim();
      if(strVal.length() == 0) return defaultValue;
      try {
         return Long.parseLong(strVal);
      } catch(NumberFormatException nfe) {
         return defaultValue;
      }
   }

   /**
    * Gets a boolean parameter from the request with a default value.
    * @param request The request.
    * @param name The parameter name.
    * @param defaultValue The default value.
    * @return The parameter or default value.
    */
   public static final boolean getParameter(final HttpServletRequest request,
                                            final String name, final boolean defaultValue) {
      String strVal = Strings.nullToEmpty(request.getParameter(name)).trim();
      return strVal.isEmpty() ? defaultValue : strVal.equalsIgnoreCase("true");
   }

   /**
    * Gets a string parameter from the request with a default value.
    * @param request The request.
    * @param name The parameter name.
    * @param defaultValue The default value.
    * @return The parameter or default value.
    */
   public static final String getParameter(final HttpServletRequest request,
                                           final String name, final String defaultValue) {
      String strVal = Strings.nullToEmpty(request.getParameter(name)).trim();
      return strVal.isEmpty() ? defaultValue : strVal;
   }

   /**
    * Gets all parameter values.
    * @param request The request.
    * @param name The parameter name.
    * @return The values as an immutable list.
    */
   public static final ImmutableList<String> getParameterValues(final HttpServletRequest request, final String name) {
      String[] vals = request.getParameterValues(name);
      return (vals != null && vals.length > 0) ? ImmutableList.copyOf(vals) : ImmutableList.<String>of();
   }

   /**
    * Determine if a string can be parsed to an integer.
    * @param str The string.
    * @return Can the string be parsed to an integer?
    */
   public static final boolean isInteger(final String str) {
      if(str == null || str.length() == 0) {
         return false;
      }

      int start = 0;
      int len = str.length();
      char ch = str.charAt(0);
      if(ch == '-') {
         if(len == 1) {
            return false;
         } else {
            start++;
         }
      }

      for(int i = start; i < len; i++) {
         ch = str.charAt(i);
         if(ch < '0' || ch > '9') {
            return false;
         }
      }

      return true;
   }

   /**
    * Compares two byte arrays by performing the same number
    * of operations for both equality and inequality <em>when arrays are of equal length</em>.
    * @param b1 The first byte array.
    * @param b2 The second byte array.
    * @return Are the arrays equal?
    */
   public static final boolean secureEquals(final byte[] b1, final byte[] b2) {

      if(b1.length != b2.length) {
         return false;
      }

      int check = 0;
      for(int i = 0; i < b1.length; i++) {
         check |= (b1[i] ^ b2[i]);
      }

      return check == 0;
   }

   /**
    * Gets the first value of an array as a string, the value if node is not array or null.
    * @param fieldsObj The object containing fields.
    * @param key The key.
    * @return The value or <code>null</code>.
    */
   public static String getStringField(final JsonNode fieldsObj, final String key) {
      JsonNode fieldNode = getFieldNode(fieldsObj, key);
      return fieldNode != null ? fieldNode.textValue() : null;
   }

   /**
    * Gets the first node of an array or the node itself if it is not an array or null.
    * @param fieldsObj The object containing fields.
    * @param key The key.
    * @return The node or <code>null</code>.
    */
   public static JsonNode getFieldNode(final JsonNode fieldsObj, final String key) {
      JsonNode fieldNode = fieldsObj.get(key);
      if(fieldNode != null) {
         if(fieldNode.isArray() && fieldNode.size() > 0) {
            return fieldNode.get(0);
         } else if(!fieldNode.isArray()) {
            return fieldNode;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   /**
    * Gets an int value from a node.
    * @param node The parent node.
    * @param key The key.
    * @param defaultValue The default value.
    * @return The value or default value.
    */
   public static int getIntField(final JsonNode node, final String key, final int defaultValue) {
      JsonNode field = node.get(key);
      if(field != null && field.canConvertToInt()) {
         return field.intValue();
      } else {
         return defaultValue;
      }
   }

   /**
    * Gets an long value from a node.
    * @param node The parent node.
    * @param key The key.
    * @param defaultValue The default value.
    * @return The value or default value.
    */
   public static long getLongField(final JsonNode node, final String key, final long defaultValue) {
      JsonNode field = node.get(key);
      if(field != null && field.canConvertToInt()) {
         return field.longValue();
      } else {
         return defaultValue;
      }
   }

   /**
    * Gets a double value from a node.
    * @param node The parent node.
    * @param key The key.
    * @param defaultValue The default value.
    * @return The value or default value.
    */
   public static double getDoubleField(final JsonNode node, final String key, final double defaultValue) {
      JsonNode field = node.get(key);
      if(field != null && field.isNumber()) {
         return field.doubleValue();
      } else {
         return defaultValue;
      }
   }

   /**
    * Creates a metric key from request parameters.
    * @param request The request.
    * @return The created key.
    */
   public static MetricKey createKey(final HttpServletRequest request) {

      String app = request.getParameter(Fields.APPLICATION_FIELD);
      if(app == null) {
         app = request.getParameter("app");
      }

      return new MetricKey(
              request.getParameter(Fields.NAME_FIELD),
              app,
              request.getParameter(Fields.HOST_FIELD),
              request.getParameter(Fields.INSTANCE_FIELD),
              request.getParameter("field")
      );
   }

   /**
    * A set of property names to be ignored when copying nodes
    * to graph output.
    */
   public static final ImmutableSet<String> graphIgnoreProperties =
           ImmutableSet.of("ts", "name", "application", "host", "instance");

   /**
    * A set of fields that must be integers.
    */
   public static final ImmutableSet<String> integralNumberFields =
           ImmutableSet.of("count", "samples");

   /**
    * Milliseconds for one second.
    */
   public static long SECOND_MILLIS = 1000L;

   /**
    * Milliseconds for five seconds.
    */
   public static long FIVE_SECOND_MILLIS = 5000L;

   /**
    * Milliseconds for one minute.
    */
   public static long MINUTE_MILLIS = 1000L * 60L;

   /**
    * Milliseconds for five minutes.
    */
   public static long FIVE_MINUTE_MILLIS = MINUTE_MILLIS * 5L;

   /**
    * Milliseconds for one hour.
    */
   public static long HOUR_MILLIS = MINUTE_MILLIS * 60L;

   /**
    * Milliseconds for one day.
    */
   public static long DAY_MILLIS = HOUR_MILLIS * 24L;

   /**
    * Milliseconds for one week.
    */
   public static long WEEK_MILLIS = 7L * DAY_MILLIS;

   /**
    * Milliseconds for one month.
    */
   public static long MONTH_MILLIS = DAY_MILLIS * 30L;

   /**
    * Gets a string (hour, day) closest to an interval.
    * @param intervalMillis The interval in milliseconds.
    * @return The interval name.
    */
   public static String nearestInterval(long intervalMillis) {

      intervalMillis = Math.abs(intervalMillis);

      if(intervalMillis == 0L) {
         return null;
      } else if(intervalMillis <= SECOND_MILLIS) {
         return "second";
      } else if(intervalMillis > SECOND_MILLIS && intervalMillis <= FIVE_SECOND_MILLIS) {
         long i0 = Math.abs(intervalMillis - SECOND_MILLIS);
         long i1 = Math.abs(intervalMillis - FIVE_SECOND_MILLIS);
         return i0 < i1 ? "second" : "5s";
      } else if(intervalMillis > FIVE_SECOND_MILLIS && intervalMillis <= MINUTE_MILLIS) {
         long i0 = Math.abs(intervalMillis - FIVE_SECOND_MILLIS);
         long i1 = Math.abs(intervalMillis - MINUTE_MILLIS);
         return i0 < i1 ? "5s" : "minute";
      } else if(intervalMillis > MINUTE_MILLIS && intervalMillis <= FIVE_MINUTE_MILLIS) {
         long i0 = Math.abs(intervalMillis - MINUTE_MILLIS);
         long i1 = Math.abs(intervalMillis - FIVE_MINUTE_MILLIS);
         return i0 < i1 ? "minute" : "5m";
      } else if(intervalMillis > FIVE_MINUTE_MILLIS && intervalMillis <= HOUR_MILLIS) {
         long i0 = Math.abs(intervalMillis - FIVE_MINUTE_MILLIS);
         long i1 = Math.abs(intervalMillis - HOUR_MILLIS);
         return i0 < i1 ? "5m" : "hour";
      } else if(intervalMillis > HOUR_MILLIS && intervalMillis <= DAY_MILLIS) {
         long i0 = Math.abs(intervalMillis - HOUR_MILLIS);
         long i1 = Math.abs(intervalMillis - DAY_MILLIS);
         return i0 < i1 ? "hour" : "day";
      } else if(intervalMillis > DAY_MILLIS && intervalMillis <= WEEK_MILLIS) {
         long i0 = Math.abs(intervalMillis - DAY_MILLIS);
         long i1 = Math.abs(intervalMillis - WEEK_MILLIS);
         return i0 < i1 ? "day" : "week";
      } else if(intervalMillis > WEEK_MILLIS && intervalMillis <= MONTH_MILLIS) {
         long i0 = Math.abs(intervalMillis - WEEK_MILLIS);
         long i1 = Math.abs(intervalMillis - MONTH_MILLIS);
         return i0 < i1 ? "week" : "month";
      } else {
         return "year";
      }
   }

   /**
    * Gets a default downsample interval for a range.
    * @param range The range.
    * @return The downsample interval.
    */
   public static DateHistogramAggregation.Interval defaultDownsampleInterval(String range) {
      switch(range.toLowerCase()) {
         case "minute": return DateHistogramAggregation.Interval.SECOND;
         case "hour": return DateHistogramAggregation.Interval.FIVE_SECOND;
         case "day": return DateHistogramAggregation.Interval.FIVE_MINUTE;
         case "week": return DateHistogramAggregation.Interval.HOUR;
         case "month": return DateHistogramAggregation.Interval.HOUR;
         case "year": return DateHistogramAggregation.Interval.DAY;
         default: return DateHistogramAggregation.Interval.HOUR;
      }
   }

   /**
    * Close without throwing an exception.
    * @param c The closable.
    */
   public static void closeQuietly(final Closeable c) {
      if(c != null) {
         try {
            c.close();
         } catch(IOException ioe) {
            //Ignore...
         }
      }
   }

   /**
    * Creates a string that is a valid "Java identifier" by
    * replacing invalid characters with underscore ('_').
    * @param str The input string.
    * @return The valid java identifier.
    */
   public static String toJavaIdentifier(final String str) {

      StringBuilder buf = new StringBuilder();
      char[] chars = str.toCharArray();
      if(Character.isJavaIdentifierStart(chars[0])) {
         buf.append(chars[0]);
      } else if(Character.isJavaIdentifierPart(chars[0])) {
         buf.append("_").append(chars[0]);
      } else {
         buf.append("_");
      }

      for(int pos = 1; pos < chars.length; pos++) {
         if(Character.isJavaIdentifierPart(chars[pos])) {
            buf.append(chars[pos]);
         } else {
            buf.append("_");
         }
      }

      return buf.toString();
   }

   /**
    * Creates a string that is a valid (safe) "identifier" by
    * replacing invalid characters with underscore ('_').
    * @param str The input string.
    * @return The valid identifier.
    */
   public static String toValidIdentifier(final String str) {  //TODO
      if(Strings.isNullOrEmpty(str)) {
         return str;
      }

      StringBuilder buf = new StringBuilder();
      for(char ch : str.toCharArray()) {
         if(Character.isSpaceChar(ch)) ch = ' ';
         if(isValidIdentifier(ch)) {
            buf.append(ch);
         } else {
            buf.append("_");
         }
      }
      return buf.toString();
   }

   /**
    * Determine if the string is a valid identifier.
    * @param str The string.
    * @return Is the string a valid identifier?
    */
   public static boolean isValidIdentifier(final String str) {
      if(str == null || str.isEmpty()) return true;
      for(char ch : str.toCharArray()) if(!isValidIdentifier(ch)) return false;
      return true;
   }

   /**
    * Determine if the character is a valid identifier.
    * @param ch The character.
    * @return Is the character a valid identifier?
    */
   public static boolean isValidIdentifier(char ch) { //TODO
         switch(ch) {
            case '-':
            case '+':
            case '!':
            case '.':
            case ',':
            case '%':
            case ':':
            case '#':
            case '^':
            case '{':
            case '}':
            case '[':
            case ']':
            case '(':
            case ')':
            case '@':
            case '/':
            case '\\':
            case '~':
            case '|':
            case ' ':
               break;
            default:
               if(!Character.isJavaIdentifierPart(ch)) return false;
         }
      return true;
   }

   /**
    * Converts a node to a "pretty" JSON string.
    * @param node The node.
    * @return The node as a string.
    */
   public static final String toPrettyJSON(final JsonNode node) {
      try {
         return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
      } catch(JsonProcessingException pe) {
         return "[invalid json]";
      }
   }

   /**
    * Creates a time unit from a string.
    * @param str The string.
    * @param defaultUnit The default unit.
    * @return The time unit or the default value.
    */
   public static final TimeUnit timeUnitFromString(final String str, final TimeUnit defaultUnit) {
      TimeUnit unit = str != null ? timeUnitMap.get(str.toLowerCase().trim()) : defaultUnit;
      return unit != null ? unit : defaultUnit;
   }

   /**
    * Maps a string name to a time unit.
    */
   private static final ImmutableMap<String, TimeUnit> timeUnitMap =
           ImmutableMap.<String, TimeUnit>builder()
                   .put("nano", TimeUnit.NANOSECONDS)
                   .put("nanoseconds", TimeUnit.NANOSECONDS)
                   .put("nanos", TimeUnit.NANOSECONDS)

                   .put("us", TimeUnit.MICROSECONDS)
                   .put("microseconds", TimeUnit.MICROSECONDS)
                   .put("micros", TimeUnit.MICROSECONDS)

                   .put("ms", TimeUnit.MILLISECONDS)
                   .put("milliseconds", TimeUnit.MILLISECONDS)
                   .put("millis", TimeUnit.MILLISECONDS)

                   .put("s", TimeUnit.SECONDS)
                   .put("second", TimeUnit.SECONDS)
                   .put("seconds", TimeUnit.SECONDS)

                   .put("min", TimeUnit.MINUTES)
                   .put("minute", TimeUnit.MINUTES)
                   .put("minutes", TimeUnit.MINUTES)

                   .put("h", TimeUnit.HOURS)
                   .put("hour", TimeUnit.HOURS)
                   .put("hours", TimeUnit.HOURS)

                   .put("d", TimeUnit.DAYS)
                   .put("day", TimeUnit.DAYS)
                   .put("days", TimeUnit.DAYS)
                   .build();

   /**
    * Converts nanos to the specified units.
    * @param value The value to convert.
    * @param units The units (may be {@code null}).
    * @return The converted value.
    */
   public static long nanosToUnits(final long value, final TimeUnit units) {
      return units != null ? value / TimeUnit.NANOSECONDS.convert(1, units) : value;
   }

   /**
    * Converts nanos (as a double) to the specified units.
    * @param value The value to convert.
    * @param units The units (may be {@code null}).
    * @return The converted value.
    */
   public static double nanosToUnits(final double value, final TimeUnit units) {
      return units != null ? value / (double)TimeUnit.NANOSECONDS.convert(1, units) : value;
   }
}