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

package com.attribyte.essem;

import com.attribyte.essem.es.SearchRequest;
import com.attribyte.essem.model.graph.MetricKey;
import com.attribyte.essem.query.GraphQuery;
import com.attribyte.essem.query.HistogramQuery;
import com.attribyte.essem.query.NameQuery;
import com.attribyte.essem.query.StatsQuery;
import com.attribyte.essem.util.Util;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.attribyte.api.http.AsyncClient;
import org.attribyte.api.http.Request;
import org.attribyte.api.http.RequestOptions;
import org.attribyte.api.http.Response;
import org.attribyte.essem.metrics.Timer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;

import static com.attribyte.essem.util.Util.splitPath;

/**
 * Accept and enqueue reports.
 */
public class APIServlet extends HttpServlet implements MetricSet {

   /**
    * The default range ('day').
    */
   public static final String DEFAULT_RANGE = "day";

   /**
    * The rate unit parameter ('rateUnit').
    */
   public static final String RATE_UNIT_PARAM = "rateUnit";

   /**
    * Creates the servlet.
    * @param indexAuthorization Authorizes index access based on name.
    */
   public APIServlet(final ESEndpoint esEndpoint,
                     final AsyncClient httpClient,
                     final RequestOptions requestOptions,
                     final IndexAuthorization indexAuthorization,
                     final ResponseGenerator responseGenerator) {
      this.esEndpoint = esEndpoint;
      this.httpClient = httpClient;
      this.requestOptions = requestOptions;
      this.indexAuthorization = indexAuthorization;
      this.responseGenerator = responseGenerator;
      this.graphTimer = new Timer();
      this.graphErrors = new Meter();
      this.nameTimer = new Timer();
      this.nameErrors = new Meter();
      this.statsTimer = new Timer();
      this.statsErrors = new Meter();
      this.metrics = ImmutableMap.<String, Metric>builder()
              .put("graph-requests", graphTimer)
              .put("graph-request-errors", graphErrors)
              .put("name-requests", nameTimer)
              .put("name-request-errors", nameErrors)
              .put("stats-requests", statsTimer)
              .put("stats-request-errors", statsErrors)
              .build();
   }

   /**
    * Allowed operations.
    */
   private enum Op {
      GRAPH, METRIC, STATS, HISTOGRAM
   }

   //http://localhost:8086/pass/test/graph?aggregateOn=name&downsampleTo=second&downsampleFn=avg&name=requests&field=p99&range=day&limit=5000&host=app01

   /**
    * Valid operations.
    */
   private static ImmutableMap<String, Op> ops =
           ImmutableMap.of("graph", Op.GRAPH, "metric", Op.METRIC, "stats", Op.STATS, "histogram", Op.HISTOGRAM);

