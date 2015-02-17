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

import com.attribyte.essem.query.Fields;
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.protobuf.ByteString;
import org.attribyte.api.Logger;
import org.attribyte.api.http.AsyncClient;
import org.attribyte.api.http.Request;
import org.attribyte.api.http.Response;
import org.attribyte.essem.ReportProtos;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;

@SuppressWarnings("NullableProblems")
class ESReporter implements Reporter {

   /**
    * Creates a reporter.
    * @param esEndpoint The elasticsearch endpoint.
    * @param schema The index schema.
    * @param httpClient The (Async) HTTP client.
    * @param logger The logger.
    */
   public ESReporter(final ESEndpoint esEndpoint,
                     final ByteString schema,
                     final AsyncClient httpClient,
                     final Logger logger) {
      this.esEndpoint = esEndpoint;
      this.schema = schema;
      this.httpClient = httpClient;
      this.logger = logger;
   }

   /**
    * Builds the URI to bulk-write to a specified index.
    * @param indexName The index name.
    * @return The URI.
    */
   private URI buildIndexURI(final String indexName) {
      try {
         return new URI(esEndpoint.uri.getScheme(),
                 esEndpoint.uri.getUserInfo(),
                 esEndpoint.uri.getHost(),
                 esEndpoint.uri.getPort(), "/" + indexName + "/_bulk", null, null);
      } catch(URISyntaxException use) {
         throw new AssertionError();
      }
   }

   @Override
   public boolean createStore(final String indexName) throws IOException {
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

   @Override
   public void report(final Collection<QueuedReport> reports,
                      final Function<QueuedReport, Boolean> failed) {

      for(final QueuedReport report : reports) {
         final Timer.Context requestGenerateTime = requestGenerateTimer.time();
         try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
            generateReport(baos, report.report);
            byte[] esRequestBody = baos.toByteArray();
            requestSize.update(esRequestBody.length);
            Request esRequest = esEndpoint.postRequestBuilder(buildIndexURI(report.index), esRequestBody).create();
            final Timer.Context requestSendTime = requestSendTimer.time();
            Futures.addCallback(httpClient.asyncSend(esRequest), new FutureCallback<Response>() {
               public void onSuccess(final Response response) {
                  requestSendTime.stop();
                  if(response.getStatusCode() > 299) {
                     requestErrorMeter.mark();
                     failed.apply(new QueuedReport(report, new IOException("HTTP " + response.getStatusCode())));
                     try {
                        logger.error("Failed ES write: " + new String(response.getBody().toByteArray(), Charsets.UTF_8));
                     } catch(IOException ioe) {
                        logger.error("Failed ES write + response read failed", ioe);
                     }
                  }
               }

               public void onFailure(final Throwable throwable) {
                  logger.error("Failed ES write", throwable);
                  requestErrorMeter.mark();
                  requestSendTime.stop();
                  failed.apply(new QueuedReport(report, throwable));
               }
            });
         } catch(IOException ioe) {
            logger.error("Failed ES write", ioe);
            failed.apply(new QueuedReport(report, ioe));
         } finally {
            requestGenerateTime.stop();
         }
      }
   }

   @Override
   public void retry(final QueuedReport failedReport,
                     final Function<QueuedReport, Boolean> failed) {

      final Timer.Context requestGenerateTime = requestGenerateTimer.time();
      try {
         ByteArrayOutputStream baos = new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
         generateReport(baos, failedReport.report);
         byte[] esRequestBody = baos.toByteArray();
         Request esRequest = esEndpoint.postRequestBuilder(buildIndexURI(failedReport.index), esRequestBody).create();
         final Timer.Context requestSendTime = requestSendTimer.time();
         Futures.addCallback(httpClient.asyncSend(esRequest), new FutureCallback<Response>() {
            public void onSuccess(final Response response) {
               requestSendTime.stop();
               if(response.getStatusCode() > 299) {
                  requestErrorMeter.mark();
                  failed.apply(failedReport.incrementCount(new IOException("HTTP " + response.getStatusCode())));
                  try {
                     logger.error("Failed ES write: " + new String(response.getBody().toByteArray(), Charsets.UTF_8));
                  } catch(IOException ioe) {
                     logger.error("Failed ES write + response read failed", ioe);
                  }
               }
            }

            public void onFailure(final Throwable throwable) {
               logger.error("Failed ES write", throwable);
               requestErrorMeter.mark();
               requestSendTime.stop();
               failed.apply(failedReport.incrementCount(throwable));
            }
         });
      } catch(IOException ioe) {
         logger.error("Failed ES write", ioe);
         failed.apply(failedReport.incrementCount(ioe));
      } finally {
         requestGenerateTime.stop();
      }
   }

