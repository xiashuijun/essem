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
 * Matches terms with integer values.
 */
public class IntTermQuery extends Query {

   public IntTermQuery(final String term, final long value) {
      this.term = term;
      this.value = value;
   }

   @Override
   public void generate(final JsonGenerator generator) throws IOException {
      generator.writeStartObject();
      generator.writeObjectFieldStart("term");
      generator.writeNumberField(term, value);
      generator.writeEndObject();
      generator.writeEndObject();
   }

   public final String term;
   public final long value;
}
