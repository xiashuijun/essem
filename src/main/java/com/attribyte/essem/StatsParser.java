/*
 * Copyright 2015 Attribyte, LLC
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

import com.attribyte.essem.model.graph.Stats;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.attribyte.essem.util.Util.getIntField;
import static com.attribyte.essem.util.Util.getDoubleField;

/**
 * Parse stats from the ES response to a <code>StatsQuery</code>.
 */
public class StatsParser {

   /**
    * Parses a stats response.
    * @param esObject The ES response object.
    * @return The parsed stats.
    */
   public static Stats parseStats(ObjectNode esObject, RateUnit rateUnit) {

      //This assumes the "extended stats" aggregation.

      JsonNode aggregations = esObject.get("aggregations");
      if(aggregations != null) {
         JsonNode stats = aggregations.get("stats");
         if(stats != null) {
            Stats newStats = new Stats(
                    getIntField(stats, "count", 0),
                    getDoubleField(stats, "min", 0.0),
                    getDoubleField(stats, "max", 0.0),
                    getDoubleField(stats, "avg", 0.0),
                    getDoubleField(stats, "sum", 0.0),
                    getDoubleField(stats, "sum_of_squares", 0.0),
                    getDoubleField(stats, "variance", 0.0),
                    getDoubleField(stats, "std_deviation", 0.0)
            );

            return rateUnit != null ? newStats.scale(rateUnit.mult) : newStats;
         } else {
            return Stats.EMPTY_STATS;
         }
      } else {
         return Stats.EMPTY_STATS;
      }
   }
}