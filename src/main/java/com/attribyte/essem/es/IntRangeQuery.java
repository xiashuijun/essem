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

import java.io.IOException;

public class IntRangeQuery extends Query {

   /**
    * Creates an inclusive range query.
    * @param minValue The minimum value.
    * @param maxValue The maximum value.
    */
   public IntRangeQuery(final String term, final long minValue, final long maxValue) {
      this(term, minValue, maxValue, true, true);
   }

   /**
    * Creates a range query.
    * @param minValue The minimum value.
    * @param maxValue The maximum value.
    * @param includeMin Should the minimum be included in the range?
    * @param includeMax Should the maximum be included in the range?
    */
   public IntRangeQuery(final String term, final long minValue, final long maxValue,
                        final boolean includeMin, final boolean includeMax) {
      this.term = term;
      this.minValue = minValue;
      this.maxValue = maxValue;
      this.includeMin = includeMin;
      this.includeMax = includeMax;
   }

   @Override
   public void generate(final JsonGenerator generator) throws IOException {
      generator.writeStartObject();
      {
         generator.writeObjectFieldStart("range");
         {
            generator.writeObjectFieldStart(term);
            {
               if(includeMin) {
                  generator.writeNumberField("gte", minValue);
               } else {
                  generator.writeNumberField("gt", minValue);
               }

               if(includeMax) {
                  generator.writeNumberField("lte", maxValue);
               } else {
                  generator.writeNumberField("lt", maxValue);
               }
            }
            generator.writeEndObject();
         }
         generator.writeEndObject();
      }
      generator.writeEndObject();
   }

   /**
    * The range term.
    */
   public final String term;

   /**
    * The minimum value.
    */
   public final long minValue;

   /**
    * The maximum value.
    */
   public final long maxValue;

   /**
    * Should the minimum be included in the range?
    */
   public final boolean includeMin;

   /**
    * Should the maximum be included in the range?
    */
   public final boolean includeMax;
}