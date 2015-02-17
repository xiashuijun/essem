package com.attribyte.essem;

import com.attribyte.essem.model.MonitoredEndpoint;
import org.attribyte.essem.reporter.EssemReporter;
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.attribyte.api.Logger;
import org.attribyte.api.http.AsyncClient;
import org.attribyte.api.http.GetRequestBuilder;
import org.attribyte.api.http.Request;
import org.attribyte.api.http.RequestOptions;
import org.attribyte.api.http.Response;
import org.attribyte.essem.ReportProtos;

import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Periodically samples configured JSON endpoints for metric reports.
 */
public class JSONEndpointSampler {

   private class Monitor implements Runnable {

      /**
       * Creates an endpoint monitor.
       * @param endpoint The endpoint.
       */
      Monitor(final MonitoredEndpoint endpoint) {
         this.endpoint = endpoint;
         this.timer = new Timer();
         this.errors = new Meter();
         this.reportSize = new Histogram(new ExponentiallyDecayingReservoir());
         this.registry = new MetricRegistry();
         this.registry.register("essem-reports", this.timer);
         this.registry.register("essem-report-errors", this.errors);
         this.registry.register("essem-report-size-bytes", this.reportSize);
      }

      @Override
      public void run() {

         Timer.Context ctx = timer.time();

         try {

            Request request = endpoint.authHeader != null ?
                    new GetRequestBuilder(endpoint.url).addHeader(AUTHORIZATION_HEADER, endpoint.authHeader).create() :
                    new GetRequestBuilder(endpoint.url).create();

            Response response = httpClient.send(request, requestOptions);
            switch(response.getStatusCode()) {
               case 200:
                  byte[] reportBytes = response.getBody().toByteArray();
                  reportSize.update(reportBytes.length);

                  ReportProtos.EssemReport.Builder reportBuilder = JSONReport.parseBuilderFrom(reportBytes);
                  reportBuilder.setApplication(endpoint.app).setHost(endpoint.host);
                  if(endpoint.instance != null) {
                     reportBuilder.setInstance(endpoint.instance);
                  }

                  //This is "a bit of a" hack...
                  EssemReporter reporter = EssemReporter.newBuilder(null, null).build();
                  ReportProtos.EssemReport internalReport = reporter.buildReport(registry);
                  reportBuilder.addTimer(internalReport.getTimer(0));
                  reportBuilder.addMeter(internalReport.getMeter(0));
                  reportBuilder.addHistogram(internalReport.getHistogram(0));

                  queue.enqueueReport(new QueuedReport(endpoint.index, reportBuilder.build()));
                  break;
               default:
                  logger.error("Report sample HTTP error for '" + endpoint.url.toString() + "' (" + response.getStatusCode() + ")");
                  errors.mark();
            }

         } catch(Exception e) {
            logger.error("Report sample error for '" + endpoint.url.toString() + "'", e);
            errors.mark();
         } finally {
            ctx.stop();
         }
      }

      private final MonitoredEndpoint endpoint;
      private final Timer timer;
      private final Meter errors;
      private final Histogram reportSize;
      private final MetricRegistry registry;

   }

   /**
    * Create the sampler.
    * @param maxConcurrency The maximum concurrent report requests.
    * @param endpoints The collection of endpoints to sample.
    * @param httpClient The HTTP client.
    * @param requestOptions Cleint request options.
    * @param queue The queue to which reports are added.
    */
   JSONEndpointSampler(final int maxConcurrency,
                       final Collection<MonitoredEndpoint> endpoints,
                       final AsyncClient httpClient,
                       final RequestOptions requestOptions,
                       final ReportQueue queue,
                       final Logger logger) {

      this.scheduler =
              MoreExecutors.getExitingScheduledExecutorService(
                      new ScheduledThreadPoolExecutor(maxConcurrency,
                              new ThreadFactoryBuilder().setNameFormat("essem-endpoint-monitor-%d").build()
                      )
              );

      this.httpClient = httpClient;
      this.requestOptions = requestOptions;
      this.queue = queue;
      this.logger = logger;

      Random rnd = new Random();

      for(MonitoredEndpoint endpoint : endpoints) {
         this.scheduler.scheduleAtFixedRate(new Monitor(endpoint),
                 rnd.nextInt(endpoint.frequencySeconds), endpoint.frequencySeconds, TimeUnit.SECONDS);
      }
   }

   /**
    * Shutdown the sampler.
    */
   public void shutdown() {
      scheduler.shutdown();
   }

   private final ScheduledExecutorService scheduler;
   private final AsyncClient httpClient;
   private final ReportQueue queue;
   private final RequestOptions requestOptions;
   private final Logger logger;
   private static final String AUTHORIZATION_HEADER = "Authorization";
}
