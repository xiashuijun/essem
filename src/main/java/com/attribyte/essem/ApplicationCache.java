/*
 * Copyright 2014, 2015 Attribyte, LLC
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
import com.attribyte.essem.model.Metric;
import com.attribyte.essem.model.graph.MetricKey;
import com.attribyte.essem.model.graph.Stats;
import com.attribyte.essem.query.ApplicationMetricsQuery;
import com.attribyte.essem.query.ApplicationsQuery;
import com.attribyte.essem.query.Fields;
import com.attribyte.essem.query.StatsQuery;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.attribyte.api.Logger;
import org.attribyte.api.http.AsyncClient;
import org.attribyte.api.http.Request;
import org.attribyte.api.http.RequestOptions;
import org.attribyte.api.http.Response;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.attribyte.essem.util.Util.parserFactory;
import static com.attribyte.essem.util.Util.mapper;

@SuppressWarnings("NullableProblems")
/**
 * A cache for application and metric data.
 */
class ApplicationCache implements MetricSet {

   /**
    * Holds a cached application along with current stats for the application metrics.
    */
   private static class CachedApplication {

      /**
       * Creates the application and stats.
       * @param app The application.
       * @param stats The stats.
       */
      private CachedApplication(final Application app, final Map<String, Stats> stats) {
         this.app = app;
         this.stats = stats != null ? ImmutableMap.copyOf(stats) : ImmutableMap.<String, Stats>of();
      }

      /**
       * The application.
       */
      final Application app;

      /**
       * The stats for application metrics.
       */
      final ImmutableMap<String, Stats> stats;
   }

   /**
    * Holds a cached application and stats for the application metrics.
    */
   private static class CachedApplications {

      /**
       * Creates cached applications.
       * @param apps The cached applications and metric stats.
       * @param expireTimeMillis The expire time.
       */
      CachedApplications(final String index, final List<CachedApplication> apps, final long expireTimeMillis) {
         this.index = index;
         this.apps = apps != null ? ImmutableList.copyOf(apps) : ImmutableList.<CachedApplication>of();
         this.expireTimeMillis = expireTimeMillis;
         this.pendingUpdate = false;
      }

      /**
       * Creates cached applications.
       * @param apps The cached applications and metric stats.
       * @param expireTimeMillis The expire time.
       */
      private CachedApplications(final String index, final List<CachedApplication> apps, final long expireTimeMillis,
                                 final boolean pendingUpdate) {
         this.index = index;
         this.apps = apps != null ? ImmutableList.copyOf(apps) : ImmutableList.<CachedApplication>of();
         this.expireTimeMillis = expireTimeMillis;
         this.pendingUpdate = pendingUpdate;
      }

      /**
       * Convert this to one with pending update flag set.
       * @return This cached applications with the pending update flag set.
       */
      final CachedApplications toPendingUpdate() {
         return new CachedApplications(this.index, this.apps, this.expireTimeMillis, true);
      }

      /**
       * The index name.
       */
      final String index;

      /**
       * The cached application.
       */
      final ImmutableList<CachedApplication> apps;

      /**
       * Is an update pending?
       */
      final boolean pendingUpdate;

      /**
       * Gets the list of applications.
       * @return The applications.
       */
      final List<Application> getApps() {
         List<Application> apps = Lists.newArrayListWithCapacity(this.apps.size());
         apps.addAll(this.apps.stream().map(app -> app.app).collect(Collectors.toList()));
         return apps;
      }

      /**
       * Gets stats for an application.
       * @param app The application.
       * @return The stats or <code>null</code>.
       */
      final ImmutableMap<String, Stats> getStats(final Application app) {
         for(CachedApplication cachedApplication : apps) {
            if(cachedApplication.app.equals(app)) {
               return cachedApplication.stats;
            }
         }
         return null;
      }

      /**
       * Creates new cached applications with expiration extended.
       * @param expireTimeMillis The new expiration time.
       * @return The cached applications with expiration extended.
       */
      final CachedApplications extendExpiration(final long expireTimeMillis) {
         return new CachedApplications(this.index, this.apps, expireTimeMillis);
      }

      /**
       * The expire timestamp.
       */
      final long expireTimeMillis;

      /**
       * Check to see if the object has expired.
       * @param currTimeMillis The timestamp to use as the current time.
       * @return Is the object expired?
       */
      final boolean isExpired(final long currTimeMillis) {
         return expireTimeMillis < currTimeMillis;
      }
   }

   /**
    * The default range (7 days).
    */
   public static final String DEFAULT_ACTIVITY_RANGE = "week";

