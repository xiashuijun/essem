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

public interface MetricAggregation extends Aggregation {

   /**
    * Create a new instance using an existing instance.
    * @param name The name.
    * @param field The field.
    * @return The new instance.
    */
   public MetricAggregation newInstance(final String name, final String field);


   /**
    * Gets the aggregation type.
    * @return The type.
    */
   public String getType();

}
