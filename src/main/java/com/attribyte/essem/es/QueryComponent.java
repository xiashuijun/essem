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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;

public abstract class QueryComponent {

   /**
    * Generate to an output stream.
    * @param os The output stream.
    * @throws IOException on write error.
    */
   public void generate(final OutputStream os) throws IOException {
      JsonGenerator generator = jsonFactory.createGenerator(os);
      generate(generator);
      generator.flush();
   }

   /**
    * Generate pretty JSON to an output stream.
    * @param os The output stream.
    * @throws IOException on write error.
    */
   public void generatePretty(final OutputStream os) throws IOException {
      JsonGenerator generator = jsonFactory.createGenerator(os).useDefaultPrettyPrinter();
      generate(generator);
      generator.flush();
   }

   /**
    * Writes the query as a "pretty" JSON string.
    * @return The JSON string.
    * @throws IOException on write error.
    */
   public String toPrettyJSON() throws IOException {
      StringWriter writer = new StringWriter();
      JsonGenerator generator = jsonFactory.createGenerator(writer).useDefaultPrettyPrinter();
      generate(generator);
      generator.flush();
      return writer.toString();
   }

   /**
    * Write the query as a JSON strung.
    * @return The JSON string.
    * @throws IOException on write error.
    */
   public String toJSON() throws IOException {
      StringWriter writer = new StringWriter();
      JsonGenerator generator = jsonFactory.createGenerator(writer);
      generate(generator);
      generator.flush();
      return writer.toString();
   }

   /**
    * Generates the JSON for this query using a generator.
    * @param generator The JSON generator.
    * @throws IOException on generate error.
    */
   public abstract void generate(final JsonGenerator generator) throws IOException;

   /**
    * The shared JSON factory.
    */
   protected static final JsonFactory jsonFactory = new JsonFactory();
}
