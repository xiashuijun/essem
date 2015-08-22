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