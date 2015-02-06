package com.attribyte.essem;

import com.attribyte.essem.model.MonitoredApplication;
import com.attribyte.essem.model.graph.MetricKey;
import com.attribyte.essem.query.SelectForDeleteQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.codec.Charsets;
import org.attribyte.api.Logger;
import org.attribyte.api.http.AsyncClient;
import org.attribyte.api.http.Request;
import org.attribyte.api.http.Response;

import java.net.URI;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Periodically performs (application-specific) index maintenance like deleting
 * old metrics, issuing alerts, etc.
 */
public class IndexKeeper {

   /**
    * Creates the index "keeper".
    * @param maxConcurrency The maximum concurrent maintenance jobs.
    * @param httpClient The HTTP client.
    * @param esEndpoint The ES endpoint.
    * @param logger A logger.
    */
   IndexKeeper(final List<MonitoredApplication> monitoredApplications,
               final int frequencyMinutes,
               final int maxConcurrency,
               final AsyncClient httpClient,
               final ESEndpoint esEndpoint,
               final Logger logger) {

      this.scheduler =
              MoreExecutors.getExitingScheduledExecutorService(
                      new ScheduledThreadPoolExecutor(maxConcurrency,
                              new ThreadFactoryBuilder().setNameFormat("essem-index-keeper-%d").build()
                      )
              );

      Random rnd = new Random();

      for(final MonitoredApplication monitoredApplication : monitoredApplications) {
         if(monitoredApplication.properties.containsKey(RETAIN_DAYS_KEY)) {
            final int retainDays = Integer.parseInt(monitoredApplication.properties.get(RETAIN_DAYS_KEY));
            final boolean debug = !monitoredApplication.properties.containsKey(DEBUG_KEY) ||
                    monitoredApplication.properties.get(DEBUG_KEY).equalsIgnoreCase("true"); //Debug mode on if no property.
            if(retainDays > 0L) {

               this.scheduler.scheduleAtFixedRate(new Runnable() {

                  final MetricKey key = new MetricKey(null, monitoredApplication.application, null, null);

                  @Override
                  public void run() {
                     try {
                        URI searchURI = esEndpoint.buildIndexURI(monitoredApplication.index);
                        SelectForDeleteQuery deleteQuery = new SelectForDeleteQuery(key, retainDays);
                        Request esRequest = esEndpoint.postRequestBuilder(searchURI, deleteQuery.searchRequest.toJSON().getBytes(Charsets.UTF_8)).create();
                        Response esResponse = httpClient.send(esRequest);
                        final long maxDeleted;
                        if(esResponse.getStatusCode() == 200) {
                           ObjectNode indexObject = Util.mapper.readTree(Util.parserFactory.createParser(esResponse.getBody().toByteArray()));
                           JsonNode hits = indexObject.path("hits").path("total");
                           if(!hits.isMissingNode()) {
                              maxDeleted = hits.longValue();
                           } else {
                              maxDeleted = 0L;
                           }
                        } else {
                           maxDeleted = 0L;
                        }

                        if(maxDeleted > 0L) {
                           if(!debug) {
                              logger.info("Index keeper deleting " + maxDeleted + " old metrics for " + monitoredApplication.toString());
                              URI deleteURI = esEndpoint.buildDeleteByQueryURI(monitoredApplication.index, deleteQuery.searchRequest.toJSON());
                              Request esDeleteRequest = esEndpoint.deleteRequestBuilder(deleteURI).create();
                              Response esDeleteResponse = httpClient.send(esDeleteRequest);
                              switch(esDeleteResponse.getStatusCode()) {
                                 case 200:
                                    logger.info("Index keeper deleted approximately " + maxDeleted + " old metrics for " + monitoredApplication.toString());
                                    break;
                                 default:
                                    logger.info("Index keeper deleted failed for " + monitoredApplication.toString() + "(" + esDeleteResponse.getStatusCode() + ")");

                              }
                           } else {
                              logger.info("Index keeper in debug mode! Skipping delete of " + maxDeleted +
                                      " old metrics for " + monitoredApplication.toString());
                           }
                        } else {
                           logger.info("Index keeper checked " + monitoredApplication.toString() + " with no old metrics");
                        }

                     } catch(Error e) {
                        throw e;
                     } catch(Exception ex) {
                        logger.error("Problem maintaining '" + monitoredApplication.index + "'", ex);
                     }
                  }
               }, rnd.nextInt(60), frequencyMinutes * 60, TimeUnit.SECONDS);
            }
         }
      }
   }

   /**
    * The key in the monitored keys properties that identifies
    * the number of retention days.
    */
   public static final String RETAIN_DAYS_KEY = "retainDays";

   /**
    * The key in the monitored keys properties that identifies
    * if in 'debug' mode (will only perform a non-destructive
    * operation and log).
    */
   public static final String DEBUG_KEY = "debug";

   /**
    * Shutdown the sampler.
    */
   public void shutdown() {
      scheduler.shutdown();
   }

   private final ScheduledExecutorService scheduler;
}
