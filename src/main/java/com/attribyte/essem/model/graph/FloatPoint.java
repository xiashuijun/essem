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

public class FloatPoint extends Point {

   /**
    * Creates a point with a float value.
    * @param timestamp The timestamp.
    * @param value The value.
    * @param observations The number of observations associated with the point.
    */
   public FloatPoint(final long timestamp, final double value, final int observations) {
      super(timestamp, observations);
      this.value = value;
   }

   /**
    * Creates a point with a float value.
    * @param timestamp The timestamp.
    * @param value The value.
    */
   public FloatPoint(final long timestamp, final double value) {
      super(timestamp);
      this.value = value;
   }

   @Override
   public double asDouble() {
      return value;
   }

   @Override
   public long asLong() {
      return (long)value;
   }

   @Override
   public boolean isIntegralNumber() {
      return false;
   }

   /**
    * The value.
    */
   public double value;
}