   private void generateReport(final OutputStream os,
                               final ReportProtos.EssemReport report) throws IOException {

      JsonGenerator generator = jsonFactory.createGenerator(os);
      long timestamp = report.hasTimestamp() ? report.getTimestamp() : System.currentTimeMillis();
      String application = Strings.emptyToNull(report.hasApplication() ? report.getApplication().trim() : null);
      String host = Strings.emptyToNull(report.hasHost() ? report.getHost().trim() : null);
      String instance = Strings.emptyToNull(report.hasInstance() ? report.getInstance().trim() : null);

      for(ReportProtos.EssemReport.Gauge gauge : report.getGaugeList()) {
         if(isValid(gauge)) {
            generateAction(generator, Fields.GAUGE_TYPE);
            os.write(NEWLINE);
            generateGauge(generator, gauge, application, host, instance, timestamp);
            os.write(NEWLINE);
         }
      }

      for(ReportProtos.EssemReport.Counter counter : report.getCounterList()) {
         if(isValid(counter)) {
            generateAction(generator, Fields.COUNTER_TYPE);
            os.write(NEWLINE);
            generateCounter(generator, counter, application, host, instance, timestamp);
            os.write(NEWLINE);
         }
      }

      for(ReportProtos.EssemReport.Meter meter : report.getMeterList()) {
         if(isValid(meter)) {
            generateAction(generator, Fields.METER_TYPE);
            os.write(NEWLINE);
            generateMeter(generator, meter, application, host, instance, timestamp);
            os.write(NEWLINE);
         }
      }

      for(ReportProtos.EssemReport.Histogram histogram : report.getHistogramList()) {
         if(isValid(histogram)) {
            generateAction(generator, Fields.HISTOGRAM_TYPE);
            os.write(NEWLINE);
            generateHistogram(generator, histogram, application, host, instance, timestamp);
            os.write(NEWLINE);
         }
      }

      for(ReportProtos.EssemReport.Timer timer : report.getTimerList()) {
         if(isValid(timer)) {
            generateAction(generator, Fields.TIMER_TYPE);
            os.write(NEWLINE);
            generateTimer(generator, timer, application, host, instance, timestamp);
            os.write(NEWLINE);
         }
      }
   }

   static final boolean isValid(final ReportProtos.EssemReport.Gauge gauge) {
      return gauge.hasName() && Strings.emptyToNull(gauge.getName().trim()) != null;
   }

   static final void generateGauge(final JsonGenerator generator,
                                   final ReportProtos.EssemReport.Gauge gauge,
                                   final String application,
                                   final String host,
                                   final String instance,
                                   final long timestamp) throws IOException {

      generator.writeStartObject();
      writeStringField(generator, Fields.APPLICATION_FIELD, application);
      writeStringField(generator, Fields.HOST_FIELD, host);
      writeStringField(generator, Fields.INSTANCE_FIELD, instance);
      writeStringField(generator, Fields.NAME_FIELD, gauge.getName().trim());
      if(gauge.hasValue()) {
         generator.writeNumberField(Fields.VALUE_FIELD, gauge.getValue());
      } else if(gauge.hasComment()) {
         generator.writeNumberField(Fields.VALUE_FIELD, 0.0);
         writeStringField(generator, Fields.COMMENT_FIELD, gauge.getComment());
      } else {
         generator.writeNullField(Fields.VALUE_FIELD);
      }

      generator.writeNumberField(Fields.TIMESTAMP_FIELD, timestamp);
      generator.writeEndObject();
      generator.flush();
   }

