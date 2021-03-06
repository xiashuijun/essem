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

package org.attribyte.essem.reporter;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import com.google.protobuf.ByteString;
import org.attribyte.essem.ReportProtos;
import org.attribyte.essem.metrics.HDRReservoir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * A scheduled metric reporter that reports over HTTP(s) using the "essem"
 * protocol.
 */
public class EssemReporter extends ScheduledReporter implements MetricSet {

   /**
    * Selects the HDR histogram reporting method.
    */
   public enum HdrReport {

      /**
       * Don't report.
       */
      NONE,

      /**
       * Report the total histogram.
       */
      TOTAL,

      /**
       * Report the snapshot histogram.
       */
      SNAPSHOT
   }

   /**
    * Creates a builder.
    * @param uri The URI to report to.
    * @param registry the registry to report
    * @return The builder.
    */
   public static Builder newBuilder(final URI uri, final MetricRegistry registry) {
      return new Builder(uri, registry);
   }

   public static class Builder {

      /**
       * Creates a builder.
       * @param uri The essem endpoint URI.
       * @param registry The registry to report.
       */
      private Builder(final URI uri, final MetricRegistry registry) {
         this.uri = uri;
         this.registry = registry;
         this.clock = Clock.defaultClock();
         this.rateUnit = TimeUnit.SECONDS;
         this.durationUnit = TimeUnit.MILLISECONDS;
         this.filter = MetricFilter.ALL;
         this.application = null;
         this.host = null;
         this.instance = null;
      }

      /**
       * Configures the clock.
       * @param clock The clock.
       * @return A self-reference.
       */
      public Builder withClock(final Clock clock) {
         this.clock = clock;
         return this;
      }

      /**
       * Sets the application name.
       * @param application The application name.
       * @return A self-reference.
       */
      public Builder forApplication(final String application) {
         this.application = application;
         return this;
      }

      /**
       * Sets the host.
       * @param host The host.
       * @return A self-reference.
       */
      public Builder forHost(final String host) {
         this.host = host;
         return this;
      }

      /**
       * Sets the instance name.
       * @param instance The instance name.
       * @return A self-reference.
       */
      public Builder forInstance(final String instance) {
         this.instance = instance;
         return this;
      }

      /**
       * Configures the rate conversion. Default is seconds.
       * @param rateUnit The rate unit.
       * @return A self-reference.
       */
      public Builder convertRatesTo(final TimeUnit rateUnit) {
         this.rateUnit = rateUnit;
         return this;
      }

      /**
       * Configures the duration conversion. Default is milliseconds.
       * @param durationUnit The duration unit.
       * @return A self-reference.
       */
      public Builder convertDurationsTo(final TimeUnit durationUnit) {
         this.durationUnit = durationUnit;
         return this;
      }

      /**
       * Applies a filter to the registry before reporting.
       * @param filter The filter.
       * @return A self-reference.
       */
      public Builder filter(final MetricFilter filter) {
         this.filter = filter;
         return this;
      }

      /**
       * Sets a value to be sent as the value of the <code>Authorization</code> header.
       * @param authValue The authorization header value.
       * @return A self-reference.
       */
      public Builder withAuthorization(final String authValue) {
         this.authValue = authValue;
         return this;
      }

      /**
       * Configures HTTP Basic auth.
       * @param username The username.
       * @param password The password.
       * @return A self-reference.
       */
      public Builder withBasicAuthorization(final String username, final String password) {
         if(username != null && password != null) {
            byte[] up = (username + ":" + password).getBytes(Charsets.UTF_8);
            return withAuthorization("Basic " + BaseEncoding.base64().encode(up));
         } else {
            return this;
         }
      }

      /**
       * Configures 'deflate' of sent reports.
       * @param deflate Should deflate be used?
       * @return A self-reference.
       */
      public Builder withDeflate(final boolean deflate) {
         this.deflate = deflate;
         return this;
      }

      /**
       * Configures the reporter to skip reports for metrics unchanged
       * since the last report. Default is 'true'.
       */
      public Builder skipUnchangedMetrics(final boolean skipUnchangedMetrics) {
         this.skipUnchangedMetrics = skipUnchangedMetrics;
         return this;
      }