   ApplicationCache(final AsyncClient client, final RequestOptions requestOptions,
                    final ESEndpoint esEndpoint,
                    final Logger logger) {

      this.client = client;
      this.requestOptions = requestOptions;
      this.esEndpoint = esEndpoint;
      this.logger = logger;


      final BlockingQueue<Runnable> requestQueue = new ArrayBlockingQueue<>(4096);
      final Gauge<Integer> requestQueueSize = requestQueue::size;

      final ThreadPoolExecutor requestExecutor = new ThreadPoolExecutor(2, 8, 5L, TimeUnit.MINUTES,
              requestQueue, new ThreadFactoryBuilder().setNameFormat("application-cache-%d").build());
      requestExecutor.prestartAllCoreThreads();

      final Counter rejectedRequests = new Counter();
      requestExecutor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
         @Override
         public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
            rejectedRequests.inc();
         }
      });
      this.requestExecutor = MoreExecutors.listeningDecorator(MoreExecutors.getExitingExecutorService(requestExecutor));


      this.appRequestTimer = new Timer();
      this.appRequestErrors = new Counter();

      this.nameRequestTimer = new Timer();
      this.nameRequestErrors = new Counter();

      this.statsRequestTimer = new Timer();
      this.statsRequestErrors = new Counter();

      Gauge<Integer> appCacheSize = new Gauge<Integer>() {
         @Override
         public Integer getValue() {
            return appCache.size();
         }
      };

      this.metrics = ImmutableMap.<String, com.codahale.metrics.Metric>builder()
              .put("request-queue-size", requestQueueSize)
              .put("rejected-background-requests", rejectedRequests)
              .put("app-requests", appRequestTimer)
              .put("app-request-errors", appRequestErrors)
              .put("name-requests", nameRequestTimer)
              .put("name-request-errors", nameRequestErrors)
              .put("app-cache-size", appCacheSize)
              .put("stats-requests", statsRequestTimer)
              .put("stats-request-errors", statsRequestErrors).build();
   }

   /**
    * Gets all applications for an index.
    * @param index The index.
    * @return The list of applications with metrics resolved.
    */
   List<Application> getApplications(final String index) {
      CachedApplications apps = getCachedApplications(index);
      if(apps != null) {
         return apps.getApps();
      } else {
         return ImmutableList.of();
      }
   }

   /**
    * Gets a particular application for an index.
    * @param index The index.
    * @param name The application name.
    * @return The application or <code>null</code> if not found.
    */
   Application getApplication(final String index, final String name) {
      List<Application> applications = getApplications(index);
      for(Application application : applications) {
         if(application.name.equalsIgnoreCase(name)) {
            return application;
         }
      }
      return null;
   }

   /**
    * Resolution used for "boring" calculation.
    */
   private static final double BORING_RESOLUTION = 0.0000001;

   /**
    * Removes "boring" metrics from an application.
    * @param app The application with "boring" metrics removed.
    * @return The modified application.
    * @throws IOException on index communication error.
    */
   Application removeBoringMetrics(final Application app) throws IOException {
      Map<String, Stats> stats = getStats(app);
      List<Metric> activeMetrics = Lists.newArrayListWithCapacity(app.metrics.size());
      for(Metric metric : app.metrics) {
         Stats metricStats = stats.get(metric.name);
         if(metricStats != null) {
            if(Math.abs(metricStats.max - metricStats.min) > BORING_RESOLUTION) {
               activeMetrics.add(metric);
            }
         } else {
            activeMetrics.add(metric);
         }
      }
      return app.withMetrics(activeMetrics);
   }

   /**
    * Keeps only "boring" metrics from an application.
    * @param app The application with only "boring" metrics.
    * @return The modified application.
    * @throws IOException on index communication error.
    */
   Application onlyBoringMetrics(final Application app) throws IOException {
      Map<String, Stats> stats = getStats(app);
      List<Metric> boringMetrics = Lists.newArrayListWithCapacity(app.metrics.size());
      for(Metric metric : app.metrics) {
         Stats metricStats = stats.get(metric.name);
         if(metricStats != null) {
            if(Math.abs(metricStats.max - metricStats.min) < BORING_RESOLUTION) {
               boringMetrics.add(metric);
            }
         }
      }
      return app.withMetrics(boringMetrics);
   }

   /**
    * The default stats lifetime (5 minutes).
    */
   private static final long CACHE_LIFETIME_MILLIS = 5L * 60L * 1000L;

   /**
    * Loads applications for an index.
    * @param index The index.
    * @return The applications with metrics loaded or <code>null</code> on application or metric load error.
    */
   private List<Application> loadApplications(final String index) {
      Timer.Context ctx = appRequestTimer.time();
      try {
         final Request esRequest = esEndpoint.postRequestBuilder(esEndpoint.buildIndexURI(index),
                 ApplicationsQuery.DEFAULT_QUERY.searchRequest.toJSON().getBytes(Charsets.UTF_8)).create();
         final Response esResponse = client.send(esRequest, requestOptions);

         switch(esResponse.getStatusCode()) {
            case 200:
               ObjectNode jsonObject = mapper.readTree(parserFactory.createParser(esResponse.getBody().toByteArray()));
               List<Application> parsedApps = ApplicationParser.parseApplications(index, jsonObject);
               ctx.stop();
               ctx = null;
               List<Application> apps = Lists.newArrayListWithCapacity(parsedApps.size());
               for(Application parsedApp : parsedApps) {
                  Application withMetrics = loadMetrics(parsedApp);
                  if(withMetrics != null) {
                     apps.add(withMetrics);
                  } else {
                     return null; //Error loading metrics for the application
                  }
               }
               return apps;
            default:
               logger.error("Unable to query applications for " + index + " (" + esResponse.getStatusCode() + ")");
               appRequestErrors.inc();
               return null;
         }
      } catch(Error e) {
         appRequestErrors.inc();
         throw e;
      } catch(Throwable t) {
         logger.error("Unable to query applications for " + index, t);
         appRequestErrors.inc();
         return null;
      } finally {
         if(ctx != null) {
            ctx.stop();
         }
      }
   }

   /**
    * Loads metrics for an application.
    * @param app The application.
    * @return The application with metrics added, or <code>null</code> on metric load error.
    */
   private Application loadMetrics(final Application app) {
      final Timer.Context ctx = nameRequestTimer.time();
      try {
         final Request esRequest = esEndpoint.postRequestBuilder(esEndpoint.buildIndexURI(app.index),
                 new ApplicationMetricsQuery(app.name, DEFAULT_ACTIVITY_RANGE).searchRequest.toJSON().getBytes(Charsets.UTF_8)).create();
         final Response esResponse = client.send(esRequest, requestOptions);

         switch(esResponse.getStatusCode()) {
            case 200:
               final ObjectNode jsonObject = mapper.readTree(parserFactory.createParser(esResponse.getBody().toByteArray()));
               return app.withMetrics(ApplicationParser.parseMetrics(jsonObject));
            default:
               logger.error("Unable to query metrics for " + app.name + " (" + esResponse.getStatusCode() + ")");
               nameRequestErrors.inc();
               return null;
         }
      } catch(Error e) {
         nameRequestErrors.inc();
         throw e;
      } catch(Throwable t) {
         logger.error("Unable to query metrics for " + app.name, t);
         nameRequestErrors.inc();
         return null;
      } finally {
         ctx.stop();
      }
   }


   /**
    * Loads a map of stats for an application.
    * @param app The application.
    * @return The map of stats or <code>null</code> on any stats load error.
    */
   private Map<String, Stats> loadStats(final Application app) {

      Map<String, Stats> statsMap = Maps.newHashMapWithExpectedSize(app.metrics.size());

      for(Metric metric : app.metrics) {

         final Timer.Context ctx = statsRequestTimer.time();
         final StatsKey key = buildStatsKey(app, metric);

         try {
            final Request esRequest = esEndpoint.postRequestBuilder(esEndpoint.buildIndexURI(key.index),
                    new StatsQuery(key.key, DEFAULT_ACTIVITY_RANGE, 0L, 0L).searchRequest.toJSON().getBytes(Charsets.UTF_8)).create();
            final Response esResponse = client.send(esRequest, requestOptions);

            switch(esResponse.getStatusCode()) {
               case 200:
                  final ObjectNode jsonObject = mapper.readTree(parserFactory.createParser(esResponse.getBody().toByteArray()));
                  statsMap.put(metric.name, StatsParser.parseStats(jsonObject, null));
                  break;
               default:
                  logger.error("Unable to query stats for " + key.key.application + " (" + esResponse.getStatusCode() + ")");
                  statsRequestErrors.inc();
                  return null;
            }
         } catch(Error e) {
            statsRequestErrors.inc();
            throw e;
         } catch(Throwable t) {
            logger.error("Unable to query stats for " + key.key.application, t);
            statsRequestErrors.inc();
            return null;
         } finally {
            ctx.stop();
         }
      }

      return statsMap; //If all goes well...
   }

   /**
    * Loads cached applications for an index.
    * @param index The index.
    * @return The cached applications or <code>null</code> on any load failures.
    */
   private CachedApplications loadCachedApplications(final String index) {
      final List<Application> apps = loadApplications(index);
      if(apps == null) {
         return null;
      }
      final List<CachedApplication> cachedApps = Lists.newArrayListWithCapacity(apps.size());
      for(Application app : apps) {
         Map<String, Stats> stats = loadStats(app);
         if(stats != null) {
            cachedApps.add(new CachedApplication(app, stats));
         } else {
            return null;
         }
      }

      return new CachedApplications(index, cachedApps, System.currentTimeMillis() + CACHE_LIFETIME_MILLIS);
   }

   /**
    * Gets cached applications, loading if needed and returning the previously cached
    * value (if any) on error.
    * @param index The index.
    * @return The cached applications or <code>null</code> if new and unable to load.
    */
   private CachedApplications getCachedApplications(final String index) {
      final CachedApplications cachedApps = appCache.get(index);
      if(cachedApps == null) {
         CachedApplications newApps = loadCachedApplications(index);
         if(newApps != null) {
            appCache.put(index, newApps);
         }
         return newApps;
      } else if(!cachedApps.isExpired(System.currentTimeMillis()) || cachedApps.pendingUpdate) {
         return cachedApps;
      } else { //Expired. Enqueue a background load and return the current value.
         appCache.put(index, cachedApps.toPendingUpdate()); //Ensure we don't enqueue more than one...
         requestExecutor.submit(new Runnable() {
            @Override
            public void run() {
               CachedApplications newApps = loadCachedApplications(index);
               if(newApps != null) {
                  appCache.put(index, newApps);
               } else { //Try again later...extend the lifetime of the current cached value.
                  appCache.put(index, cachedApps.extendExpiration(System.currentTimeMillis() + CACHE_LIFETIME_MILLIS));
                  //Note that extendExpiration resets the pending update flag...
               }
            }
         });
         return cachedApps;
      }
   }

   /**
    * Gets a map of (probably cached) stats for an application.
    * @param app The application.
    * @return The map of stats vs metric name or an empty map if not found.
    */
   private Map<String, Stats> getStats(final Application app) {
      CachedApplications apps = getCachedApplications(app.index);
      if(apps != null) {
         Map<String, Stats> stats = apps.getStats(app);
         return stats != null ? stats : ImmutableMap.<String, Stats>of();
      } else {
         return ImmutableMap.of();
      }
   }

   /**
    * Builds a unique key for stats that contains the field of interest.
    * @param app The application.
    * @param metric The metric.
    * @return The key.
    */
   private static StatsKey buildStatsKey(final Application app, final Metric metric) {

      final String field;
      switch(metric.type) {
         case TIMER:
         case METER:
            field = Fields.FIFTEEN_MINUTE_RATE_FIELD;
            break;
         case HISTOGRAM:
            field = Fields.P95_FIELD;
            break;
         case COUNTER:
            field = Fields.COUNT_FIELD;
            break;
         default:
            field = Fields.VALUE_FIELD;
            break;
      }

      return new StatsKey(MetricKey.createApplicationField(metric.name, app.name, field), app.index);
   }

   /**
    * A key for the stats cache.
    */
   private static class StatsKey {

      /**
       * Creates the key.
       * @param key The metric key.
       * @param index The index name.
       */
      StatsKey(final MetricKey key, final String index) {
         this.key = key;
         this.index = index;
         this.hashCode = Objects.hash(key, index);
      }

      @Override
      public boolean equals(Object o) {
         if(o instanceof StatsKey) {
            StatsKey other = (StatsKey)o;
            return key.equals(other.key) && index.equals(other.index);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return hashCode;
      }

      final MetricKey key;
      final String index;
      final int hashCode;
   }

   @Override
   public final Map<String, com.codahale.metrics.Metric> getMetrics() {
      return metrics;
   }

   /**
    * A cache of applications for an index.
    */
   private final ConcurrentMap<String, CachedApplications> appCache = Maps.newConcurrentMap();

   private final AsyncClient client;
   private final RequestOptions requestOptions;
   private final ESEndpoint esEndpoint;
   private final Logger logger;

   private final Timer statsRequestTimer;
   private final Counter statsRequestErrors;

   private final Timer nameRequestTimer;
   private final Counter nameRequestErrors;

   private final Timer appRequestTimer;
   private final Counter appRequestErrors;

   private final ImmutableMap<String, com.codahale.metrics.Metric> metrics;

   private final ListeningExecutorService requestExecutor;
}