   static final boolean isValid(final ReportProtos.EssemReport.Counter counter) {
      return counter.hasName() && Strings.emptyToNull(counter.getName().trim()) != null;
   }

   static final void generateCounter(final JsonGenerator generator,
                                     final ReportProtos.EssemReport.Counter counter,
                                     final String application,
                                     final String host,
                                     final String instance,
                                     final long timestamp) throws IOException {
      generator.writeStartObject();
      writeStringField(generator, Fields.APPLICATION_FIELD, application);
      writeStringField(generator, Fields.HOST_FIELD, host);
      writeStringField(generator, Fields.INSTANCE_FIELD, instance);
      writeStringField(generator, Fields.NAME_FIELD, counter.getName().trim());
      generator.writeNumberField(Fields.COUNT_FIELD, counter.getCount());
      generator.writeNumberField(Fields.TIMESTAMP_FIELD, timestamp);
      generator.writeEndObject();
      generator.flush();
   }

   static final boolean isValid(final ReportProtos.EssemReport.Meter meter) {
      return meter.hasName() && Strings.emptyToNull(meter.getName().trim()) != null;
   }

   static final void generateMeter(final JsonGenerator generator,
                                   final ReportProtos.EssemReport.Meter meter,
                                   final String application,
                                   final String host,
                                   final String instance,
                                   final long timestamp) throws IOException {

      generator.writeStartObject();
      writeStringField(generator, Fields.APPLICATION_FIELD, application);
      writeStringField(generator, Fields.HOST_FIELD, host);
      writeStringField(generator, Fields.INSTANCE_FIELD, instance);
      writeStringField(generator, Fields.NAME_FIELD, meter.getName().trim());
      generator.writeNumberField(Fields.ONE_MINUTE_RATE_FIELD, meter.getOneMinuteRate());
      generator.writeNumberField(Fields.FIVE_MINUTE_RATE_FIELD, meter.getFiveMinuteRate());
      generator.writeNumberField(Fields.FIFTEEN_MINUTE_RATE_FIELD, meter.getFifteenMinuteRate());
      generator.writeNumberField(Fields.MEAN_RATE_FIELD, meter.getMeanRate());
      generator.writeNumberField(Fields.COUNT_FIELD, meter.getCount());
      generator.writeNumberField(Fields.TIMESTAMP_FIELD, timestamp);
      generator.writeEndObject();
      generator.flush();
   }

   static final boolean isValid(final ReportProtos.EssemReport.Timer timer) {
      return timer.hasName() && Strings.emptyToNull(timer.getName().trim()) != null;
   }

   static final void generateTimer(final JsonGenerator generator,
                                   final ReportProtos.EssemReport.Timer timer,
                                   final String application,
                                   final String host,
                                   final String instance,
                                   final long timestamp) throws IOException {

      generator.writeStartObject();
      writeStringField(generator, Fields.APPLICATION_FIELD, application);
      writeStringField(generator, Fields.HOST_FIELD, host);
      writeStringField(generator, Fields.INSTANCE_FIELD, instance);
      writeStringField(generator, Fields.NAME_FIELD, timer.getName().trim());
      generator.writeNumberField(Fields.ONE_MINUTE_RATE_FIELD, timer.getOneMinuteRate());
      generator.writeNumberField(Fields.FIVE_MINUTE_RATE_FIELD, timer.getFiveMinuteRate());
      generator.writeNumberField(Fields.FIFTEEN_MINUTE_RATE_FIELD, timer.getFifteenMinuteRate());
      generator.writeNumberField(Fields.MEAN_RATE_FIELD, timer.getMeanRate());
      generator.writeNumberField(Fields.COUNT_FIELD, timer.getCount());
      generator.writeNumberField(Fields.MAX_FIELD, timer.getMax());
      generator.writeNumberField(Fields.MIN_FIELD, timer.getMin());
      generator.writeNumberField(Fields.MEAN_FIELD, timer.getMean());
      generator.writeNumberField(Fields.P50_FIELD, timer.getMedian());
      generator.writeNumberField(Fields.P75_FIELD, timer.getPercentile75());
      generator.writeNumberField(Fields.P95_FIELD, timer.getPercentile95());
      generator.writeNumberField(Fields.P98_FIELD, timer.getPercentile98());
      generator.writeNumberField(Fields.P99_FIELD, timer.getPercentile99());
      generator.writeNumberField(Fields.P999_FIELD, timer.getPercentile999());
      generator.writeNumberField(Fields.STD_FIELD, timer.getStd());
      generator.writeNumberField(Fields.TIMESTAMP_FIELD, timestamp);
      generator.writeEndObject();
      generator.flush();
   }

