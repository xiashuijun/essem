/*
 * Copyright 2014, 2015 Attribyte, LLC
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

package com.attribyte.essem.model;

public class GraphRange {

   /**
    * Creates the range.
    * @param name The name: hour, day, month, etc.
    * @param startMillis The range start timestamp.
    * @param endMillis The range end timestamp.
    */
   public GraphRange(final String name, final long startMillis, final long endMillis) {
      this.name = name;
      this.startMillis = startMillis;
      this.endMillis = endMillis;
   }

   /**
    * The range name: hour, day, etc.
    */
   public final String name;

   /**
    * The start timestamp.
    */
   public final long startMillis;

   /**
    * The end timestamp.
    */
   public final long endMillis;
}
