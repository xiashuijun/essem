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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.List;

public class BooleanQuery extends Query {

   /**
    * Build immutable boolean queries.
    * @return The builder.
    */
   public static Builder builder() {
      return new Builder();
   }

   public static class Builder {

      /**
       * Adds a query that must match.
       * @param query The query.
       * @return A self-reference.
       */
      public Builder mustMatch(final Query query) {
         mustTerms.add(query);
         return this;
      }

      /**
       * Adds a query that must not match.
       * @param query The query.
       * @return A self-reference.
       */
      public Builder mustNotMatch(final Query query) {
         mustNotTerms.add(query);
         return this;
      }

      /**
       * Adds a query that should match.
       * @param query The query.
       * @return A self-reference.
       */
      public Builder shouldMatch(final Query query) {
         shouldTerms.add(query);
         return this;
      }

      /**
       * Builds the query.
       * @return The query.
       */
      public BooleanQuery build() {
         return new BooleanQuery(ImmutableList.copyOf(mustTerms),
                 ImmutableList.copyOf(mustNotTerms), ImmutableList.copyOf(shouldTerms));
      }

      private final List<Query> mustTerms = Lists.newArrayListWithExpectedSize(4);
      private final List<Query> mustNotTerms = Lists.newArrayListWithExpectedSize(4);
      private final List<Query> shouldTerms = Lists.newArrayListWithExpectedSize(4);
   }

   public void generate(final JsonGenerator generator) throws IOException {

      if(isEmpty()) {
         return; //Write nothing...there's no constraint.
      }

      generator.writeStartObject();
      {
         generator.writeObjectFieldStart("bool");
         {
            if(mustTerms.size() > 0) {
               generator.writeArrayFieldStart("must");
               for(QueryComponent query : mustTerms) {
                  query.generate(generator);
               }
               generator.writeEndArray();
            }

            if(mustNotTerms.size() > 0) {
               generator.writeArrayFieldStart("must_not");
               for(QueryComponent query : mustNotTerms) {
                  query.generate(generator);
               }
               generator.writeEndArray();
            }

            if(shouldTerms.size() > 0) {
               generator.writeArrayFieldStart("should");
               for(QueryComponent query : shouldTerms) {
                  query.generate(generator);
               }
               generator.writeEndArray();
            }
         }
         generator.writeEndObject();
      }
      generator.writeEndObject();

   }

   boolean isEmpty() {
      return mustTerms.size() == 0 && mustNotTerms.size() == 0 && shouldTerms.size() == 0;
   }

   private BooleanQuery(final ImmutableList<Query> mustTerms,
                        final ImmutableList<Query> mustNotTerms,
                        final ImmutableList<Query> shouldTerms) {
      this.mustTerms = mustTerms;
      this.mustNotTerms = mustNotTerms;
      this.shouldTerms = shouldTerms;
   }

   private final ImmutableList<Query> mustTerms;
   private final ImmutableList<Query> mustNotTerms;
   private final ImmutableList<Query> shouldTerms;
}
