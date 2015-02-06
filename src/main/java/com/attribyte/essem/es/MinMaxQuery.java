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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;

public class MinMaxQuery extends Query {

   /**
    * The min/max mode.
    */
   public static enum Mode {

      /**
       * Minimum value, inclusive.
       */
      MIN_INCLUSIVE("gte"),

      /**
       * Minimum value, exclusive.
       */
      MIN_EXCLUSIVE("gt"),

      /**
       * Maximum value, inclusive.
       */
      MAX_INCLUSIVE("lte"),

      /**
       * Mximum value, exclusive.
       */
      MAX_EXCLUSIVE("lt");

      private Mode(final String op) {
         this.op = op;
      }

      /**
       * Gets the mode from a string.
       * @param str The string value.
       * @return The mode or <code>null</code>.
       */
      public Mode fromString(final String str) {
         return modeMap.get(Strings.nullToEmpty(str).trim().toLowerCase());
      }

      public static final ImmutableMap<String, Mode> modeMap =
              ImmutableMap.of(
                      "gte", MIN_INCLUSIVE,
                      "gt", MIN_EXCLUSIVE,
                      "lte", MAX_INCLUSIVE,
                      "lt", MAX_EXCLUSIVE);

      /**
       * One of 'gte', 'gt', 'lte', 'lt'.
       */
      public final String op;
   }

   /**
    * Create a min/max value query.
    * @param term The term name.
    * @param mode The min-max mode.
    * @param endpointValue The endpoint value.
    */
   public MinMaxQuery(final String term,
                      final Mode mode,
                      final Number endpointValue) {
      this.term = term;
      this.mode = mode;
      this.endpointValue = endpointValue;
   }

   @Override
   public void generate(final JsonGenerator generator) throws IOException {
      generator.writeStartObject();
      {
         generator.writeObjectFieldStart("range");
         {
            generator.writeObjectFieldStart(term);
            {
               if(endpointValue instanceof Float || endpointValue instanceof Double) {
                  generator.writeNumberField(mode.op, endpointValue.doubleValue());
               } else {
                  generator.writeNumberField(mode.op, endpointValue.longValue());
               }
            }
            generator.writeEndObject();
         }
         generator.writeEndObject();
      }
      generator.writeEndObject();
   }

   /**
    * The term name.
    */
   public final String term;

   /**
    * The min/max mode.
    */
   public final Mode mode;

   /**
    * The endpoint value.
    */
   public final Number endpointValue;
}
