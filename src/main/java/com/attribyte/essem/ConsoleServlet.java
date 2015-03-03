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

import com.attribyte.essem.model.Application;
import com.attribyte.essem.model.Dashboard;
import com.attribyte.essem.model.DisplayTZ;
import com.attribyte.essem.model.DownsampleFunction;
import com.attribyte.essem.model.Metric;
import com.attribyte.essem.model.MetricGraph;
import com.attribyte.essem.model.Sort;
import com.attribyte.essem.model.GraphRange;
import com.attribyte.essem.model.StoredGraph;
import com.attribyte.essem.model.graph.MetricKey;
import com.attribyte.essem.model.graph.Stats;
import com.attribyte.essem.model.index.IndexStats;
import com.attribyte.essem.query.Fields;
import com.attribyte.essem.query.GraphQuery;
import com.attribyte.essem.query.QueryBase;
import com.attribyte.essem.query.StatsQuery;
import com.attribyte.essem.util.Util;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.attribyte.api.Logger;
import org.attribyte.api.http.AsyncClient;
import org.attribyte.api.http.Request;
import org.attribyte.api.http.RequestOptions;
import org.attribyte.api.http.Response;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.stringtemplate.v4.DateRenderer;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupDir;
import org.stringtemplate.v4.STGroupFile;
import org.stringtemplate.v4.misc.STMessage;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.attribyte.essem.util.Util.splitPath;

public class ConsoleServlet extends HttpServlet {

   public ConsoleServlet(final ESEndpoint esEndpoint,
                         final ESUserStore userStore,
                         final ServletContextHandler rootContext,
                         final IndexAuthorization indexAuthorization,
                         final String templateDirectory,
                         final String dashboardTemplateDirectory,
                         String assetDirectory,
                         final Collection<String> allowedAssetPaths,
                         final Collection<String> allowedIndexes,
                         final List<DisplayTZ> zones,
                         final AsyncClient client,
                         final RequestOptions requestOptions,
                         final Logger logger,
                         final boolean debug) {

      this.esEndpoint = esEndpoint;
      this.userStore = userStore;
      this.indexAuthorization = indexAuthorization;
      this.templateDirectory = templateDirectory;
      this.dashboardTemplateDirectory = dashboardTemplateDirectory;
      this.allowedIndexes = ImmutableList.copyOf(allowedIndexes);
      this.zones = ImmutableList.copyOf(zones);

      ImmutableMap.Builder<String, DisplayTZ> zoneMapBuilder = ImmutableMap.builder();
      for(DisplayTZ tz : zones) {
         zoneMapBuilder.put(tz.id, tz);
      }
      this.zoneMap = zoneMapBuilder.build();

      this.client = client;
      this.requestOptions = requestOptions;
      this.logger = logger;
      this.debug = debug;
      this.templateGroup = debug ? null : loadTemplates();
      this.dashboardTemplateGroup = debug ? null : loadDashboardTemplates();

      if(!assetDirectory.endsWith("/")) {
         assetDirectory = assetDirectory + "/";
      }

      rootContext.addAliasCheck(new ContextHandler.ApproveAliases());
      rootContext.setInitParameter("org.eclipse.jetty.servlet.Default.resourceBase", assetDirectory);
      rootContext.setInitParameter("org.eclipse.jetty.servlet.Default.acceptRanges", "false");
      rootContext.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
      rootContext.setInitParameter("org.eclipse.jetty.servlet.Default.welcomeServlets", "true");
      rootContext.setInitParameter("org.eclipse.jetty.servlet.Default.redirectWelcome", "false");
      rootContext.setInitParameter("org.eclipse.jetty.servlet.Default.aliases", "true");
      rootContext.setInitParameter("org.eclipse.jetty.servlet.Default.gzip", "true");

      DefaultServlet defaultServlet = new DefaultServlet();
      for(String path : allowedAssetPaths) {
         logger.info("Enabling asset path: " + path);
         rootContext.addServlet(new ServletHolder(defaultServlet), path);
      }

      this.applicationCache = new ApplicationCache(client, requestOptions, esEndpoint, logger);

      for(String index : allowedIndexes) {
         try {
            List<Application> apps = this.applicationCache.getApplications(index);
            if(apps != null && apps.size() > 0) {
               logger.info("Found " + apps.size() + " apps for '" + index + "'");
               for(Application app : apps) {
                  long start = System.currentTimeMillis();
                  logger.info("Priming '" + app.name + "'");
                  int totalMetrics = app.getMetricsCount();
                  int activeMetrics = this.applicationCache.removeBoringMetrics(app).getMetricsCount();
                  logger.info("Found " + activeMetrics + " active metrics of " + totalMetrics);
                  logger.info("Primed in " + (System.currentTimeMillis() - start) + " ms");
               }

            }
         } catch(IOException ioe) {
            logger.error("Problem getting applications", ioe);
         }
      }
   }

   static final class ErrorListener implements STErrorListener {

      ErrorListener(final Logger logger) {
         this.logger = logger;
      }

      public void compileTimeError(STMessage msg) {
         logger.error("Template compilation error: " + msg);
      }

      public void runTimeError(STMessage msg) {
         logger.error("Template rendering error: " + msg);
      }

      public void IOError(STMessage msg) {
         logger.error("Template load error: " + msg);
      }

      public void internalError(STMessage msg) {
         logger.error("Template system error: " + msg);
      }

      private final Logger logger;
   }

   /**
    * Allowed operations.
    */
   private enum Op {
      DEFAULT,
      DEFAULT_WITH_APP,
      APPS,
      METRICS,
      GRAPHS,
      INDEX_STATS,
      FIELD_STATS,
      SAVEGRAPH,
      DELETEGRAPH,
      USERGRAPH,
      USERGRAPHS,
      DASH
   }

