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

/**
 * A query that selects documents with a term less than (or optionally equal to) a value.
 */
public class LessThanIntQuery extends Query {

   /**
    * Creates a query that includes matches that have a value less than or equal to the maximum value.
    */
   public LessThanIntQuery(final String term, final long maxValue) {
      this(term, maxValue, true);
   }

   /**
    * Creates a query that includes values less than or optionally, less than or equal to the maximum value.
    * @param maxValue The maximum value.
    * @param includeMax Should the maximum be included?
    */
   public LessThanIntQuery(final String term, final long maxValue, final boolean includeMax) {
      this.term = term;
      this.maxValue = maxValue;
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
    * The maximum value.
    */
   public final long maxValue;

   /**
    * Is the maximum be included in the range?
    */
   public final boolean includeMax;
}