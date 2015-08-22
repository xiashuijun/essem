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

package com.attribyte.essem;

import com.attribyte.essem.query.GraphQuery;
import com.attribyte.essem.query.NameQuery;
import com.attribyte.essem.query.StatsQuery;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.attribyte.api.http.Response;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumSet;

public interface ResponseGenerator {

   /**
    * Response options.
    */
   public static enum Option {

      /**
       * Generate a "pretty" response, if possible.
       */
      PRETTY,

      /**
       * Add a query "explanation", if possible.
       */
      EXPLAIN,

      /**
       * Include empty bins.
       */
      EMPTY_BINS
   }

   /**
    * Rate unit for conversion.
    */
   public static enum RateUnit {

      /**
       * Per second (default).
       */
      PER_SECOND(1.0),

      /**
       * Per minute.
       */
      PER_MINUTE(60.0),

      /**
       * Per hour.
       */
      PER_HOUR(3600.0);

      RateUnit(final double mult) {
         this.mult = mult;
      }

      /**
       * The multiplier for conversion from the default unit.
       */
      public final double mult;

      /**
       * Map strings to rate units.
       */
      private static ImmutableMap<String, RateUnit> unitMap = ImmutableMap.of(
              "", PER_SECOND,
              "persecond", PER_SECOND,
              "perminute", PER_MINUTE,
              "perhour", PER_HOUR
      );

      /**
       * Gets a rate unit from a string.
       * @param str The string.
       * @return The rate unit or the default if string is not recognized.
       */
      public static RateUnit fromString(final String str) {
         RateUnit unit = unitMap.get(Strings.nullToEmpty(str).trim().toLowerCase());
         return unit != null ? unit : RAW_RATE_UNIT;
      }
   }

   /**
    * The rate unit in which raw data is expressed (per second).
    */
   public static final RateUnit RAW_RATE_UNIT = RateUnit.PER_SECOND;

   /**
    * Generates a graph response.
    * @param graphQuery The query.
    * @param esResponse The response from ES.
    * @param options Response options.
    * @param rateUnit The rate unit.
    * @param response The target HTTP response.
    * @return Was the resposne an error?
    * @throws IOException on write error.
    */
   public boolean generateGraph(GraphQuery graphQuery,
                                Response esResponse,
                                EnumSet<Option> options,
                                RateUnit rateUnit,
                                HttpServletResponse response) throws IOException;

   /**
    * Generates a name response.
    * @param nameQuery The query.
    * @param esResponse The response from ES.
    * @param options Response options.
    * @param response The target HTTP response.
    * @return Was the response an error?
    * @throws IOException on write error.
    */
   public boolean generateNames(NameQuery nameQuery,
                                Response esResponse,
                                EnumSet<Option> options,
                                HttpServletResponse response) throws IOException;

   /**
    * Generates a stats response.
    * @param statsQuery The query.
    * @param esResponse The response from ES.
    * @param options Response options.
    * @param response The target HTTP response.
    * @return Was the response an error?
    * @throws IOException on write error.
    */
   public boolean generateStats(StatsQuery statsQuery,
                                Response esResponse,
                                EnumSet<Option> options,
                                HttpServletResponse response) throws IOException;
}