   /**
    * Valid operations.
    */
   private static ImmutableMap<String, Op> ops = ImmutableMap.<String, Op>builder()
           .put("", Op.DEFAULT)
           .put("apps", Op.APPS)
           .put("metrics", Op.METRICS)
           .put("graphs", Op.GRAPHS)
           .put("fstats", Op.FIELD_STATS)
           .put("savegraph", Op.SAVEGRAPH)
           .put("usergraph", Op.USERGRAPH)
           .put("usergraphs", Op.USERGRAPHS)
           .put("dash", Op.DASH)
           .put("stats", Op.INDEX_STATS).build();

   /**
    * Valid PUT/POST operations.
    */
   private static ImmutableMap<String, Op> putOps = ImmutableMap.of(
           "savegraph", Op.SAVEGRAPH);

   /**
    * Valid DELETE operations.
    */
   private static ImmutableMap<String, Op> deleteOps = ImmutableMap.of(
           "deletegraph", Op.DELETEGRAPH);

   @Override
   protected void doPost(final HttpServletRequest request,
                         final HttpServletResponse response) throws IOException {
      doPut(request, response);
   }

   @Override
   protected void doDelete(final HttpServletRequest request,
                           final HttpServletResponse response) throws IOException {

      Iterator<String> path = splitPath(request).iterator();
      if(!path.hasNext()) {
         sendError(request, response, HttpServletResponse.SC_BAD_REQUEST);
         return;
      }

      final String index = path.next();
      final IndexAuthorization.Auth auth;

      if(indexAuthorization == null) {
         auth = IndexAuthorization.Auth.SYSTEM;
      } else {
         auth = indexAuthorization.getAuth(index, request);
         if(!auth.isAuthorized) {
            indexAuthorization.sendUnauthorized(index, response);
            return;
         }
      }

      String opPath = path.hasNext() ? path.next().toLowerCase() : "";
      Op op = deleteOps.get(opPath);
      if(op == null) {
         response.sendError(404, "Not Found");
         return;
      }

      switch(op) {
         case DELETEGRAPH:
            if(!path.hasNext()) {
               sendError(request, response, HttpServletResponse.SC_BAD_REQUEST, "An 'id' must be specified");
               return;
            }
            String id = path.hasNext() ? path.next() : null;
            doUserGraphDelete(request, auth, index, id, response);
            break;
         default:
            sendError(request, response, HttpServletResponse.SC_NOT_FOUND);

      }

   }

   @Override
   protected void doPut(final HttpServletRequest request,
                        final HttpServletResponse response) throws IOException {

      Iterator<String> path = splitPath(request).iterator();
      if(!path.hasNext()) {
         sendError(request, response, HttpServletResponse.SC_BAD_REQUEST);
         return;
      }

      final String index = path.next();
      final IndexAuthorization.Auth auth;

      if(indexAuthorization == null) { //No auth configured
         auth = IndexAuthorization.Auth.SYSTEM;
      } else {
         auth = indexAuthorization.getAuth(index, request);
         if(!auth.isAuthorized) {
            indexAuthorization.sendUnauthorized(index, response);
            return;
         }
      }

      String opPath = path.hasNext() ? path.next().toLowerCase() : "";
      Op op = putOps.get(opPath);
      if(op == null) {
         response.sendError(404, "Not Found");
         return;
      }

      switch(op) {
         case SAVEGRAPH:
            if(!path.hasNext()) {
               sendError(request, response, HttpServletResponse.SC_BAD_REQUEST, "An 'application' must be specified");
               return;
            }
            String appName = path.next();
            doSaveGraphPut(request, auth, index, appName, response);
            break;
         default:
            sendError(request, response, HttpServletResponse.SC_NOT_FOUND);
      }
   }

   @Override
   protected void doGet(final HttpServletRequest request,
                        final HttpServletResponse response) throws IOException {

      Iterator<String> path = splitPath(request).iterator();
      if(!path.hasNext()) {
         sendError(request, response, HttpServletResponse.SC_BAD_REQUEST);
         return;
      }

      final String index = path.next();
      final IndexAuthorization.Auth auth;

      if(indexAuthorization == null) { //No auth configured
         auth = IndexAuthorization.Auth.SYSTEM;
      } else {
         auth = indexAuthorization.getAuth(index, request);
         if(!auth.isAuthorized) {
            indexAuthorization.sendUnauthorized(index, response);
            return;
         }
      }

      String opPath = path.hasNext() ? path.next().toLowerCase() : "";

      Op op = ops.get(opPath);
      if(op == null) {
         op = Op.DEFAULT_WITH_APP;
      }

      switch(op) {
         case DEFAULT:
            doDefault(request, auth, index, null, response);
            break;
         case DEFAULT_WITH_APP:
            doDefault(request, auth, index, opPath, response);
            break;
         case APPS:
            doApps(request, index, response);
            break;
         case METRICS: {
            if(!path.hasNext()) {
               sendError(request, response, HttpServletResponse.SC_BAD_REQUEST, "An 'application' must be specified");
               return;
            }
            String appName = path.next();
            String metricType = path.hasNext() ? path.next() : "all";
            doMetrics(request, index, appName, metricType, response);
            break;
         }
         case GRAPHS: {
            if(!path.hasNext()) {
               sendError(request, response, HttpServletResponse.SC_BAD_REQUEST, "An 'application' must be specified");
               return;
            }
            String appName = path.next();
            doGraphs(request, auth, index, appName, response);
            break;
         }
         case USERGRAPH:
            String id = path.hasNext() ? path.next() : null;
            doUserGraph(request, auth, index, id, response);
            break;
         case USERGRAPHS:
            doUserGraphs(request, auth, index, response);
            break;
         case FIELD_STATS: {
            if(!path.hasNext()) {
               sendError(request, response, HttpServletResponse.SC_BAD_REQUEST, "An 'application' must be specified");
               return;
            }
            String appName = path.next();
            doFieldStats(request, index, appName, response);
            break;
         }
         case SAVEGRAPH: {
            if(!path.hasNext()) {
               sendError(request, response, HttpServletResponse.SC_BAD_REQUEST, "An 'application' must be specified");
               return;
            }
            String appName = path.next();
            doSaveGraphForm(request, auth, index, appName, response);
            break;
         }
         case INDEX_STATS:
            doIndexStats(request, index, response);
            break;
         case DASH:
            doDashboard(request, index, auth, response);
            break;
         default:
            response.sendError(404, "Not Found");
      }
   }