      /**
       * Sets the HDR histogram report mode. Default is {@code SNAPSHOT}.
       * @param hdrReport The HDR report mode.
       * @return A self-reference.
       */
      public Builder setHdrReport(final HdrReport hdrReport) {
         this.hdrReport = hdrReport;
         return this;
      }

      /**
       * Builds an immutable reporter instance.
       * @return The immutable reporter.
       */
      public EssemReporter build() {
         return new EssemReporter(uri, authValue, deflate,
                 registry, clock, application, host, instance, filter, rateUnit, durationUnit,
                 skipUnchangedMetrics, hdrReport);
      }


      private final URI uri;
      private final MetricRegistry registry;
      private String authValue;

      private Clock clock = Clock.defaultClock();
      private String application;
      private String host;
      private String instance;
      private boolean deflate;
      private TimeUnit rateUnit = TimeUnit.SECONDS;
      private TimeUnit durationUnit = TimeUnit.MILLISECONDS;
      private boolean skipUnchangedMetrics = false;
      private MetricFilter filter;
      private HdrReport hdrReport = HdrReport.SNAPSHOT;
   }

   protected EssemReporter(final URI uri,
                           final String authValue,
                           final boolean deflate,
                           final MetricRegistry registry,
                           final Clock clock,
                           final String application,
                           final String host,
                           final String instance,
                           final MetricFilter filter,
                           final TimeUnit rateUnit,
                           final TimeUnit durationUnit,
                           final boolean skipUnchangedMetrics,
                           final HdrReport hdrReport) {
      super(registry, "essem-reporter", filter, rateUnit, durationUnit);
      this.uri = uri;
      this.authValue = authValue;
      this.deflate = deflate;
      this.clock = clock;
      this.application = application;
      this.host = host;
      this.instance = instance;
      this.rateUnit = rateUnit;
      this.durationUnit = durationUnit;
      this.lastReportedCount = skipUnchangedMetrics ? Maps.<String, Long>newConcurrentMap() : null;
      this.hdrReport = hdrReport;
   }

   /**
    * Builds a report for a specified registry.
    * @param registry The registry.
    * @return The report.
    */
   public ReportProtos.EssemReport buildReport(final MetricRegistry registry) {
      return buildReport(registry.getGauges(),
              registry.getCounters(),
              registry.getHistograms(),
              registry.getMeters(),
              registry.getTimers());
   }

   /**
    * Builds a report for a registry.
    * @param registry The registry.
    * @param filter A filter.
    * @return The report.
    */
   public ReportProtos.EssemReport buildReport(final MetricRegistry registry,
                                               final MetricFilter filter) {
      return buildReport(registry.getGauges(filter),
              registry.getCounters(filter),
              registry.getHistograms(filter),
              registry.getMeters(filter),
              registry.getTimers(filter));
   }