   static final boolean isValid(final ReportProtos.EssemReport.Histogram histogram) {
      return histogram.hasName() && Strings.emptyToNull(histogram.getName().trim()) != null;
   }

   static final void generateHistogram(final JsonGenerator generator,
                                       final ReportProtos.EssemReport.Histogram histogram,
                                       final String application,
                                       final String host,
                                       final String instance,
                                       final long timestamp) throws IOException {

      generator.writeStartObject();
      writeStringField(generator, Fields.APPLICATION_FIELD, application);
      writeStringField(generator, Fields.HOST_FIELD, host);
      writeStringField(generator, Fields.INSTANCE_FIELD, instance);
      writeStringField(generator, Fields.NAME_FIELD, histogram.getName().trim());
      generator.writeNumberField(Fields.COUNT_FIELD, histogram.getCount());
      generator.writeNumberField(Fields.MAX_FIELD, histogram.getMax());
      generator.writeNumberField(Fields.MIN_FIELD, histogram.getMin());
      generator.writeNumberField(Fields.MEAN_FIELD, histogram.getMean());
      generator.writeNumberField(Fields.P50_FIELD, histogram.getMedian());
      generator.writeNumberField(Fields.P75_FIELD, histogram.getPercentile75());
      generator.writeNumberField(Fields.P95_FIELD, histogram.getPercentile95());
      generator.writeNumberField(Fields.P98_FIELD, histogram.getPercentile98());
      generator.writeNumberField(Fields.P99_FIELD, histogram.getPercentile99());
      generator.writeNumberField(Fields.P999_FIELD, histogram.getPercentile999());
      generator.writeNumberField(Fields.STD_FIELD, histogram.getStd());
      generator.writeNumberField(Fields.TIMESTAMP_FIELD, timestamp);
      generator.writeEndObject();
      generator.flush();
   }

   static final void writeStringField(final JsonGenerator generator, final String name, final String value) throws IOException {
      if(value != null) {
         generator.writeStringField(name, value);
      } else {
         generator.writeNullField(name);
      }
   }

   /**
    * Generates the ES bulk api "action" command.
    * @param generator The JSON generator.
    * @param objType The object type.
    * @throws IOException
    */
   static final void generateAction(final JsonGenerator generator,
                                    final String objType) throws IOException {
      generator.writeStartObject();
      generator.writeObjectFieldStart("index");
      generator.writeStringField("_type", objType);
      generator.writeEndObject();
      generator.writeEndObject();
      generator.flush();
   }

   private static final byte NEWLINE = '\n';
   private static final int INITIAL_BUFFER_SIZE = 16384;

   private static final JsonFactory jsonFactory = new JsonFactory();

   private final Timer requestGenerateTimer = new Timer();
   private final Timer requestSendTimer = new Timer();
   private final Meter requestErrorMeter = new Meter();
   private final Histogram requestSize = new Histogram(new ExponentiallyDecayingReservoir());

   private final AsyncClient httpClient;
   private final ESEndpoint esEndpoint;
   private final ByteString schema;
   private final Logger logger;

   private final ImmutableMap<String, Metric> metrics = ImmutableMap.<String, Metric>builder()
           .put("requests-generated", requestGenerateTimer)
           .put("request-size", requestSendTimer)
           .put("requests-sent", requestSendTimer)
           .put("request-errors", requestErrorMeter).build();

   public Map<String, Metric> getMetrics() {
      return metrics;
   }
}
