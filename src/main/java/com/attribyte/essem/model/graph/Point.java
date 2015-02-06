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

package com.attribyte.essem.model.graph;

/**
 * A point on a time-series graph.
 */
abstract public class Point {

   /**
    * Creates a point.
    * @param timestamp The point's timestamp.
    */
   protected Point(final long timestamp, final int observations) {
      this.timestamp = timestamp;
      this.observations = observations;
   }


   /**
    * Creates a point.
    * @param timestamp The point's timestamp.
    */
   protected Point(final long timestamp) {
      this.timestamp = timestamp;
      this.observations = 1;
   }

   /**
    * Gets the value as a double.
    * @return The value.
    */
   public abstract double asDouble();

   /**
    * Gets the value as a long integer.
    * @return The value.
    */
   public abstract long asLong();

   /**
    * Does this point hold a number that can be converted exactly to a long?
    * @return Is the point an integral number?
    */
   public abstract boolean isIntegralNumber();

   /**
    * The timestamp.
    */
   public final long timestamp;

   /**
    * The number of observations associated with this point (maybe the value is the average/max/min of N observations).
    */
   public final int observations;
}