   @Override
   protected void doGet(final HttpServletRequest request,
                        final HttpServletResponse response) throws IOException {

      Iterator<String> path = splitPath(request).iterator();
      if(!path.hasNext()) {
         response.sendError(HttpServletResponse.SC_BAD_REQUEST);
         return;
      }

      String index = path.next();
      if(indexAuthorization != null && !indexAuthorization.isAuthorized(index, request)) {
         indexAuthorization.sendUnauthorized(index, response);
         return;
      }

      Op op = ops.get(path.hasNext() ? path.next().toLowerCase() : "graph");
      if(op == null) {
         response.sendError(HttpServletResponse.SC_NOT_FOUND);
         return;
      }

      try {
         final boolean responseGenerated;
         switch(op) {
            case GRAPH: {
               final Timer.Context ctx = graphTimer.time();
               try {
                  GraphQuery graphQuery = new GraphQuery(request, DEFAULT_RANGE);
                  SearchRequest query = graphQuery.searchRequest;
                  if(graphQuery.error != null) {
                     response.sendError(HttpServletResponse.SC_BAD_REQUEST, graphQuery.error);
                     markError(op);
                     return;
                  }

                  String esQuery = query.toJSON();
                  Request esRequest = esEndpoint.postRequestBuilder(esEndpoint.buildIndexURI(index),
                          esQuery.getBytes(Charsets.UTF_8)).create();
                  Response esResponse = httpClient.send(esRequest, requestOptions);

                  if(esResponse.getStatusCode() == HttpServletResponse.SC_OK) {
                     RateUnit rateUnit = RateUnit.fromString(request.getParameter(RATE_UNIT_PARAM));
                     responseGenerated = responseGenerator.generateGraph(graphQuery, esResponse, responseOptions(request), rateUnit, response);
                  } else {
                     reportBackendError(esResponse, response);
                     responseGenerated = false;
                  }
               } finally {
                  ctx.stop();
               }
            }
            break;
            case METRIC: {
               final Timer.Context ctx = nameTimer.time();
               try {
                  NameQuery nameQuery = new NameQuery(request, DEFAULT_RANGE);
                  if(nameQuery.error != null) {
                     response.sendError(HttpServletResponse.SC_BAD_REQUEST, nameQuery.error);
                     markError(op);
                     return;
                  }
                  String esQuery = nameQuery.searchRequest.toJSON();
                  Request esRequest = esEndpoint.postRequestBuilder(esEndpoint.buildIndexURI(index),
                          esQuery.getBytes(Charsets.UTF_8)).create();
                  Response esResponse = httpClient.send(esRequest, requestOptions);
                  if(esResponse.getStatusCode() == HttpServletResponse.SC_OK) {
                     responseGenerated = responseGenerator.generateNames(nameQuery, esResponse, responseOptions(request), response);
                  } else {
                     reportBackendError(esResponse, response);
                     responseGenerated = false;
                  }
               } finally {
                  ctx.stop();
               }
            }
            break;
            case STATS: {
               final Timer.Context ctx = statsTimer.time();
               try {
                  String range = Strings.nullToEmpty(request.getParameter("range")).trim();
                  if(range.length() == 0) range = "day";
                  long startTimestamp = Util.getLongParameter(request, "startTimestamp", 0L);
                  long endTimestamp = Util.getLongParameter(request, "endTimestamp", 0L);
                  StatsQuery statsQuery = new StatsQuery(MetricKey.parseKey(request), range, startTimestamp, endTimestamp);
                  String esQuery = statsQuery.searchRequest.toJSON();
                  Request esRequest = esEndpoint.postRequestBuilder(esEndpoint.buildIndexURI(index),
                          esQuery.getBytes(Charsets.UTF_8)).create();
                  Response esResponse = httpClient.send(esRequest, requestOptions);
                  if(esResponse.getStatusCode() == HttpServletResponse.SC_OK) {
                     responseGenerated = responseGenerator.generateStats(statsQuery, esResponse, responseOptions(request), response);
                  } else {
                     reportBackendError(esResponse, response);
                     responseGenerated = false;
                  }
               } finally {
                  ctx.stop();
               }
               break;
            }
            case HISTOGRAM: {
               HistogramQuery query = new HistogramQuery(request, DEFAULT_RANGE);
               if(query.error != null) {
                  response.sendError(HttpServletResponse.SC_BAD_REQUEST, query.error);
                  markError(op);
                  return;
               }

               String esQuery = query.searchRequest.toJSON();
               Request esRequest = esEndpoint.postRequestBuilder(esEndpoint.buildIndexURI(index),
                       esQuery.getBytes(Charsets.UTF_8)).create();
               Response esResponse = httpClient.send(esRequest, requestOptions);
               if(esResponse.getStatusCode() == HttpServletResponse.SC_OK) {
                  responseGenerated = responseGenerator.generateHistogram(query, esResponse, responseOptions(request), response);
               } else {
                  reportBackendError(esResponse, response);
                  responseGenerated = false;
               }
               break;
            }

            default: {
               response.sendError(HttpServletResponse.SC_NOT_FOUND);
               responseGenerated = true;
            }
         }

         if(!responseGenerated) {
            markError(op);
         }

      } catch(IOException ioe) {
         markError(op);
         ioe.printStackTrace();
         throw ioe;
      } catch(Throwable t) {
         markError(op);
         t.printStackTrace();
         response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
   }

   /**
    * Gets response options from request parameters.
    * @param request The request.
    * @return The response options.
    */
   private EnumSet<ResponseGenerator.Option> responseOptions(final HttpServletRequest request) {
      boolean explain = Strings.nullToEmpty(request.getParameter("explain")).equalsIgnoreCase("true");
      boolean emptyBins = Strings.nullToEmpty(request.getParameter("emptyBins")).equalsIgnoreCase("true");

      if(explain || emptyBins) {
         if(explain && emptyBins) {
            return EnumSet.of(ResponseGenerator.Option.EXPLAIN, ResponseGenerator.Option.EMPTY_BINS);
         } else if(explain) {
            return EnumSet.of(ResponseGenerator.Option.EXPLAIN);
         } else {
            return EnumSet.of(ResponseGenerator.Option.EMPTY_BINS);
         }
      } else {
         return EnumSet.noneOf(ResponseGenerator.Option.class);
      }
   }

   /**
    * Logs an error response from ES.
    * @param esResponse The response.
    */
   private void reportBackendError(final Response esResponse, final HttpServletResponse response) throws IOException {
      log("Search error: " + esResponse.getBody().toStringUtf8());
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.setContentType(Util.JSON_CONTENT_TYPE_HEADER);
      response.getOutputStream().write(esResponse.getBody().toByteArray()); //TODO: maybe don't return the raw response...
      response.getOutputStream().flush();
   }

   /**
    * Marks an error for an operation.
    * @param op The operation.
    */
   private void markError(final Op op) {
      switch(op) {
         case GRAPH: graphErrors.mark(); break;
         case METRIC: nameErrors.mark(); break;
         case STATS: statsErrors.mark(); break;
      }
   }

   /**
    * Determines if a request is authorized for access to an index.
    */
   private final IndexAuthorization indexAuthorization;


   /**
    * The elasticsearch endpoint for search.
    */
   private final ESEndpoint esEndpoint;

   /**
    * The HTTP client.
    */
   private final AsyncClient httpClient;

   /**
    * The request options for ES HTTP requests.
    */
   private final RequestOptions requestOptions;

   /**
    * Generates a client response from an ES query response.
    */
   private final ResponseGenerator responseGenerator;

   /**
    * Times all graph requests.
    */
   private final Timer graphTimer;

   /**
    * Records all graph request errors.
    */
   private final Meter graphErrors;

   /**
    * Times all metric name requests.
    */
   private final Timer nameTimer;

   /**
    * Records all metric name request errors.
    */
   private final Meter nameErrors;

   /**
    * Times all stats requests.
    */
   private final Timer statsTimer;

   /**
    * Records all stats request errors.
    */
   private final Meter statsErrors;

   /**
    * An immutable map of all metrics.
    */
   private final ImmutableMap<String, Metric> metrics;

   @Override
   public Map<String, Metric> getMetrics() {
      return metrics;
   }
}