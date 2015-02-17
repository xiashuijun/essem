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

import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Timer;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import org.attribyte.essem.ReportProtos;
import org.attribyte.util.EncodingUtil;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import static com.attribyte.essem.Util.splitPath;

/**
 * Accept and enqueue reports.
 */
public class ReportServlet extends HttpServlet implements MetricSet {

   /**
    * Creates the servlet.
    * @param reportQueue The report queue.
    * @param indexAuthorization Authorizes index access based on name.
    */
   public ReportServlet(final ReportQueue reportQueue, final IndexAuthorization indexAuthorization) {
      this.reportQueue = reportQueue;
      this.indexAuthorization = indexAuthorization;
      this.acceptTimer = new Timer();
      this.acceptSize = new Histogram(new ExponentiallyDecayingReservoir());
      this.acceptCompressionRatio = new Histogram(new ExponentiallyDecayingReservoir());
      this.metrics = ImmutableMap.<String, Metric>builder()
              .putAll(this.reportQueue.getMetrics())
              .put("reports-accepted", acceptTimer)
              .put("report-size-bytes", acceptSize)
              .put("report-compression-ratio-percent", acceptCompressionRatio).build();
   }

   /**
    * The content encoding header.
    */
   public static final String CONTENT_ENCODING_HEADER = "Content-Encoding";

   @Override
   protected void doPut(final HttpServletRequest request,
                        final HttpServletResponse response) throws IOException {
      doPost(request, response);
   }

   @Override
   protected void doPost(final HttpServletRequest request,
                         final HttpServletResponse response) throws IOException {
      final Timer.Context acceptTime = acceptTimer.time();
      try {

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

         byte[] reportBytes = ByteStreams.toByteArray(request.getInputStream());
         String encoding = request.getHeader(CONTENT_ENCODING_HEADER);
         if(encoding != null && encoding.equals("deflate")) {
            double inputSize = (double)reportBytes.length;
            reportBytes = EncodingUtil.inflate(reportBytes);
            double originalSize = (double)reportBytes.length;
            double compressionRatio = originalSize / inputSize * 100.0;
            acceptCompressionRatio.update((int)compressionRatio);
         }
         acceptSize.update(reportBytes.length);

         final ReportProtos.EssemReport report;
         if(Strings.nullToEmpty(request.getHeader("Content-Type")).equals(Util.JSON_CONTENT_TYPE_HEADER)) {
            report = JSONReport.parseFrom(reportBytes);
         } else {
            report = ReportProtos.EssemReport.parseFrom(reportBytes);
         }
         reportQueue.enqueueReport(new QueuedReport(index, report));
      } catch(InterruptedException ie) {
         Thread.currentThread().interrupt();
      } catch(Exception e) {
         e.printStackTrace();
         response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      } finally {
         acceptTime.stop();
      }
   }

   @Override
   public void destroy() {
      shutdown();
   }

   /**
    * Shutdown the servlet.
    */
   public void shutdown() {
      reportQueue.shutdown();
   }

   /**
    * The report queue.
    */
   private final ReportQueue reportQueue;

   /**
    * Determines if a request is authorized for acces to an index.
    */
   private final IndexAuthorization indexAuthorization;

   /**
    * The amount of time required to ingest and parse the report.
    */
   private final Timer acceptTimer;

   /**
    * The size in bytes of the report.
    */
   private final Histogram acceptSize;

   /**
    * The compression ratio achieved for compressed reports.
    */
   private final Histogram acceptCompressionRatio;

   /**
    * An immutable map of all metrics.
    */
   private final ImmutableMap<String, Metric> metrics;

   @Override
   public Map<String, Metric> getMetrics() {
      return metrics;
   }
}