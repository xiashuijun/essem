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

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.List;

public class DateHistogramAggregation extends QueryComponent implements BucketAggregation {

   public enum Interval {
      YEAR("year"),
      QUARTER("quarter"),
      MONTH("month"),
      WEEK("week"),
      DAY("day"),
      HOUR("hour"),
      FIVE_MINUTE("5m"),
      MINUTE("minute"),
      FIVE_SECOND("5s"),
      SECOND("second");

      Interval(final String name) {
         this.name = name;
      }

      /**
       * Maps a string to an interval.
       */
      public static final ImmutableMap<String, Interval> intervalMap =
              ImmutableMap.<String, Interval>builder()
                      .put("year", YEAR)
                      .put("quarter", QUARTER)
                      .put("month", MONTH)
                      .put("week", WEEK)
                      .put("day", DAY)
                      .put("hour", HOUR)
                      .put("minute", MINUTE)
                      .put("5m", FIVE_MINUTE)
                      .put("5_minute", FIVE_MINUTE)
                      .put("five_minute", FIVE_MINUTE)
                      .put("5s", FIVE_SECOND)
                      .put("5_second", FIVE_SECOND)
                      .put("five_second", FIVE_SECOND)
                      .put("second", SECOND).build();

      public final String name;
   }

   /**
    * The 'histogram' type.
    */
   public static final String type = "date_histogram";

   public DateHistogramAggregation(final String name,
                                   final String field,
                                   final Interval interval,
                                   final Order order) {
      this.name = name;
      this.field = field;
      this.interval = interval.name;
      this.subs = ImmutableList.of();
      this.filter = null;
      this.order = order;
   }

   public DateHistogramAggregation(final String name,
                                   final String field,
                                   final Interval interval,
                                   final Order order,
                                   final List<Aggregation> subs) {
      this.name = name;
      this.field = field;
      this.interval = interval.name;
      this.subs = subs == null ? ImmutableList.<Aggregation>of() : ImmutableList.copyOf(subs);
      this.filter = null;
      this.order = order;
   }


   public DateHistogramAggregation(final String name,
                                   final String field,
                                   final Interval interval) {
      this.name = name;
      this.field = field;
      this.interval = interval.name;
      this.subs = ImmutableList.of();
      this.filter = null;
      this.order = Order.KEY_ASC;
   }

   public DateHistogramAggregation(final String name,
                                   final String field,
                                   final Interval interval,
                                   final List<Aggregation> subs) {
      this.name = name;
      this.field = field;
      this.interval = interval.name;
      this.subs = subs == null ? ImmutableList.<Aggregation>of() : ImmutableList.copyOf(subs);
      this.filter = null;
      this.order = Order.KEY_ASC;
   }

   @Override
   public void generate(final JsonGenerator generator) throws IOException {
      generator.writeObjectFieldStart(name);
      {
         if(filter != null) {
            generator.writeFieldName("filter");
            filter.generate(generator);
         }

         generator.writeObjectFieldStart(type);
         {
            generator.writeStringField("field", field);
            generator.writeStringField("interval", interval);
            generator.writeNumberField("min_doc_count", 0);

            switch(order) { //Key ascending is default...
               case DOC_COUNT_ASC:
                  generator.writeObjectFieldStart("order");
                  generator.writeStringField("_count", "asc");
                  generator.writeEndObject();
                  break;
               case DOC_COUNT_DESC:
                  generator.writeObjectFieldStart("order");
                  generator.writeStringField("_count", "desc");
                  generator.writeEndObject();
                  break;
               case KEY_DESC:
                  generator.writeObjectFieldStart("order");
                  generator.writeStringField("_key", "desc");
                  generator.writeEndObject();
                  break;
            }
         }
         generator.writeEndObject();
         if(subs != null && subs.size() > 0) {
            generator.writeObjectFieldStart(AGGREGATION_OBJECT_NAME);
            for(Aggregation sub : subs) {
               sub.generate(generator);
            }
            generator.writeEndObject();
         }
      }
      generator.writeEndObject();
   }

   public final String name;
   public final String field;
   public final String interval;
   public final ImmutableList<Aggregation> subs;
   public final QueryComponent filter;
   public final Order order;


}
