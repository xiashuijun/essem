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

public abstract class FieldAggregation extends QueryComponent implements Aggregation {

   public FieldAggregation(final String type,
                           final String name,
                           final String field) {
      this.type = type;
      this.name = name;
      this.field = field;
   }

   public void generate(final JsonGenerator generator) throws IOException {
      generator.writeObjectFieldStart(name);
      {
         generator.writeObjectFieldStart(type);
         {
            generator.writeStringField("field", field);
         }
         generator.writeEndObject();
      }
      generator.writeEndObject();
   }

   public String getType() {
      return type;
   }

   public final String name;
   public final String field;
   public final String type;

}