   /**
    * Renders the default page.
    * @param request The request.
    * @param auth The auth info.
    * @param index The index.
    * @param appName The application name.
    * @param response The response.
    * @throws IOException on output error.
    */
   protected void doDefault(final HttpServletRequest request,
                            final IndexAuthorization.Auth auth,
                            final String index,
                            final String appName,
                            final HttpServletResponse response) throws IOException {
      ST template = getTemplate(MAIN_TEMPLATE);
      if(template == null) {
         sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Missing '" + MAIN_TEMPLATE + "' template");
         return;
      }

      if(!auth.isSystem) {
         template.add("uid", auth.uid);
      }

      List<String> indexList = buildAllowedIndexList(request, index);
      List<Application> apps = applicationCache.getApplications(index);

      Application defaultApp = null;

      if(!Strings.isNullOrEmpty(appName)) {
         for(Application app : apps) {
            if(app.name.equalsIgnoreCase(appName)) {
               defaultApp = app;
               break;
            }
         }
      } else if(apps.size() > 0) {
         defaultApp = apps.get(0);
      }

      if(defaultApp == null) {
         response.sendError(404, "No application found");
         return;
      }


      template.add("index", index);
      template.add("content", "");
      template.add("time", new Date());
      template.add("indexList", indexList);
      template.add("zoneList", zones);
      template.add("appList", apps);
      template.add("defaultApp", defaultApp.name);

      response.setStatus(HttpServletResponse.SC_OK);
      response.setContentType(HTML_CONTENT_TYPE);
      response.getWriter().print(template.render());
      response.getWriter().flush();
   }

   /**
    * Renders the content for the application list.
    * @param request The request.
    * @param response The response.
    * @throws IOException on output error.
    */
   protected void doApps(final HttpServletRequest request,
                         final String index,
                         final HttpServletResponse response) throws IOException {
      ST template = getTemplate(APPS_TEMPLATE);
      if(template == null) {
         sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Missing '" + APPS_TEMPLATE + "' template");
         return;
      }

      try {
         List<Application> apps = applicationCache.getApplications(index);
         template.add("index", index);
         template.add("appList", apps);
         response.setStatus(HttpServletResponse.SC_OK);
         response.setContentType(HTML_CONTENT_TYPE);
         response.getWriter().print(template.render());
         response.getWriter().flush();
      } catch(Exception e) {
         sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
         e.printStackTrace();
      }
   }

   /**
    * Renders the content for application metrics.
    * @param request The request.
    * @param index The index.
    * @param appName The application name.
    * @param metricType The metric type.
    * @param response The response.
    * @throws IOException on output error.
    */
   protected void doMetrics(final HttpServletRequest request,
                            final String index,
                            final String appName,
                            final String metricType,
                            final HttpServletResponse response) throws IOException {
      ST template = getTemplate(METRICS_TEMPLATE);
      if(template == null) {
         sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Missing '" + METRICS_TEMPLATE + "' template");
         return;
      }

      try {
         Application app = applicationCache.getApplication(index, appName);
         if(app != null) {

            String filter = Strings.nullToEmpty(request.getParameter("filter")).trim();
            if(filter.equals("active")) {
               app = applicationCache.removeBoringMetrics(app);
            } else if(filter.equals("boring")) {
               app = applicationCache.onlyBoringMetrics(app);
            }

            template.add("index", index);
            template.add("app", app);
            String sortStr = request.getParameter("sort");
            Sort sort = Sort.fromString(sortStr, Sort.ASC);
            Metric.Type type = Metric.Type.fromString(metricType);
            String matchPrefix = Strings.nullToEmpty(request.getParameter("prefix")).trim();

            if(matchPrefix.isEmpty()) {
               List<Metric> metrics = type == Metric.Type.UNKNOWN ? app.getMetrics(sort) : app.getMetrics(type, sort);
               template.add("type", type == Metric.Type.UNKNOWN ? "all" : type.toString().toLowerCase());
               template.add("metrics", metrics);
            } else {
               List<Metric> metrics = app.matchPrefix(matchPrefix, type);
               if(sort == Sort.DESC) {
                  Collections.reverse(metrics);
               }
               template.add("type", type == Metric.Type.UNKNOWN ? "all" : type.toString().toLowerCase());
               template.add("metrics", metrics);
            }
         }
         response.setStatus(HttpServletResponse.SC_OK);
         response.setContentType(HTML_CONTENT_TYPE);
         response.getWriter().print(template.render());
         response.getWriter().flush();
      } catch(Exception e) {
         sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
         e.printStackTrace();
      }
   }

