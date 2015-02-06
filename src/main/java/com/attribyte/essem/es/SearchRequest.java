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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class SearchRequest extends QueryComponent {

   /**
    * The default limit.
    */
   public static int DEFAULT_LIMIT = 50;

   /**
    * Creates an empty builder.
    * @return The new builder.
    */
   public static Builder builder() {
      return new Builder();
   }

   /**
    * Builds an immutable <code>SearchRequest</code>.
    */
   public static class Builder {

      /**
       * Sets the query.
       * @param query The query.
       * @return A self-reference.
       */
      public Builder setQuery(QueryComponent query) {
         this.query = query;
         return this;
      }

      /**
       * Sets the aggregation.
       * @param aggregation The aggregation.
       * @return A self-reference.
       */
      public Builder addAggregation(Aggregation aggregation) {
         this.aggregations.add(aggregation);
         return this;
      }

      /**
       * Sets the start index.
       * @param start The start index.
       * @return A self-reference.
       */
      public Builder setStart(final int start) {
         this.start = start;
         return this;
      }

      /**
       * Sets the limit.
       * @param limit The limit.
       * @return A self-reference.
       */
      public Builder setLimit(final int limit) {
         this.limit = limit;
         return this;
      }


      /**
       * Disables the send of paging properties.
       * @return A self-reference.
       */
      public Builder disablePaging() {
         this.disablePaging = true;
         return this;
      }

      /**
       * Enables the send of paging properties.
       * @return A self-reference.
       */
      public Builder enablePaging() {
         this.disablePaging = false;
         return this;
      }

      /**
       * Sets the sort field.
       * @param sortField The sort field.
       * @return A self-reference.
       */
      public Builder setSortField(final String sortField) {
         this.sortField = sortField;
         return this;
      }

      /**
       * Sets the explain flag.
       * @param explain Should the query be explained?
       * @return A self-reference.
       */
      public Builder explain(final boolean explain) {
         this.explain = explain;
         return this;
      }

      /**
       * Sets ascending sort.
       * @return A self-reference.
       */
      public Builder ascendingSort() {
         isAscending = true;
         return this;
      }

      /**
       * Sets descending source.
       * @return A self-reference.
       */
      public Builder descendingSort() {
         isAscending = false;
         return this;
      }

      /**
       * Adds a field to be returned with the result.
       * @param field The field name.
       * @return A self-reference.
       */
      public Builder addField(final String field) {
         fields.add(field);
         return this;
      }

      /**
       * Adds a collection of fields to be returned with the result.
       * @param fields The fields.
       * @return A self-reference.
       */
      public Builder addFields(final Collection<String> fields) {
         this.fields.addAll(fields);
         return this;
      }

      /**
       * Enables the return of all fields.
       * @return A self-reference.
       */
      public Builder returnAllFields() {
         fields.clear();
         fields.add("*");
         return this;
      }

      /**
       * Enables the return of the original document, if available.
       * @return A self-reference.
       */
      public Builder enableSourceReturn() {
         retrieveSource = true;
         return this;
      }

      /**
       * Enables the return of the original document, if available.
       * @return A self-reference.
       */
      public Builder disableSourceReturn() {
         retrieveSource = false;
         return this;
      }

      /**
       * Sets the sort expression.
       * @param sort The sort.
       * @return A self-reference.
       */
      public Builder setSort(final Sort sort) {
         this.sort = sort;
         return this;
      }

      /**
       * Sets the query timeout in seconds.
       * @param timeoutSeconds The timeout in seconds.
       * @return A self-reference.
       */
      public Builder setTimeoutSeconds(final int timeoutSeconds) {
         this.timeoutSeconds = timeoutSeconds;
         return this;
      }

      /**
       * Builds the request.
       * @return The immutable request.
       */
      public SearchRequest build() {
         return new SearchRequest(start, limit, disablePaging, sort, timeoutSeconds, explain, sortField, isAscending, ImmutableSet.copyOf(fields),
                 retrieveSource, query, ImmutableList.copyOf(aggregations));
      }

      private int start = 0;
      private int limit = DEFAULT_LIMIT;
      private boolean disablePaging = false;
      private boolean explain;
      private String sortField;
      private boolean isAscending = false;
      private Set<String> fields = Sets.newHashSetWithExpectedSize(16);
      private boolean retrieveSource = false;
      private QueryComponent query;
      private List<Aggregation> aggregations = Lists.newArrayListWithExpectedSize(4);
      private Sort sort;
      private int timeoutSeconds = 0;
   }

   public SearchRequest(final int start, final int limit,
                        final boolean disablePaging,
                        final Sort sort,
                        final int timeoutSeconds,
                        final boolean explain, final String sortField, final boolean isAscending,
                        final ImmutableSet<String> fields, final boolean retrieveSource,
                        final QueryComponent query,
                        final ImmutableList<Aggregation> aggregations) {
      this.start = start;
      this.limit = limit;
      this.disablePaging = disablePaging;
      this.sort = sort;
      this.timeoutSeconds = timeoutSeconds;
      this.explain = explain;
      this.sortField = sortField;
      this.isAscending = isAscending;
      this.fields = fields;
      this.retrieveSource = retrieveSource;
      this.query = query;
      this.aggregations = aggregations;
   }

   @Override
   public void generate(final JsonGenerator generator) throws IOException {
      generator.writeStartObject();
      {
         if(!retrieveSource) generator.writeBooleanField("_source", false);
         if(explain) generator.writeBooleanField("explain", true);
         if(!disablePaging) {
            generator.writeNumberField("from", start);
            generator.writeNumberField("size", limit);
         }
         if(timeoutSeconds > 0) {
            generator.writeNumberField("timeout", timeoutSeconds * 1000);
         }
         if(sortField != null) generateSort(generator);
         if(fields != null && fields.size() > 0) generateFields(generator);
         if(query != null) {
            generator.writeFieldName("query");
            query.generate(generator);
         }

         if(sort != null) {
            sort.generate(generator);
         }

         if(aggregations != null && aggregations.size() > 0) {
            generator.writeObjectFieldStart(Aggregation.AGGREGATION_OBJECT_NAME);
            for(Aggregation aggregation : aggregations) {
               aggregation.generate(generator);
            }
            generator.writeEndObject();
         }
      }
      generator.writeEndObject();
   }

   private void generateSort(final JsonGenerator generator) throws IOException {

      generator.writeArrayFieldStart("sort");
      {
         generator.writeStartObject();
         {
            generator.writeObjectFieldStart(sortField);
            {
               generator.writeStringField("order", isAscending ? "asc" : "desc");
            }
            generator.writeEndObject();
         }
         generator.writeEndObject();
      }
      generator.writeEndArray();
   }

   private void generateFields(final JsonGenerator generator) throws IOException {
      generator.writeArrayFieldStart("fields");
      if(fields.contains("*")) {
         generator.writeString("*");
      } else {
         for(String field : fields) {
            generator.writeString(field);
         }
      }
      generator.writeEndArray();

   }

   public final int start;
   public final int limit;
   public final boolean disablePaging;
   public final Sort sort;
   public final boolean explain;
   public final String sortField;
   public final boolean isAscending;
   public final ImmutableSet<String> fields;
   public final boolean retrieveSource;
   public final QueryComponent query;
   public final ImmutableList<Aggregation> aggregations;
   public final int timeoutSeconds;
}
