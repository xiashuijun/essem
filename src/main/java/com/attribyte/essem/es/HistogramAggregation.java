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

import java.io.IOException;
import java.util.List;

public class HistogramAggregation extends QueryComponent implements BucketAggregation {

   /**
    * The 'histogram' type.
    */
   public static final String type = "histogram";

   public HistogramAggregation(final String name,
                               final String field,
                               final int interval) {
      this.name = name;
      this.field = field;
      this.interval = interval;
      this.subs = ImmutableList.of();
   }

   public HistogramAggregation(final String name,
                               final String field,
                               final int interval,
                               final List<Aggregation> subs) {
      this.name = name;
      this.field = field;
      this.interval = interval;
      this.subs = subs == null ? ImmutableList.<Aggregation>of() : ImmutableList.copyOf(subs);
   }


   @Override
   public void generate(final JsonGenerator generator) throws IOException {
      generator.writeObjectFieldStart(name);
      {
         generator.writeObjectFieldStart(type);
         {
            generator.writeStringField("field", field);
            generator.writeNumberField("interval", interval);
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
   public final long interval;
   public final ImmutableList<Aggregation> subs;


}