   protected final ImmutableMap<Metric.Type, List<MetricGraph>> defaultGraphFields =
           ImmutableMap.<Metric.Type, List<MetricGraph>>builder()
                   .put(Metric.Type.TIMER,
                           ImmutableList.of(
                                   new MetricGraph(Fields.P999_FIELD, "99.9th Percentile", "99.9 Pctl", MetricGraph.DEFAULT_TIMER_LABLES, false),
                                   new MetricGraph(Fields.P99_FIELD, "99th Percentile", "99 Pctl", MetricGraph.DEFAULT_TIMER_LABLES, true),
                                   new MetricGraph(Fields.P98_FIELD, "98th Percentile", "98 Pctl", MetricGraph.DEFAULT_TIMER_LABLES, false),
                                   new MetricGraph(Fields.P95_FIELD, "95th Percentile", "95 Pctl", MetricGraph.DEFAULT_TIMER_LABLES, true),
                                   new MetricGraph(Fields.P75_FIELD, "75th Percentile", "75 Pctl", MetricGraph.DEFAULT_TIMER_LABLES, false),
                                   new MetricGraph(Fields.P50_FIELD, "Median", "Median", MetricGraph.DEFAULT_TIMER_LABLES, false),
                                   new MetricGraph(Fields.MAX_FIELD, "Max", "Max", MetricGraph.DEFAULT_TIMER_LABLES, false),
                                   new MetricGraph(Fields.MIN_FIELD, "Min", "Min", MetricGraph.DEFAULT_TIMER_LABLES, false),
                                   new MetricGraph(Fields.MEAN_FIELD, "Mean", "Mean", MetricGraph.DEFAULT_TIMER_LABLES, false),
                                   new MetricGraph(Fields.STD_FIELD, "Standard Deviation", "Std", MetricGraph.DEFAULT_TIMER_LABLES, false),

                                   new MetricGraph(Fields.ONE_MINUTE_RATE_FIELD, "One Minute Rate", "1m Rate", MetricGraph.DEFAULT_METER_LABLES, true),
                                   new MetricGraph(Fields.FIVE_MINUTE_RATE_FIELD, "Five Minute Rate", "5m Rate", MetricGraph.DEFAULT_METER_LABLES, false),
                                   new MetricGraph(Fields.FIFTEEN_MINUTE_RATE_FIELD, "Fifteen Minute Rate", "15m Rate", MetricGraph.DEFAULT_METER_LABLES, false),
                                   new MetricGraph(Fields.MEAN_RATE_FIELD, "Mean Rate", "Mean Rate", MetricGraph.DEFAULT_METER_LABLES, false),
                                   new MetricGraph(Fields.COUNT_FIELD, "Count", "Count", MetricGraph.DEFAULT_OTHER_LABLES, false)

                           )
                   )
                   .put(Metric.Type.METER,
                           ImmutableList.of(
                                   new MetricGraph(Fields.ONE_MINUTE_RATE_FIELD, "One Minute Rate", "1m Rate", MetricGraph.DEFAULT_METER_LABLES, true),
                                   new MetricGraph(Fields.FIVE_MINUTE_RATE_FIELD, "Five Minute Rate", "5m Rate", MetricGraph.DEFAULT_METER_LABLES, true),
                                   new MetricGraph(Fields.FIFTEEN_MINUTE_RATE_FIELD, "Fifteen Minute Rate", "15m Rate", MetricGraph.DEFAULT_METER_LABLES, true),
                                   new MetricGraph(Fields.MEAN_RATE_FIELD, "Mean Rate", "Mean Rate", MetricGraph.DEFAULT_METER_LABLES, false),
                                   new MetricGraph(Fields.COUNT_FIELD, "Count", "Count", MetricGraph.DEFAULT_OTHER_LABLES, false)
                           )
                   )
                   .put(Metric.Type.HISTOGRAM,
                           ImmutableList.of(
                                   new MetricGraph(Fields.P999_FIELD, "99.9th Percentile", "99.9 Pctl", MetricGraph.DEFAULT_OTHER_LABLES, false),
                                   new MetricGraph(Fields.P99_FIELD, "99th Percentile", "99 Pctl", MetricGraph.DEFAULT_OTHER_LABLES, true),
                                   new MetricGraph(Fields.P98_FIELD, "98th Percentile", "98 Pctl", MetricGraph.DEFAULT_OTHER_LABLES, false),
                                   new MetricGraph(Fields.P95_FIELD, "95th Percentile", "95 Pctl", MetricGraph.DEFAULT_OTHER_LABLES, true),
                                   new MetricGraph(Fields.P75_FIELD, "75th Percentile", "75 Pctl", MetricGraph.DEFAULT_OTHER_LABLES, false),
                                   new MetricGraph(Fields.P50_FIELD, "Median", "Median", MetricGraph.DEFAULT_OTHER_LABLES, false),
                                   new MetricGraph(Fields.MAX_FIELD, "Max", "Max", MetricGraph.DEFAULT_OTHER_LABLES, false),
                                   new MetricGraph(Fields.MIN_FIELD, "Min", "Min", MetricGraph.DEFAULT_OTHER_LABLES, false),
                                   new MetricGraph(Fields.MEAN_FIELD, "Mean", "Mean", MetricGraph.DEFAULT_OTHER_LABLES, true),
                                   new MetricGraph(Fields.STD_FIELD, "Standard Deviation", "Std", MetricGraph.DEFAULT_OTHER_LABLES, false),
                                   new MetricGraph(Fields.COUNT_FIELD, "Count", "Count", MetricGraph.DEFAULT_OTHER_LABLES, false)
                           )
                   )
                   .put(Metric.Type.GAUGE,
                           ImmutableList.of(
                                   new MetricGraph("value", "Value", "Value", MetricGraph.DEFAULT_OTHER_LABLES, true)
                           )
                   )
                   .put(Metric.Type.COUNTER,
                           ImmutableList.of(
                                   new MetricGraph("count", "Count", "Count", MetricGraph.DEFAULT_OTHER_LABLES, true)
                           )
                   )
                   .build();

