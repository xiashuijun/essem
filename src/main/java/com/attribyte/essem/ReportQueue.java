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

import com.codahale.metrics.Counter;
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.attribyte.essem.ReportProtos;

import javax.servlet.ServletException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * A report processing queue.
 * <ol>
 * <li>Source metrics reports are added to the queue.</li>
 * <li>N threads monitor the queue to extract the reports.</li>
 * <li>Each thread uses a <code>Reporter</code> to transform and send a report to a back-end (like ES).</li>
 * <li>If a report fails, it may be retried with a configurable strategy.</li>
 * </ol>
 */
public class ReportQueue implements MetricSet {

   /**
    * The default queue timeout in seconds (5).
    */
   public static final int DEFAULT_QUEUE_TIMEOUT_SECONDS = 5;

   /**
    * The config parameter key for queue timeout ('queue-timeout').
    */
   public static final String QUEUE_TIMEOUT_KEY = "queueTimeout";

   /**
    * The default queue capacity (0 == 'unlimited').
    */
   public static final int DEFAULT_QUEUE_CAPACITY = 0;

   /**
    * The config key for number of retry threads.
    */
   public static final String RETRY_THREADS_KEY = "retryThreads";

   /**
    * The default number of retry threads ('1').
    */
   public static final int DEFAULT_RETRY_THREADS = 1;

   /**
    * The config key for number of reporter threads.
    */
   public static final String REPORTER_THREADS_KEY = "reporterThreads";

   /**
    * The default number of reporter threads ('1').
    */
   public static final int DEFAULT_REPORTER_THREADS = 1;

   /**
    * The config key for size of reporter drain list.
    */
   public static final String REPORTER_DRAIN_SIZE_KEY = "reporterDrainSize";

   /**
    * The default size of the reporter drain list ('64').
    */
   public static final int DEFAULT_DRAIN_SIZE = 64;

   /**
    * The config parameter key for queue capacity.
    */
   public static final String QUEUE_CAPACITY_KEY = "queueCapacity";

   /**
    * Creates a report queue from servlet config.
    * @param reporter The reporter.
    * @param retryStrategy The retry strategy.
    * @param props The properties.
    * @return The report queue.
    * @throws ServletException on configuration error.
    */
   public static ReportQueue fromProperties(final Reporter reporter,
                                            final RetryStrategy retryStrategy,
                                            final Properties props) throws ServletException {

      final int queueCapacity = getIntProperty(QUEUE_CAPACITY_KEY, DEFAULT_QUEUE_CAPACITY, props);
      final int queueTimeoutSeconds = getIntProperty(QUEUE_TIMEOUT_KEY, DEFAULT_QUEUE_TIMEOUT_SECONDS, props);
      final int numReporterThreads = getIntProperty(REPORTER_THREADS_KEY, DEFAULT_REPORTER_THREADS, props);
      final int numRetryThreads = getIntProperty(RETRY_THREADS_KEY, DEFAULT_RETRY_THREADS, props);
      final int reporterProcessingListSize = getIntProperty(REPORTER_DRAIN_SIZE_KEY, DEFAULT_DRAIN_SIZE, props);
      return new ReportQueue(queueCapacity, queueTimeoutSeconds, numReporterThreads, numRetryThreads,
              reporter, retryStrategy, reporterProcessingListSize);
   }

   private static int getIntProperty(final String key, final int defaultValue,
                                     final Properties props) {
      return Integer.parseInt(props.getProperty(key, Integer.toString(defaultValue)));
   }

