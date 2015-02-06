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

import java.io.IOException;
import java.util.List;

/**
 * Creates one bucket per term.
 *
 * See: <a href="http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-aggregations-bucket-terms-aggregation.html">Terms Aggregation</a>
 */
public class TermsAggregation implements BucketAggregation {

   public TermsAggregation(final String name, final String field, final int size, final Order order) {
      this.name = name;
      this.field = field;
      this.size = size;
      this.subs = null;
      this.order = order;
   }

   public TermsAggregation(final String name, final String field, final int size, final Order order, final List<Aggregation> subs) {
      this.name = name;
      this.field = field;
      this.size = size;
      this.order = order;
      this.subs = subs != null ? ImmutableList.copyOf(subs) : null;
   }

   public TermsAggregation(final String name, final String field, final int size) {
      this.name = name;
      this.field = field;
      this.size = size;
      this.subs = null;
      this.order = Order.DOC_COUNT_DESC;
   }

   public TermsAggregation(final String name, final String field, final int size, final List<Aggregation> subs) {
      this.name = name;
      this.field = field;
      this.size = size;
      this.order = Order.DOC_COUNT_DESC;
      this.subs = subs != null ? ImmutableList.copyOf(subs) : null;
   }

   @Override
   public void generate(final JsonGenerator generator) throws IOException {
      generator.writeObjectFieldStart(name);
      {
         generator.writeObjectFieldStart("terms");
         {
            generator.writeStringField("field", field);
            generator.writeNumberField("size", size);
            switch(order) { //Doc count descending is default...
               case DOC_COUNT_ASC:
                  generator.writeObjectFieldStart("order");
                  generator.writeStringField("_count", "asc");
                  generator.writeEndObject();
                  break;
               case TERM_ASC:
                  generator.writeObjectFieldStart("order");
                  generator.writeStringField("_term", "asc");
                  generator.writeEndObject();
                  break;
               case TERM_DESC:
                  generator.writeObjectFieldStart("order");
                  generator.writeStringField("_term", "desc");
                  generator.writeEndObject();
                  break;
            }
         }
         generator.writeEndObject();
         if(subs != null && subs.size() > 0) {
            generator.writeObjectFieldStart(AGGREGATION_OBJECT_NAME);
            for(Aggregation sub : subs) {
               sub.generate(generator);
            }
            generator.writeEndObject();
         }
      }
      generator.writeEndObject();
   }

   public final String name;
   public final String field;
   public final int size;
   public final ImmutableList<Aggregation> subs;
   public final Order order;

}