   /**
    * Renders a page of graphs a metric.
    * @param request The request.
    * @param auth The auth info.
    * @param index The index.
    * @param appName The application name.
    * @param response The response.
    * @throws IOException on output error.
    */
   protected void doGraphs(final HttpServletRequest request,
                           final IndexAuthorization.Auth auth,
                           final String index,
                           final String appName,
                           final HttpServletResponse response) throws IOException {


      ST template = getTemplate(GRAPHS_TEMPLATE);
      if(template == null) {
         sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Missing '" + GRAPHS_TEMPLATE + "' template");
         return;
      }

      if(!auth.isSystem) {
         template.add("uid", auth.uid);
      }

      try {
         Application app = applicationCache.getApplication(index, appName);
         List<MetricGraph> graphs = Lists.newArrayListWithExpectedSize(8);
         String metricName = request.getParameter("name");
         if(app != null) {
            Metric metric = app.getMetric(metricName);
            if(metric != null) {
               template.add("metric", metric);
               List<MetricGraph> protos = defaultGraphFields.get(metric.type);
               for(MetricGraph proto : protos) {
                  graphs.add(new MetricGraph(proto, metric));
               }
            }
         }

         long rangeStart = Util.getLongParameter(request, QueryBase.START_TIMESTAMP_PARAMETER, 0L);
         long rangeEnd = Util.getLongParameter(request, QueryBase.END_TIMESTAMP_PARAMETER, 0L);
         String rangeName = Strings.nullToEmpty(request.getParameter(QueryBase.RANGE_PARAMETER)).trim();
         if(rangeName.isEmpty()) {
            rangeName = Util.nearestInterval(rangeEnd - rangeStart);
         }

         if(rangeName == null) {
            rangeName = DEFAULT_GRAPH_RANGE;
            rangeStart = 0;
            rangeEnd = 0;
         }

         GraphRange range = new GraphRange(rangeName, rangeStart, rangeEnd);

         String host = Strings.nullToEmpty(request.getParameter("host")).trim();
         if(host.isEmpty()) {
            template.add("host", host);
         }

         template.add("indexList", buildAllowedIndexList(request, index));
         template.add("zoneList", zones);
         template.add("downsampleList", buildDownsampleFunctionList(request));
         template.add("range", range);
         template.add("app", app);
         template.add("index", index);
         template.add("graphs", graphs);

         response.setStatus(HttpServletResponse.SC_OK);
         response.setContentType(HTML_CONTENT_TYPE);
         response.getWriter().print(template.render());
         response.getWriter().flush();
      } catch(Exception e) {
         sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
         e.printStackTrace();
      }
   }

   /**
    * Renders a saved user graph.
    * @param request The request.
    * @param auth The auth info.
    * @param index The index.
    * @param response The response.
    * @throws IOException on output error.
    */
   protected void doUserGraph(final HttpServletRequest request,
                              final IndexAuthorization.Auth auth,
                              final String index,
                              String id,
                              final HttpServletResponse response) throws IOException {


      ST template = getTemplate(USER_GRAPH_TEMPLATE);
      if(template == null) {
         sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Missing '" + GRAPHS_TEMPLATE + "' template");
         return;
      }

      if(Strings.nullToEmpty(id).isEmpty()) {
         id = Strings.nullToEmpty(request.getParameter("id")).trim();
      }

      if(id.isEmpty()) {
         sendError(request, response, HttpServletResponse.SC_BAD_REQUEST, "An 'id' is required");
         return;
      }

      try {

         StoredGraph graph = userStore.getGraph(index, id);
         if(graph == null) {
            sendError(request, response, HttpServletResponse.SC_NOT_FOUND, "The graph with id = '" + id + "' does not exist");
            return;
         }

         //if(!auth.uid.equals(graph.uid)) We'll let this slide. Any authorized user should be able to view a saved graph.

         template.add("graph", graph);
         if(!auth.isSystem) {
            template.add("uid", auth.uid);
         }
         template.add("downsampleList", buildDownsampleFunctionList(request));
         template.add("indexList", buildAllowedIndexList(request, index));
         template.add("zoneList", zones);
         template.add("index", index);

         response.setStatus(HttpServletResponse.SC_OK);
         response.setContentType(HTML_CONTENT_TYPE);
         response.getWriter().print(template.render());
         response.getWriter().flush();
      } catch(Exception e) {
         sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
         e.printStackTrace();
      }
   }

   /**
    * Deletes a saved user graph.
    * @param request The request.
    * @param auth The auth info.
    * @param index The index.
    * @param response The response.
    * @throws IOException on output error.
    */
   protected void doUserGraphDelete(final HttpServletRequest request,
                                    final IndexAuthorization.Auth auth,
                                    final String index,
                                    String id,
                                    final HttpServletResponse response) throws IOException {

      if(Strings.nullToEmpty(id).isEmpty()) {
         id = Strings.nullToEmpty(request.getParameter("id")).trim();
      }

      if(id.isEmpty()) {
         sendError(request, response, HttpServletResponse.SC_BAD_REQUEST, "An 'id' is required");
         return;
      }

      try {
         StoredGraph graph = userStore.getGraph(index, id);
         if(graph != null) {
            userStore.deleteGraph(graph);
         }
         response.setStatus(HttpServletResponse.SC_OK);
         response.setContentType(HTML_CONTENT_TYPE);
         response.getWriter().print("true");
         response.getWriter().flush();
      } catch(Exception e) {
         sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
         e.printStackTrace();
      }
   }

   /**
    * Renders a list of user graphs.
    * @param request The request.
    * @param auth The authorization.
    * @param index The index.
    * @param response The response.
    * @throws IOException on output error.
    */
   protected void doUserGraphs(final HttpServletRequest request,
                               final IndexAuthorization.Auth auth,
                               final String index,
                               final HttpServletResponse response) throws IOException {

      String templateName = GRAPH_LIST_TEMPLATE;
      ST template = getTemplate(templateName);
      if(template == null) {
         sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Missing '" + GRAPH_LIST_TEMPLATE + "' template");
         return;
      }

      try {
         List<StoredGraph> graphs = userStore.getUserGraphs(index, auth.uid, 0, 100); //TODO...paging
         template.add("index", index);
         template.add("graphs", graphs);
      } catch(Exception e) {
         sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
         e.printStackTrace();
         return;
      }

      response.setStatus(HttpServletResponse.SC_OK);
      response.setContentType(HTML_CONTENT_TYPE);
      response.getWriter().print(template.render());
      response.getWriter().flush();
   }


