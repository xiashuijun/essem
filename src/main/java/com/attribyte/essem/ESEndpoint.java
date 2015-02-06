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

package com.attribyte.essem;

import org.attribyte.api.http.DeleteRequestBuilder;
import org.attribyte.api.http.GetRequestBuilder;
import org.attribyte.api.http.PostRequestBuilder;
import org.attribyte.api.http.PutRequestBuilder;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * The Elasticsearch endpoint URL and auth, if required.
 */
public class ESEndpoint {

   /**
    * Creates the endpoint.
    * @param uri The endpoint URI.
    * @param auth The endpoint auth.
    * @throws java.net.URISyntaxException on invalid URI.
    */
   ESEndpoint(final String uri, final String auth) throws URISyntaxException {
      this.uri = new URI(uri);
      this.auth = auth;
   }

   /**
    * Creates a GET request builder with authentication, if required.
    * @param requestURI The request URI.
    * @return The builder.
    */
   final GetRequestBuilder getRequestBuilder(final URI requestURI) {
      GetRequestBuilder builder = new GetRequestBuilder(requestURI);
      BasicAuth.addAuth(builder, auth);
      return builder;
   }

   /**
    * Creates a PUT request builder with authentication, if required.
    * @param requestURI The request URI.
    * @param body The body.
    * @return The builder.
    */
   final PutRequestBuilder putRequestBuilder(final URI requestURI, final byte[] body) {
      PutRequestBuilder builder = new PutRequestBuilder(requestURI, body);
      BasicAuth.addAuth(builder, auth);
      return builder;
   }

   /**
    * Creates a POST request builder with authentication, if required.
    * @param requestURI The request URI.
    * @param body The body.
    * @return The builder.
    */
   final PostRequestBuilder postRequestBuilder(final URI requestURI, final byte[] body) {
      PostRequestBuilder builder = new PostRequestBuilder(requestURI, body);
      BasicAuth.addAuth(builder, auth);
      return builder;
   }

   /**
    * Creates a DELETE request builder with authentication, if required.
    * @param requestURI The request URI.
    * @return The builder.
    */
   final DeleteRequestBuilder deleteRequestBuilder(final URI requestURI) {
      DeleteRequestBuilder builder = new DeleteRequestBuilder(requestURI);
      BasicAuth.addAuth(builder, auth);
      return builder;
   }

   /**
    * Builds the URI to search a specified index.
    * @param indexName The index name.
    * @return The URI.
    */
   final URI buildIndexURI(final String indexName) {
      try {
         return new URI(uri.getScheme(),
                 uri.getUserInfo(),
                 uri.getHost(),
                 uri.getPort(), "/" + indexName + "/_search", null, null);
      } catch(URISyntaxException use) {
         throw new AssertionError();
      }
   }

   /**
    * Builds the URI to get stats for a specified index.
    * @param indexName The index name.
    * @return The URI.
    */
   final URI buildIndexStatsURI(final String indexName) {
      try {

         if(indexName != null) {
            return new URI(uri.getScheme(),
                    uri.getUserInfo(),
                    uri.getHost(),
                    uri.getPort(), "/" + indexName + "/_stats", null, null);
         } else {
            return new URI(uri.getScheme(),
                    uri.getUserInfo(),
                    uri.getHost(),
                    uri.getPort(), "/_stats", null, null);
         }
      } catch(URISyntaxException use) {
         throw new AssertionError();
      }
   }

   /**
    * Builds the URL to delete by query.
    * <p>
    * The query JSON is added as the 'source" parameter.
    * </p>
    * @param indexName The index name.
    * @param jsonBody The body to add.
    * @return The URI.
    */
   final URI buildDeleteByQueryURI(final String indexName, final String jsonBody) {
      try {
         return new URI(uri.getScheme(),
                 uri.getUserInfo(),
                 uri.getHost(),
                 uri.getPort(), "/" + indexName + "/_query", "source=" + jsonBody, null);
      } catch(URISyntaxException use) {
         throw new AssertionError();
      }
   }

   /**
    * Builds the URI to get stats for the index cluster.
    */
   final URI buildClusterStatsURI() {
      try {
         return new URI(uri.getScheme(),
                 uri.getUserInfo(),
                 uri.getHost(),
                 uri.getPort(), "/_cluster/stats", null, null);
      } catch(URISyntaxException use) {
         throw new AssertionError();
      }
   }

   final URI uri;
   final String auth;
}
