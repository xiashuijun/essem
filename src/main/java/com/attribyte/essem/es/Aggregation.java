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
 * Indicates an aggregation.
 */
public interface Aggregation {

   /**
    * The aggregation object name ('aggs').
    */
   public static final String AGGREGATION_OBJECT_NAME = "aggs";

   /**
    * Generates the JSON for this aggregation.
    * @param generator The JSON generator.
    * @throws IOException on generate error.
    */
   public void generate(final JsonGenerator generator) throws IOException;
}