   /**
    * Renders stats for an index.
    * @param request The request.
    * @param index The index.
    * @param response The response.
    * @throws IOException on output error.
    */
   protected void doIndexStats(final HttpServletRequest request,
                               final String index,
                               final HttpServletResponse response) throws IOException {

      ST template = getTemplate(INDEX_STATS_TEMPLATE);
      if(template == null) {
         sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Missing '" + INDEX_STATS_TEMPLATE + "' template");
         return;
      }

      try {

         template.add("index", index);

         final Request esIndexRequest = esEndpoint.getRequestBuilder(esEndpoint.buildIndexStatsURI(index)).create();
         final Response esIndexResponse = client.send(esIndexRequest);

         final Request esClusterRequest = esEndpoint.getRequestBuilder(esEndpoint.buildClusterStatsURI()).create();
         final Response esClusterResponse = client.send(esClusterRequest);

         if(esIndexResponse.getStatusCode() == 200 && esClusterResponse.getStatusCode() == 200) {
            ObjectNode indexObject = Util.mapper.readTree(Util.parserFactory.createParser(esIndexResponse.getBody().toByteArray()));
            ObjectNode clusterObject = Util.mapper.readTree(Util.parserFactory.createParser(esClusterResponse.getBody().toByteArray()));
            JsonNode totalNode = indexObject.path("_all").path("total");
            if(!totalNode.isMissingNode()) {
               IndexStats stats = new IndexStats(totalNode, clusterObject);
               template.add("stats", stats);
            } else {
               logger.error("Invalid stats response for '" + index + "'");
            }
         } else if(esIndexResponse.getStatusCode() != 200) {
            logger.error("Index stats response error for '" + index + "' (" + esIndexResponse.getStatusCode() + ")");
         } else {
            logger.error("Cluster stats response error for '" + index + "' (" + esClusterResponse.getStatusCode() + ")");
         }

      } catch(Exception e) {
         sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
         e.printStackTrace();
         return;
      }

      response.setStatus(HttpServletResponse.SC_OK);
      response.setContentType(HTML_CONTENT_TYPE);
      response.getWriter().print(template.render());
      response.getWriter().flush();
   }

   protected void doSaveGraphPut(final HttpServletRequest request,
                                 final IndexAuthorization.Auth auth,
                                 final String index, final String app,
                                 final HttpServletResponse response) throws IOException {

      StoredGraph.Builder graphBuilder = StoredGraph.parseGraph(request, app);
      if(graphBuilder.hasAbsoluteRange()) {
         graphBuilder.setRangeName(Util.nearestInterval(graphBuilder.getRangeMillis()));
      }
      graphBuilder.setUserId(auth.uid);
      graphBuilder.setIndex(index);

      try {
         StoredGraph graph = graphBuilder.build();
         boolean stored = userStore.storeGraph(graph);
         if(stored) {
            response.setStatus(201);
            response.getOutputStream().print(graph.id);
         } else {
            sendError(request, response, 500, "Problem storing key");
         }
      } catch(IOException ioe) {
         logger.error("Problem storing user key", ioe);
         sendError(request, response, 500, "Problem storing key");
      }
   }

   /**
    * Renders stats for a field.
    * @param request The request.
    * @param index The index.
    * @param response The response.
    * @throws IOException on output error.
    */
   protected void doFieldStats(final HttpServletRequest request,
                               final String index,
                               final String app,
                               final HttpServletResponse response) throws IOException {

      String templateName = request.getParameter("t");
      if(Strings.nullToEmpty(templateName).trim().isEmpty()) {
         templateName = FIELD_STATS_TEMPLATE;
      }

      ST template = getTemplate(templateName);
      if(template == null) {
         sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Missing '" + INDEX_STATS_TEMPLATE + "' template");
         return;
      }

      if(Strings.nullToEmpty(request.getParameter("name")).trim().isEmpty()) {
         sendError(request, response, HttpServletResponse.SC_BAD_REQUEST, "A 'name' parameter is required");
         return;
      }

      if(Strings.nullToEmpty(request.getParameter("field")).trim().isEmpty()) {
         sendError(request, response, HttpServletResponse.SC_BAD_REQUEST, "A 'field' parameter is required");
         return;
      }

      String range = Strings.nullToEmpty(request.getParameter("range")).trim();
      if(range.isEmpty()) {
         sendError(request, response, HttpServletResponse.SC_BAD_REQUEST, "A 'range' parameter is required");
         return;
      }

      try {

         long startTimestamp = Util.getLongParameter(request, "startTimestamp", 0L);
         long endTimestamp = Util.getLongParameter(request, "endTimestamp", 0L);

         StatsQuery query = new StatsQuery(MetricKey.parseKey(request, app), range, startTimestamp, endTimestamp);
         template.add("index", index);
         template.add("app", app);
         template.add("key", query.key);
         template.add("range", query.range);

         String esQuery = query.searchRequest.toJSON();
         Request esRequest = esEndpoint.postRequestBuilder(esEndpoint.buildIndexURI(index),
                 esQuery.getBytes(Charsets.UTF_8)).create();
         Response esResponse = client.send(esRequest, requestOptions);

         if(esResponse.getStatusCode() == 200) {
            ObjectNode statsObject = Util.mapper.readTree(Util.parserFactory.createParser(esResponse.getBody().toByteArray()));
            Stats stats = StatsParser.parseStats(statsObject);
            template.add("stats", stats);
         } else {
            logger.error("Field stats response error for '" + index + "' (" + esResponse.getStatusCode() + ")");
         }
      } catch(Exception e) {
         sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
         e.printStackTrace();
         return;
      }

      response.setStatus(HttpServletResponse.SC_OK);
      response.setContentType(HTML_CONTENT_TYPE);
      response.getWriter().print(template.render());
      response.getWriter().flush();
   }

