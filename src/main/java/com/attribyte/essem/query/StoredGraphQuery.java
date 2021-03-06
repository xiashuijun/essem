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

package com.attribyte.essem.query;

import com.attribyte.essem.es.BooleanQuery;
import com.attribyte.essem.es.SearchRequest;
import com.attribyte.essem.es.Sort;
import com.attribyte.essem.es.StringTermQuery;

import java.util.Collection;

/**
 * Creates ES query for stored keys.
 */
public class StoredGraphQuery {

   private static final Sort CREATED_DESC = new Sort(new Sort.SingleFieldSort("created", Sort.Direction.DESC));

   /**
    * Builds a search request for user graphs in descending order by create time.
    * @param uid The user id.
    * @param tags A list of tags. May be <code>null</code> or empty.
    * @param start The start index.
    * @param limit The maximum returned.
    */
   public static SearchRequest buildUserGraphsRequest(final String uid, final Collection<String> tags,
                                                      final int start, final int limit) {
      SearchRequest.Builder requestBuilder = SearchRequest.builder();
      BooleanQuery.Builder queryBuilder = BooleanQuery.builder();
      queryBuilder.mustMatch(new StringTermQuery("uid", uid));

      if(tags != null && !tags.isEmpty()) {
         BooleanQuery.Builder tagQueryBuilder = BooleanQuery.builder();
         for(String tag : tags) {
            tagQueryBuilder.shouldMatch(new StringTermQuery("tag", tag));
         }
         queryBuilder.mustMatch(tagQueryBuilder.build());
      }

      requestBuilder.returnAllFields();
      requestBuilder.setQuery(queryBuilder.build());
      requestBuilder.setSort(CREATED_DESC);
      requestBuilder.setStart(start).setLimit(limit);
      return requestBuilder.build();
   }

   /**
    * Builds a search request for a graph by id.
    * @param id The graph id.
    */
   public static SearchRequest buildGraphRequest(final String id) {
      SearchRequest.Builder requestBuilder = SearchRequest.builder();
      BooleanQuery.Builder queryBuilder = BooleanQuery.builder();
      queryBuilder.mustMatch(new StringTermQuery("_id", id));
      requestBuilder.returnAllFields();
      requestBuilder.setQuery(queryBuilder.build());
      requestBuilder.setStart(0).setLimit(1);
      return requestBuilder.build();
   }
}