   private ReportProtos.EssemReport buildReport(SortedMap<String, Gauge> gauges,
                                                SortedMap<String, Counter> counters,
                                                SortedMap<String, Histogram> histograms,
                                                SortedMap<String, Meter> meters,
                                                SortedMap<String, Timer> timers) {

      ReportProtos.EssemReport.Builder builder = ReportProtos.EssemReport.newBuilder();
      builder.setTimestamp(clock.getTime());
      builder.setDurationUnit(toProto(durationUnit));
      builder.setRateUnit(toProto(rateUnit));
      if(application != null) builder.setApplication(application);
      if(host != null) builder.setHost(host);
      if(instance != null) builder.setInstance(instance);

      lastMetricCount.set(gauges.size() + counters.size() + histograms.size() + meters.size() + timers.size());

      for(Map.Entry<String, Gauge> gauge : gauges.entrySet()) {
         Object val = gauge.getValue().getValue();
         ReportProtos.EssemReport.Gauge.Builder gaugeBuilder = builder.addGaugeBuilder();
         gaugeBuilder.setName(gauge.getKey());
         if(val instanceof Number) {
            gaugeBuilder.setValue(((Number)val).doubleValue());
         } else {
            gaugeBuilder.setComment(val.toString());
         }
      }

      for(Map.Entry<String, Counter> counter : counters.entrySet()) {
         String name = counter.getKey();
         long value = counter.getValue().getCount();
         if(!skipCountedReport(name, value)) {
            builder.addCounterBuilder()
                    .setName(name)
                    .setCount(value);
         }
      }

      for(Map.Entry<String, Meter> nv : meters.entrySet()) {
         String name = nv.getKey();
         Meter meter = nv.getValue();
         if(!skipCountedReport(name, meter.getCount())) {
            builder.addMeterBuilder()
                    .setName(name)
                    .setCount(meter.getCount())
                    .setOneMinuteRate(convertRate(meter.getOneMinuteRate()))
                    .setFiveMinuteRate(convertRate(meter.getFiveMinuteRate()))
                    .setFifteenMinuteRate(convertRate(meter.getFifteenMinuteRate()))
                    .setMeanRate(convertRate(meter.getMeanRate()));
         }
      }

      for(Map.Entry<String, Histogram> nv : histograms.entrySet()) {
         String name = nv.getKey();
         Histogram histogram = nv.getValue();
         if(!skipCountedReport(name, histogram.getCount())) {
            Snapshot snapshot = histogram.getSnapshot();
            final HDRReservoir.HDRSnapshot hdrSnapshot;
            if(snapshot instanceof HDRReservoir.HDRSnapshot && hdrReport != HdrReport.NONE) {
               hdrSnapshot = (HDRReservoir.HDRSnapshot)snapshot;
               switch(hdrReport) {
                  case TOTAL:
                     snapshot = hdrSnapshot.totalSnapshot();
                     break;
                  case SNAPSHOT:
                     snapshot = hdrSnapshot.sinceLastSnapshot();
                     break;
               }
            } else {
               hdrSnapshot = null;
            }

            ReportProtos.EssemReport.Histogram.Builder histogramBuilder = builder.addHistogramBuilder();
            histogramBuilder
                    .setName(name)
                    .setCount(histogram.getCount())
                    .setMax(snapshot.getMax())
                    .setMin(snapshot.getMin())
                    .setMedian(snapshot.getMedian())
                    .setMean(snapshot.getMean())
                    .setStd(snapshot.getStdDev())
                    .setPercentile75(snapshot.get75thPercentile())
                    .setPercentile95(snapshot.get95thPercentile())
                    .setPercentile98(snapshot.get98thPercentile())
                    .setPercentile99(snapshot.get99thPercentile())
                    .setPercentile999(snapshot.get999thPercentile());

            if(hdrSnapshot != null) {
               org.HdrHistogram.Histogram storedHistogram = hdrSnapshot.sinceLastSnapshot().getHistogram();
               ByteBuffer buf = ByteBuffer.allocate(storedHistogram.getNeededByteBufferCapacity());
               int compressedSize = storedHistogram.encodeIntoCompressedByteBuffer(buf);
               buf.rewind();
               histogramBuilder.setHdrHistogram(ByteString.copyFrom(buf, compressedSize));
            }
         }
      }

      for(Map.Entry<String, Timer> nv : timers.entrySet()) {
         final String name = nv.getKey();
         Timer timer = nv.getValue();
         if(!skipCountedReport(name, timer.getCount())) {
            Snapshot snapshot = timer.getSnapshot();
            final HDRReservoir.HDRSnapshot hdrSnapshot;
            if(snapshot instanceof HDRReservoir.HDRSnapshot && hdrReport != HdrReport.NONE) {
               hdrSnapshot = (HDRReservoir.HDRSnapshot)snapshot;
               switch(hdrReport) {
                  case TOTAL:
                     snapshot = hdrSnapshot.totalSnapshot();
                     break;
                  case SNAPSHOT:
                     snapshot = hdrSnapshot.sinceLastSnapshot();
                     break;
               }
            } else {
               hdrSnapshot = null;
            }

            ReportProtos.EssemReport.Timer.Builder timerBuilder = builder.addTimerBuilder();
            timerBuilder
                    .setName(nv.getKey())
                    .setOneMinuteRate(convertRate(timer.getOneMinuteRate()))
                    .setFiveMinuteRate(convertRate(timer.getFiveMinuteRate()))
                    .setFifteenMinuteRate(convertRate(timer.getFifteenMinuteRate()))
                    .setMeanRate(convertRate(timer.getMeanRate()))
                    .setCount(timer.getCount())
                    .setMax(convertDuration(snapshot.getMax()))
                    .setMin(convertDuration(snapshot.getMin()))
                    .setMedian(convertDuration(snapshot.getMedian()))
                    .setMean(convertDuration(snapshot.getMean()))
                    .setStd(convertDuration(snapshot.getStdDev()))
                    .setPercentile75(convertDuration(snapshot.get75thPercentile()))
                    .setPercentile95(convertDuration(snapshot.get95thPercentile()))
                    .setPercentile98(convertDuration(snapshot.get98thPercentile()))
                    .setPercentile99(convertDuration(snapshot.get99thPercentile()))
                    .setPercentile999(convertDuration(snapshot.get999thPercentile()));

            if(hdrSnapshot != null) {
               org.HdrHistogram.Histogram storedHistogram = hdrSnapshot.sinceLastSnapshot().getHistogram();
               ByteBuffer buf = ByteBuffer.allocate(storedHistogram.getNeededByteBufferCapacity());
               int compressedSize = storedHistogram.encodeIntoCompressedByteBuffer(buf);
               buf.rewind();
               timerBuilder.setHdrHistogram(ByteString.copyFrom(buf, compressedSize));
            }
         }
      }

      return builder.build();
   }