   /**
    * Renders stats for a field.
    * @param request The request.
    * @param index The index.
    * @param response The response.
    * @throws IOException on output error.
    */
   protected void doSaveGraphForm(final HttpServletRequest request,
                                  final IndexAuthorization.Auth auth,
                                  final String index,
                                  final String app,
                                  final HttpServletResponse response) throws IOException {

      ST template = getTemplate(SAVE_KEY_TEMPLATE);
      if(template == null) {
         sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Missing '" + SAVE_KEY_TEMPLATE + "' template");
         return;
      }

      if(Strings.nullToEmpty(request.getParameter("name")).trim().isEmpty()) {
         sendError(request, response, HttpServletResponse.SC_BAD_REQUEST, "A 'name' parameter is required");
         return;
      }

      if(Strings.nullToEmpty(request.getParameter("field")).trim().isEmpty()) {
         sendError(request, response, HttpServletResponse.SC_BAD_REQUEST, "A 'field' parameter is required");
         return;
      }

      try {
         StoredGraph.Builder graphBuilder = StoredGraph.parseGraph(request, app);
         graphBuilder.setUserId(auth.uid);
         graphBuilder.setIndex(index);
         String gid = graphBuilder.build().id;
         StoredGraph currGraph = userStore.getGraph(index, gid);
         if(currGraph != null) {
            graphBuilder.setTitle(currGraph.title);
            graphBuilder.setDescription(currGraph.description);
            graphBuilder.setXLabel(currGraph.xLabel);
            graphBuilder.setYLabel(currGraph.yLabel);
            graphBuilder.setCreateTime(currGraph.createTime);
            for(String tag : currGraph.tags) {
               graphBuilder.addTag(tag);
            }
         }
         template.add("graph", graphBuilder.build());
      } catch(Exception e) {
         sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
         e.printStackTrace();
         return;
      }
      response.setStatus(HttpServletResponse.SC_OK);
      response.setContentType(HTML_CONTENT_TYPE);
      response.getWriter().print(template.render());
      response.getWriter().flush();
   }

   private void addIfSet(final ST template, final String name, final String val) {
      if(val != null && !val.isEmpty()) template.add(name, val);
   }

   /**
    * Renders a dashboard.
    * @param request The request.
    * @param auth The auth info.
    * @param response The response.
    * @throws IOException on output error.
    */
   protected void doDashboard(final HttpServletRequest request,
                              final String index,
                              final IndexAuthorization.Auth auth,
                              final HttpServletResponse response) throws IOException {


      ST template = getTemplate(DASHBOARD_TEMPLATE);
      if(template == null) {
         sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Missing '" + DASHBOARD_TEMPLATE + "' template");
         return;
      }

      Dashboard dash = new Dashboard(request, zoneMap);

      if(dash.id.isEmpty() && dash.tags.isEmpty()) {
         sendError(request, response, HttpServletResponse.SC_BAD_REQUEST, "At least one 'tag' is required");
         return;
      }

      try {

         final List<StoredGraph> graphs;
         if(dash.id.isEmpty()) {
            int start = Util.getParameter(request, "start", 0);
            int limit = Util.getParameter(request, "limit", 50);
            graphs = userStore.getUserGraphs(index, auth.uid, dash.tags, start, limit);
         } else {
            StoredGraph graph = userStore.getGraph(index, dash.id);
            graphs = graph != null ? Lists.newArrayList(graph) : Collections.<StoredGraph>emptyList();
         }

         template.add("graphs", graphs);

         String customTemplateName = Util.getParameter(request, "template", "");
         if(!customTemplateName.isEmpty()) {
            ST customTemplate = getDashboardTemplate(customTemplateName);
            if(customTemplate == null) {
               sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Missing '" + customTemplateName + "' template");
               return;
            }
            Map<String, String> idMap = Maps.newHashMap();
            for(StoredGraph graph : graphs) {
               idMap.put(graph.sid, graph.sid);
               idMap.put(graph.key.toString().replace('-', '_'), graph.sid);
            }
            customTemplate.add("graphs", idMap);
            template.add("custom", customTemplate.render());
         }

         if(!auth.isSystem) {
            template.add("uid", auth.uid);
         }

         List<Dashboard> zoneDashboards = Lists.newArrayListWithCapacity(zones.size());
         for(DisplayTZ zone : zones) {
            zoneDashboards.add(new Dashboard.Builder(dash).setTz(zone).build());
         }

         List<String> userTags = userStore.getUserTags(index, auth.uid);
         List<Dashboard> tagDashboards = Lists.newArrayListWithExpectedSize(userTags.size());
         for(String tag : userTags) {
            tagDashboards.add(new Dashboard.Builder(dash).setTags(Collections.singletonList(tag)).build());
         }

         if(dash.isAutoUpdate()) {
            template.add("toggleAuto", new Dashboard.Builder(dash).setAutoUpdateSeconds(0).build().queryString);
         } else {
            template.add("toggleAuto", new Dashboard.Builder(dash).setAutoUpdateSeconds(60).build().queryString);
         }

         template.add("dash", dash);
         template.add("zoneList", zones);
         template.add("zoneDashboards", zoneDashboards);
         template.add("tagDashboards", tagDashboards);

         template.add("index", index);

         response.setStatus(HttpServletResponse.SC_OK);
         response.setContentType(HTML_CONTENT_TYPE);
         response.getWriter().print(template.render());
         response.getWriter().flush();
      } catch(Exception e) {
         sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
         e.printStackTrace();
      }
   }


   protected void sendError(final HttpServletRequest request,
                            final HttpServletResponse response,
                            final int code) throws IOException {
      sendError(request, response, code, null);
   }


   protected void sendError(final HttpServletRequest request,
                            final HttpServletResponse response,
                            final int code, final String message) throws IOException {
      if(message != null) {
         response.sendError(code, message);
      } else {
         response.sendError(code);
      }
   }

   /**
    * Builds a list of indexes the user is allowed to access.
    * @param request The request (for index auth).
    * @param index The current index.
    * @return The list of indexes (with current first).
    */
   protected List<String> buildAllowedIndexList(final HttpServletRequest request, final String index) {

      List<String> indexList = Lists.newArrayListWithCapacity(this.allowedIndexes.size());
      indexList.add(index);
      for(String allowedIndex : this.allowedIndexes) {
         if(!allowedIndex.equals(index)) {
            List<Application> apps = applicationCache.getApplications(allowedIndex);
            if(apps != null && apps.size() > 0) {
               if(indexAuthorization == null) {
                  indexList.add(allowedIndex);
               } else if(indexAuthorization.isAuthorized(allowedIndex, request)) {
                  indexList.add(allowedIndex);
               }
            }
         }
      }
      return indexList;
   }