   /**
    * Creates a report processing queue.
    * @param queueCapacity The capacity of the report queue. If < 1, capacity is limited only by available memory.
    * @param queueTimeoutSeconds The maximum amount of time to wait for space to be available in the report queue.
    * @param numReporterThreads The number of reporter threads.
    * @param numRetryThreads The number of retry threads.
    * @param reporter The reporter instance (must be thread-safe).
    * @param retryStrategy The retry strategy.
    * @param reporterProcessingListSize The size of the reporter thread processing list.
    */
   public ReportQueue(final int queueCapacity,
                      final int queueTimeoutSeconds,
                      final int numReporterThreads,
                      final int numRetryThreads,
                      final Reporter reporter,
                      final RetryStrategy retryStrategy,
                      final int reporterProcessingListSize) {
      this.queueTimeoutSeconds = queueTimeoutSeconds;
      this.retryStrategy = retryStrategy;
      this.reportQueue = queueCapacity < 1 ? new LinkedBlockingDeque<>()
              : new ArrayBlockingQueue<>(queueCapacity);
      this.reporter = reporter;

      this.failedReportService = Executors.newScheduledThreadPool(numRetryThreads,
              new ThreadFactoryBuilder().setNameFormat("essem-failed-report-%d").build());

      this.reporterThreads = Lists.newArrayListWithCapacity(numReporterThreads);
      for(int i = 0; i < numReporterThreads; i++) {
         final Thread currThread = threadFactory.newThread(new Runnable() {
            final List<QueuedReport> processingList = Lists.newArrayListWithCapacity(reporterProcessingListSize + 1);

            @Override
            public void run() {
               while(true) {
                  try {
                     QueuedReport first = reportQueue.take();
                     processingList.clear();
                     processingList.add(first);
                     queueSize.dec();
                     queueSize.dec(reportQueue.drainTo(processingList, reporterProcessingListSize));
                     reporter.report(ImmutableList.copyOf(processingList), enqueueRetryFn);
                  } catch(InterruptedException ie) {
                     return;
                  }
               }
            }
         });

         this.reporterThreads.add(currThread);
      }

      startReporters();
   }

   /**
    * Stop reporting.
    */
   public void shutdown() {
      for(Thread reporterThread : reporterThreads) {
         reporterThread.interrupt();
      }
      failedReportService.shutdown();
   }

   /**
    * Start threads that monitor the report queue.
    */
   private void startReporters() {
      for(Thread reporterThread : reporterThreads) {
         reporterThread.start();
      }
   }

   /**
    * Enqueue a report.
    * @param report The report.
    * @return Was the offer successful?
    * @throws InterruptedException on interrupt during offer.
    */
   public boolean enqueueReport(final QueuedReport report) throws InterruptedException {
      final int metricCount = countMetrics(report.report);
      reportMetricCount.update(metricCount);
      if(metricCount > 0) {
         boolean offered = reportQueue.offer(report, queueTimeoutSeconds, TimeUnit.SECONDS);
         if(offered) {
            queueSize.inc();
         }
         return offered;
      } else {
         return false;
      }
   }

   private final Function<QueuedReport, Boolean> enqueueRetryFn = this::enqueueRetry;

   private boolean enqueueRetry(final QueuedReport failedReport) {

      long backoffMillis = retryStrategy.backoffMillis(failedReport.failedCount);

      if(backoffMillis > 0L) {
         failedReportService.schedule((Runnable)() -> reporter.retry(failedReport, this::enqueueRetry), backoffMillis, TimeUnit.MILLISECONDS);
         return true;
      } else {
         return false;
      }
   }

   /**
    * Counts the total number of metrics in a report.
    * @param report The report.
    * @return the total number of metrics.
    */
   private int countMetrics(final ReportProtos.EssemReport report) {
      return report.getCounterCount() + report.getGaugeCount() + report.getHistogramCount()
              + report.getMeterCount() + report.getTimerCount();
   }

   /**
    * The queue holding metrics ready to be stored.
    */
   private final BlockingQueue<QueuedReport> reportQueue;

   /**
    * The number of metrics in reports.
    */
   private final Histogram reportMetricCount = new Histogram(new ExponentiallyDecayingReservoir());

   /**
    * The current number of reports in the queue.
    */
   private final Counter queueSize = new Counter();

   /**
    * The maximum number of seconds to wait for space to be available in the report queue.
    */
   private final int queueTimeoutSeconds;

   /**
    * Reporters.
    */
   private final List<Thread> reporterThreads;

   /**
    * The failed reporter.
    */
   private final Reporter reporter;

   /**
    * A service used to retry failed reports.
    */
   private final ScheduledExecutorService failedReportService;

   /**
    * The retry strategy for failed reports.
    */
   private final RetryStrategy retryStrategy;

   /**
    * The thread factory.
    */
   private static final ThreadFactory threadFactory =
           new ThreadFactoryBuilder()
                   .setNameFormat("essem-reporter-%d")
                   .setDaemon(true)
                   .build();

   /**
    * An immutable map of all metrics.
    */
   private final ImmutableMap<String, Metric> metrics = ImmutableMap.<String, Metric>builder()
           .put("report-metric-count", reportMetricCount)
           .put("report-queue-size", queueSize).build();

   @Override
   public Map<String, Metric> getMetrics() {
      return metrics;
   }
}