   @Override
   public void report(SortedMap<String, Gauge> gauges,
                      SortedMap<String, Counter> counters,
                      SortedMap<String, Histogram> histograms,
                      SortedMap<String, Meter> meters,
                      SortedMap<String, Timer> timers) {

      Timer.Context ctx = sendTimer.time();
      try {
         int responseCode = send(uri, buildReport(gauges, counters, histograms, meters, timers));
         if(responseCode / 100 != 2) {
            LOGGER.warn("EssemReporter: Unable to report (" + responseCode + ")");
         } else {
            LOGGER.debug("EssemReporter: Reported (" + responseCode + ")");
         }
      } catch(IOException ioe) {
         ioe.printStackTrace();
         LOGGER.warn("Unable to report to Essem", ioe);
         sendErrors.mark();
      } finally {
         ctx.stop();
      }
   }

   private int send(final URI uri, final ReportProtos.EssemReport report) throws IOException {
      HttpURLConnection conn = null;
      InputStream is = null;
      try {
         URL url = uri.toURL();
         conn = (HttpURLConnection)url.openConnection();
         if(conn != null) {
            conn.setRequestMethod("PUT");
            conn.setRequestProperty(CONTENT_TYPE_HEADER, PROTOBUF_CONTENT_TYPE);
            if(!Strings.isNullOrEmpty(authValue)) {
               conn.setRequestProperty(AUTHORIZATION_HEADER, authValue);
            }
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);

            byte[] reportBytes = report.toByteArray();
            if(deflate) {
               conn.setRequestProperty(CONTENT_ENCODING_HEADER, DEFLATE_ENCODING);
               reportBytes = deflate(reportBytes);
            }
            conn.setFixedLengthStreamingMode(reportBytes.length);
            reportSize.update(reportBytes.length);
            conn.connect();
            OutputStream os = conn.getOutputStream();
            os.write(reportBytes);
            os.flush();
            os.close();
            int code = conn.getResponseCode();
            is = code / 100 == 2 ? conn.getInputStream() : conn.getErrorStream();
            discardInputAndClose(is);
            is = null;
            return code;
         } else {
            throw new IOException("Unable to 'PUT' to " + uri.toString());
         }
      } catch(IOException ioe) {
         if(conn != null && is == null) discardInputAndClose(conn.getErrorStream());
         throw ioe;
      } finally {
         if(conn != null) {
            conn.disconnect();
         }
      }
   }

   /**
    * Converts time units to the proto enum.
    * @param timeUnit The time unit.
    * @return The protobuf enum value.
    */
   protected ReportProtos.EssemReport.TimeUnit toProto(final TimeUnit timeUnit) {
      switch(timeUnit) {
         case NANOSECONDS: return ReportProtos.EssemReport.TimeUnit.NANOS;
         case MICROSECONDS: return ReportProtos.EssemReport.TimeUnit.MICROS;
         case MILLISECONDS: return ReportProtos.EssemReport.TimeUnit.MILLIS;
         case SECONDS: return ReportProtos.EssemReport.TimeUnit.SECONDS;
         case MINUTES: return ReportProtos.EssemReport.TimeUnit.MINUTES;
         case HOURS: return ReportProtos.EssemReport.TimeUnit.HOURS;
         case DAYS: return ReportProtos.EssemReport.TimeUnit.DAYS;
         default: throw new AssertionError();
      }
   }

   /**
    * Reads and discards all input from a stream.
    * @param is The input stream.
    * @throws IOException on read error.
    */
   private void discardInputAndClose(final InputStream is) throws IOException {
      if(is != null) {
         ByteStreams.toByteArray(is);
         try {
            is.close();
         } catch(IOException ioe) {
            //Ignore
         }
      }
   }

   /**
    * Deflates bytes.
    * @param b The bytes to deflate.
    * @return The deflated bytes.
    */
   private static byte[] deflate(final byte[] b) {
      try {
         Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, false); //nowrap = false
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         DeflaterOutputStream dos = new DeflaterOutputStream(baos, deflater);
         dos.write(b);
         dos.close();
         return baos.toByteArray();
      } catch(IOException ioe) {
         throw new AssertionError("I/O exception on in-memory stream");
      }
   }

   /**
    * The authorization header.
    */
   public static final String AUTHORIZATION_HEADER = "Authorization";

   /**
    * The content type header.
    */
   public static final String CONTENT_TYPE_HEADER = "Content-Type";

   /**
    * The protocol buffer content type (application/x-protobuf).
    */
   public static final String PROTOBUF_CONTENT_TYPE = "application/x-protobuf";

   /**
    * The content encoding header.
    */
   public static final String CONTENT_ENCODING_HEADER = "Content-Encoding";

   /**
    * The 'deflate' content type.
    */
   public static final String DEFLATE_ENCODING = "deflate";

   /**
    * The default logger.
    */
   private static final Logger LOGGER = LoggerFactory.getLogger(EssemReporter.class);

   private final String application;
   private final String host;
   private final String instance;
   private final Clock clock;
   private final URI uri;
   private final TimeUnit rateUnit;
   private final TimeUnit durationUnit;
   private final String authValue;
   private final boolean deflate;

   private final Timer sendTimer = new org.attribyte.essem.metrics.Timer();
   private final Meter sendErrors = new Meter();
   private final Histogram reportSize = new Histogram(new HDRReservoir(2, HDRReservoir.REPORT_SNAPSHOT_HISTOGRAM));
   private final Counter skippedUnchanged = new Counter();

   private final ImmutableMap<String, Metric> metrics =
           ImmutableMap.of(
                   "reports", sendTimer,
                   "failed-reports", sendErrors,
                   "report-size-bytes", reportSize,
                   "skipped-unchanged", skippedUnchanged,
                   "report-count", new Gauge<Integer>() {
                      public Integer getValue() {
                         return lastMetricCount.get();
                      }
                   }
           );
   @Override
   public Map<String, Metric> getMetrics() {
      return metrics;
   }

   /**
    * A map that contains the last reported value for counters, meters, histograms and timers.
    * <p>
    *    If configured, and the previously reported value is unchanged, the metric
    *    will not be reported.
    * </p>
    */
   private final Map<String, Long> lastReportedCount;


   /**
    * The number of metrics last reported.
    */
   private final AtomicInteger lastMetricCount = new AtomicInteger();

   /**
    * Should reporting be skipped for this counted metric?
    *
    * <p>
    *    If the count is zero, no values have ever been added to the metric.
    *    If the count is unchanged, no measurements have been recorded since the last report.
    * </p>
    * @param name The metric name.
    * @param currentValue The current value.
    * @return Should reporting be skipped?
    */
   private boolean skipCountedReport(final String name, final long currentValue) {
      if(lastReportedCount == null) {
         return false;
      } else if(lastReportedCount.getOrDefault(name, Long.MIN_VALUE) == currentValue) {
         skippedUnchanged.inc();
         return true;
      } else {
         lastReportedCount.put(name, currentValue);
         return false;
      }
   }

   /**
    * The HDR histogram report mode.
    */
   private final HdrReport hdrReport;
}