   /**
    * Builds the list of downsample functions, activating the one in-use.
    * @param request The servlet request.
    * @return The list of functions.
    */
   protected List<DownsampleFunction> buildDownsampleFunctionList(final HttpServletRequest request) {
      String fnName = Strings.nullToEmpty(request.getParameter(GraphQuery.DOWNSAMPLE_FN_PARAMETER)).trim();
      if(fnName.isEmpty() || fnName.equals("avg")) {
         return downsampleFunctions;
      } else {
         List<DownsampleFunction> functionList = Lists.newArrayListWithCapacity(downsampleFunctions.size());
         for(DownsampleFunction fn : downsampleFunctions) {
            if(fn.functionName.equals(fnName)) {
               functionList.add(fn.activate());
            } else {
               functionList.add(fn.deactivate());
            }
         }
         return functionList;
      }
   }

   /**
    * Load (or reload) templates.
    */
   private STGroup loadTemplates() {

      STGroup group = new STGroupDir(templateDirectory, '$', '$');

      File globalConstantsFile = new File(templateDirectory, "constants.stg");
      if(globalConstantsFile.exists()) {
         STGroupFile globalConstants = new STGroupFile(globalConstantsFile.getAbsolutePath());
         group.importTemplates(globalConstants);
      }

      group.setListener(new ErrorListener(logger));
      group.registerRenderer(java.util.Date.class, new DateRenderer());
      return group;
   }

   /**
    * Load (or reload) dashboard templates.
    */
   private STGroup loadDashboardTemplates() {

      STGroup group = new STGroupDir(dashboardTemplateDirectory, '$', '$');

      File globalConstantsFile = new File(dashboardTemplateDirectory, "constants.stg");
      if(globalConstantsFile.exists()) {
         STGroupFile globalConstants = new STGroupFile(globalConstantsFile.getAbsolutePath());
         group.importTemplates(globalConstants);
      }

      group.setListener(new ErrorListener(logger));
      group.registerRenderer(java.util.Date.class, new DateRenderer());
      return group;
   }

   /**
    * Gets a template instance.
    * <p>
    * If debug mode is configured, templates are reloaded from disk
    * on every request. Otherwise, template changes are recognized
    * only on restart.
    * </p>
    * @param name The template name.
    * @return The instance or <code>null</code> if template not found.
    */
   protected ST getTemplate(final String name) {

      if(this.templateGroup == null) {
         STGroup debugTemplateGroup = loadTemplates();
         try {
            return debugTemplateGroup.getInstanceOf(name);
         } catch(Exception e) {
            e.printStackTrace();
            return null;
         }
      } else {
         try {
            return templateGroup.getInstanceOf(name);
         } catch(Exception e) {
            e.printStackTrace();
            return null;
         }
      }
   }

   /**
    * Gets a dashboard template instance.
    * @param name The template name.
    * @return The instance or <code>null</code> if template not found.
    */
   protected ST getDashboardTemplate(final String name) {

      if(this.dashboardTemplateGroup == null) {
         STGroup debugTemplateGroup = loadDashboardTemplates();
         try {
            return debugTemplateGroup.getInstanceOf(name);
         } catch(Exception e) {
            e.printStackTrace();
            return null;
         }
      } else {
         try {
            return dashboardTemplateGroup.getInstanceOf(name);
         } catch(Exception e) {
            e.printStackTrace();
            return null;
         }
      }
   }

   /**
    * The default graph range if none specified.
    */
   public static final String DEFAULT_GRAPH_RANGE = "day";

   /**
    * The value sent with HTML for 'Content-Type'.
    */
   public static final String HTML_CONTENT_TYPE = "text/html; charset=utf8";

   /**
    * The main (index) template.
    */
   public static final String MAIN_TEMPLATE = "main";

   /**
    * The main graphs template.
    */
   public static final String GRAPHS_TEMPLATE = "graphs_main";

   /**
    * The user (saved) graph template.
    */
   public static final String USER_GRAPH_TEMPLATE = "user_graph_main";

   /**
    * The template that renders the list of apps.
    */
   public static final String APPS_TEMPLATE = "apps";

   /**
    * The template that renders the list of metrics for an app.
    */
   public static final String METRICS_TEMPLATE = "metrics";

   /**
    * The template for rendering index stats.
    */
   public static final String INDEX_STATS_TEMPLATE = "index_stats";

   /**
    * The template for rendering field stats.
    */
   public static final String FIELD_STATS_TEMPLATE = "field_stats";

   /**
    * The template for rendering the save key form.
    */
   public static final String SAVE_KEY_TEMPLATE = "save_key_form";

   /**
    * The template for rendering a list of user graphs.
    */
   public static final String GRAPH_LIST_TEMPLATE = "dash_list";

   /**
    * The template for rendering a dashboard.
    */
   public static final String DASHBOARD_TEMPLATE = "dashboard";

   /**
    * Available downsample functions.
    */
   public static final ImmutableList<DownsampleFunction> downsampleFunctions =
           ImmutableList.of(
                   new DownsampleFunction("Avg", "avg", true),
                   new DownsampleFunction("Max", "max", false),
                   new DownsampleFunction("Min", "min", false),
                   new DownsampleFunction("Sum", "sum", false)
           );

   /*
    * These template groups may be null if debug mode is configured.
    */
   private final STGroup templateGroup;
   private final STGroup dashboardTemplateGroup;

   private final String templateDirectory;
   private final String dashboardTemplateDirectory;
   private final Logger logger;
   private final AsyncClient client;
   private final RequestOptions requestOptions;
   private final boolean debug;
   private final IndexAuthorization indexAuthorization;
   private final ESEndpoint esEndpoint;
   private final ESUserStore userStore;
   private final ImmutableList<String> allowedIndexes;
   private final ImmutableList<DisplayTZ> zones;
   private final ImmutableMap<String, DisplayTZ> zoneMap;

   final ApplicationCache applicationCache;
}
