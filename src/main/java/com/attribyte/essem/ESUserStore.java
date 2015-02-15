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

import com.attribyte.essem.model.StoredKey;
import com.attribyte.essem.query.StoredKeyQuery;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.protobuf.ByteString;
import org.attribyte.api.Logger;
import org.attribyte.api.http.AsyncClient;
import org.attribyte.api.http.Request;
import org.attribyte.api.http.Response;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class ESUserStore {

   /**
    * Creates a user store.
    * @param esEndpoint The ES endpoint.
    * @param schema The index schema.
    * @param httpClient The (Async) HTTP client.
    * @param logger The logger.
    */
   public ESUserStore(final ESEndpoint esEndpoint,
                     final ByteString schema,
                     final AsyncClient httpClient,
                     final Logger logger) {
      this.esEndpoint = esEndpoint;
      this.schema = schema;
      this.httpClient = httpClient;
      this.logger = logger;
   }

   /**
    * The suffix appended to the metrics index name to create the user index.
    */
   public static final String SUFFIX = "_user";

   /**
    * The type for stored keys.
    */
   public static final String STORED_KEY_TYPE = "graph_key";

   /**
    * Creates the user store.
    * @param index The index.
    * @return Was the store created?
    * @throws IOException on create error.
    */
   public boolean createStore(final String index) throws IOException {

      String indexName = index + SUFFIX;

      try {
         URI indexURI = new URI(esEndpoint.uri.getScheme(),
                 esEndpoint.uri.getUserInfo(),
                 esEndpoint.uri.getHost(),
                 esEndpoint.uri.getPort(), "/" + indexName, null, null);

         Request esRequest = esEndpoint.putRequestBuilder(indexURI, schema.toByteArray()).create();
         Response esResponse = httpClient.send(esRequest);
         boolean created = esResponse.getStatusCode() / 100 == 2;
         if(created) {
            logger.info("Created ES index, '" + indexName + "'");
         } else {
            logger.warn("ES Index, '" + indexName + "' exists!");
         }
         return created;
      } catch(URISyntaxException use) {
         throw new AssertionError();
      }
   }

   /**
    * Stores a key.
    * @param key The key.
    * @return Was the key stored?
    * @throws IOException on store error.
    */
   public boolean storeKey(final StoredKey key) throws IOException {

      String indexName = key.index + SUFFIX;

      try {
         URI indexURI = new URI(esEndpoint.uri.getScheme(),
                 esEndpoint.uri.getUserInfo(),
                 esEndpoint.uri.getHost(),
                 esEndpoint.uri.getPort(), "/" + indexName + "/" + STORED_KEY_TYPE + "/" + key.id, null, null);

         Request esRequest = esEndpoint.putRequestBuilder(indexURI, key.getAsJSON().getBytes(Charsets.UTF_8)).create();

         System.out.println(esRequest.toString());

         Response esResponse = httpClient.send(esRequest);

         System.out.println(esResponse.toString());

         return esResponse.getStatusCode() / 100 == 2;
      } catch(URISyntaxException use) {
         throw new AssertionError();
      }
   }

   /**
    * Deletes a key.
    * @param key The key.
    * @return Was the key deleted?
    * @throws IOException on delete error.
    */
   public boolean deleteKey(final StoredKey key) throws IOException {

      String indexName = key.index + SUFFIX;

      try {
         URI indexURI = new URI(esEndpoint.uri.getScheme(),
                 esEndpoint.uri.getUserInfo(),
                 esEndpoint.uri.getHost(),
                 esEndpoint.uri.getPort(), "/" + indexName + "/" + STORED_KEY_TYPE + "/" + key.id, null, null);

         Request esRequest = esEndpoint.deleteRequestBuilder(indexURI).create();
         Response esResponse = httpClient.send(esRequest);
         return esResponse.getStatusCode() / 100 == 2;
      } catch(URISyntaxException use) {
         throw new AssertionError();
      }
   }

   /**
    * Gets all keys for a user.
    * @param index The index.
    * @param uid The user id.
    * @return The list of keys.
    * @throws IOException on retrieve error.
    */
   public List<StoredKey> getUserKeys(final String index, final String uid) throws IOException {

      String indexName = index + SUFFIX;

      try {
         URI indexURI = new URI(esEndpoint.uri.getScheme(),
                 esEndpoint.uri.getUserInfo(),
                 esEndpoint.uri.getHost(),
                 esEndpoint.uri.getPort(), "/" + indexName + "/" + STORED_KEY_TYPE, null, null);
         StoredKeyQuery query = new StoredKeyQuery(uid);
         Request esRequest = esEndpoint.postRequestBuilder(indexURI, query.searchRequest.toJSON().getBytes(Charsets.UTF_8)).create();
         Response esResponse = httpClient.send(esRequest);
         switch(esResponse.getStatusCode()) {
            case 200:
               ObjectNode keysObject = Util.mapper.readTree(Util.parserFactory.createParser(esResponse.getBody().toByteArray()));
               return StoredKeyParser.parseKeys(keysObject);
            default:
               throw new IOException("Problem selecting user keys (" + esResponse.getStatusCode() + ")");
         }
      } catch(URISyntaxException use) {
         throw new AssertionError();
      }
   }

   private final ESEndpoint esEndpoint;
   private final AsyncClient httpClient;
   private final ByteString schema;
   private final Logger logger;

}
