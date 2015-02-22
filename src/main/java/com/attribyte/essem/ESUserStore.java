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

import com.attribyte.essem.model.StoredGraph;
import com.attribyte.essem.model.graph.MetricKey;
import com.attribyte.essem.query.StoredGraphQuery;
import com.attribyte.essem.util.Util;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;
import org.attribyte.api.Logger;
import org.attribyte.api.http.AsyncClient;
import org.attribyte.api.http.Request;
import org.attribyte.api.http.Response;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
    * Stores a graph.
    * @param graph The graph.
    * @return Was the graph stored?
    * @throws IOException on store error.
    */
   public boolean storeGraph(final StoredGraph graph) throws IOException {

      String indexName = graph.index + SUFFIX;

      try {
         URI indexURI = new URI(esEndpoint.uri.getScheme(),
                 esEndpoint.uri.getUserInfo(),
                 esEndpoint.uri.getHost(),
                 esEndpoint.uri.getPort(), "/" + indexName + "/" + STORED_KEY_TYPE + "/" + graph.id, null, null);

         Request esRequest = esEndpoint.putRequestBuilder(indexURI, graph.getAsJSON().getBytes(Charsets.UTF_8)).create();
         Response esResponse = httpClient.send(esRequest);
         boolean stored = esResponse.getStatusCode() / 100 == 2;
         if(stored) {
            cacheGraph(graph);
         }
         return stored;
      } catch(URISyntaxException use) {
         throw new AssertionError();
      }
   }

   /**
    * Deletes a graph.
    * @param graph The graph.
    * @return Was the graph deleted?
    * @throws IOException on delete error.
    */
   public boolean deleteGraph(final StoredGraph graph) throws IOException {

      removeGraph(graph);

      String indexName = graph.index + SUFFIX;

      try {
         URI indexURI = new URI(esEndpoint.uri.getScheme(),
                 esEndpoint.uri.getUserInfo(),
                 esEndpoint.uri.getHost(),
                 esEndpoint.uri.getPort(), "/" + indexName + "/" + STORED_KEY_TYPE + "/" + graph.id, null, null);

         Request esRequest = esEndpoint.deleteRequestBuilder(indexURI).create();
         Response esResponse = httpClient.send(esRequest);
         return esResponse.getStatusCode() / 100 == 2;
      } catch(URISyntaxException use) {
         throw new AssertionError();
      }
   }

   /**
    * Gets a previously saved graph.
    * @param index The index.
    * @param id The graph id.
    * @return The previously saved graph or <code>null</code> if none.
    * @throws IOException on retrieve error.
    */
   public StoredGraph getGraph(final String index, final String id) throws IOException {

      StoredGraph cachedGraph = recentGraphCache.getIfPresent(id);
      if(cachedGraph != null) {
         return cachedGraph;
      }

      String indexName = index + SUFFIX;

      try {
         URI indexURI = new URI(esEndpoint.uri.getScheme(),
                 esEndpoint.uri.getUserInfo(),
                 esEndpoint.uri.getHost(),
                 esEndpoint.uri.getPort(), "/" + indexName + "/_search", null, null);
         Request esRequest = esEndpoint.postRequestBuilder(indexURI, StoredGraphQuery.buildGraphRequest(id).toJSON().getBytes(Charsets.UTF_8)).create();
         Response esResponse = httpClient.send(esRequest);
         if(esResponse.getStatusCode() == 200) {
            ObjectNode keysObject = Util.mapper.readTree(Util.parserFactory.createParser(esResponse.getBody().toByteArray()));
            List<StoredGraph> graphs = StoredGraphParser.parseGraphs(keysObject);
            return graphs.size() > 0 ? graphs.get(0) : null;
         } else {
            return null;
         }
      } catch(URISyntaxException use) {
         throw new AssertionError();
      }
   }

   /**
    * Gets all graphs for a user.
    * @param index The index.
    * @param uid The user id.
    * @param start The start index.
    * @param limit The maximum returned.
    * @return The list of graphs.
    * @throws IOException on retrieve error.
    */
   public List<StoredGraph> getUserGraphs(final String index, final String uid, final int start, final int limit) throws IOException {

      String indexName = index + SUFFIX;

      try {
         URI indexURI = new URI(esEndpoint.uri.getScheme(),
                 esEndpoint.uri.getUserInfo(),
                 esEndpoint.uri.getHost(),
                 esEndpoint.uri.getPort(), "/" + indexName + "/_search", null, null);
         Request esRequest = esEndpoint.postRequestBuilder(indexURI, StoredGraphQuery.buildUserGraphsRequest(uid, start, limit).toJSON().getBytes(Charsets.UTF_8)).create();
         Response esResponse = httpClient.send(esRequest);
         switch(esResponse.getStatusCode()) {
            case 200:
               ObjectNode keysObject = Util.mapper.readTree(Util.parserFactory.createParser(esResponse.getBody().toByteArray()));
               List<StoredGraph> storedGraphs = StoredGraphParser.parseGraphs(keysObject);
               Map<MetricKey, StoredGraph> recentUserGraphs = recentUserGraphCache.getIfPresent(uid);
               if(start == 0 && recentUserGraphs != null && recentUserGraphs.size() > 0) {
                  Set<StoredGraph> combinedGraphs = Sets.newHashSet(storedGraphs);
                  combinedGraphs.addAll(recentUserGraphs.values());
                  storedGraphs = Lists.newArrayList(combinedGraphs);
                  Collections.sort(storedGraphs, Collections.reverseOrder(StoredGraph.createTimeComparator));
                  return storedGraphs.size() < limit ? storedGraphs : storedGraphs.subList(0, limit);
               } else {
                  return storedGraphs;
               }
            default:
               throw new IOException("Problem selecting user graphs (" + esResponse.getStatusCode() + ")");
         }
      } catch(URISyntaxException use) {
         throw new AssertionError();
      }
   }

   private final ESEndpoint esEndpoint;
   private final AsyncClient httpClient;
   private final ByteString schema;
   private final Logger logger;

   /**
    * Caches a graph.
    * @param graph The graph.
    * @return The input graph.
    */
   private StoredGraph cacheGraph(StoredGraph graph) {
      recentGraphCache.put(graph.id, graph);
      Map<MetricKey, StoredGraph> userMap = recentUserGraphCache.getIfPresent(graph.uid);
      if(userMap == null) {
         userMap = Maps.newConcurrentMap();
         recentUserGraphCache.put(graph.uid, userMap);
      }
      userMap.put(graph.key, graph);
      return graph;
   }

   /**
    * Removes a graph.
    * @param graph The graph.
    * @return The input graph.
    */
   private StoredGraph removeGraph(StoredGraph graph) {
      recentGraphCache.invalidate(graph.id);
      Map<MetricKey, StoredGraph> userMap = recentUserGraphCache.getIfPresent(graph.uid);
      if(userMap != null) {
         userMap.remove(graph.key);
      }
      return graph;
   }

   /**
    * Cache recent graphs so that they are available immediately
    * after save.
    */
   private final Cache<String, StoredGraph> recentGraphCache = CacheBuilder.newBuilder()
           .concurrencyLevel(4)
           .maximumSize(1000000)
           .expireAfterWrite(60, TimeUnit.MINUTES).build();

   /**
    * Cache the most recent version of a user's graph.
    */
   private final Cache<String, Map<MetricKey, StoredGraph>> recentUserGraphCache = CacheBuilder.newBuilder()
           .concurrencyLevel(4)
           .maximumSize(5000)
           .expireAfterWrite(60, TimeUnit.MINUTES).build();
}
