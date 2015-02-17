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
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.attribyte.essem.ReportProtos;
import org.junit.Test;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.*;

import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * Very basic reporter tests.
 */
public class EssemReporterTest {

   @Test
   public void testMeta() throws Exception {
      MetricRegistry registry = new MetricRegistry();
      EssemReporter reporter = EssemReporter.newBuilder(new URI("http://127.0.0.1"), registry)
              .convertRatesTo(TimeUnit.MINUTES)
              .convertDurationsTo(TimeUnit.NANOSECONDS)
              .build();
      ReportProtos.EssemReport report = reporter.buildReport(registry);
      assertTrue(report.getTimestamp() > 0L);
      assertEquals("MINUTES", report.getRateUnit().toString());
      assertEquals("NANOS", report.getDurationUnit().toString());
   }

   @Test
   public void testCounter() throws Exception {
      MetricRegistry registry = new MetricRegistry();
      Counter counter = registry.counter("test-counter-0");
      counter.inc(314);

      EssemReporter reporter = EssemReporter.newBuilder(new URI("http://127.0.0.1"), registry).build();
      ReportProtos.EssemReport report = reporter.buildReport(registry);
      assertEquals(1, report.getCounterCount());
      assertEquals("test-counter-0", report.getCounter(0).getName());
      assertEquals(314, report.getCounter(0).getCount());
   }

   @Test
   public void testGauge() throws Exception {
      MetricRegistry registry = new MetricRegistry();
      registry.register("test-gauge-0", new Gauge<Integer>() {
         public Integer getValue() {
            return 314;
         }

      });

      EssemReporter reporter = EssemReporter.newBuilder(new URI("http://127.0.0.1"), registry)
              .convertRatesTo(TimeUnit.MINUTES)
              .convertDurationsTo(TimeUnit.NANOSECONDS)
              .build();

      ReportProtos.EssemReport report = reporter.buildReport(registry);
      assertEquals(1, report.getGaugeCount());
      assertEquals("test-gauge-0", report.getGauge(0).getName());
      assertEquals(314, (int)report.getGauge(0).getValue());
   }

   @Test
   public void testMeter() throws Exception {
      MetricRegistry registry = new MetricRegistry();
      Meter meter = registry.meter("test-meter-0");
      meter.mark(314);

      EssemReporter reporter = EssemReporter.newBuilder(new URI("http://127.0.0.1"), registry).build();
      ReportProtos.EssemReport report = reporter.buildReport(registry);
      assertEquals(1, report.getMeterCount());
      assertEquals("test-meter-0", report.getMeter(0).getName());
      assertEquals(314, report.getMeter(0).getCount());
      assertTrue(report.getMeter(0).getMeanRate() > 0.0);
   }

   @Test
   public void testTimer() throws Exception {
      MetricRegistry registry = new MetricRegistry();
      Timer timer = registry.timer("test-timer-0");
      timer.update(500, TimeUnit.MILLISECONDS);
      timer.update(300, TimeUnit.MILLISECONDS);

      EssemReporter reporter = EssemReporter.newBuilder(new URI("http://127.0.0.1"), registry).build();
      ReportProtos.EssemReport report = reporter.buildReport(registry);
      assertEquals(1, report.getTimerCount());
      assertEquals("test-timer-0", report.getTimer(0).getName());
      assertEquals(2, report.getTimer(0).getCount());
      assertTrue(report.getTimer(0).getMeanRate() > 0.0);
      assertEquals(500, (int)report.getTimer(0).getMax());
      assertEquals(300, (int)report.getTimer(0).getMin());
      assertTrue(report.getTimer(0).getStd() > 0.0);
      assertEquals(400, (int)report.getTimer(0).getMean());
      assertEquals(400, (int)report.getTimer(0).getMedian());
      assertEquals(500, (int)report.getTimer(0).getPercentile75());
      assertEquals(500, (int)report.getTimer(0).getPercentile95());
      assertEquals(500, (int)report.getTimer(0).getPercentile98());
      assertEquals(500, (int)report.getTimer(0).getPercentile99());
      assertEquals(500, (int)report.getTimer(0).getPercentile999());
   }

   @Test
   public void testHistogram() throws Exception {
      MetricRegistry registry = new MetricRegistry();
      Histogram histogram = registry.histogram("test-histo-0");
      histogram.update(500);
      histogram.update(300);

      EssemReporter reporter = EssemReporter.newBuilder(new URI("http://127.0.0.1"), registry).build();
      ReportProtos.EssemReport report = reporter.buildReport(registry);
      assertEquals(1, report.getHistogramCount());
      assertEquals("test-histo-0", report.getHistogram(0).getName());
      assertEquals(2, report.getHistogram(0).getCount());
      assertEquals(500, (int)report.getHistogram(0).getMax());
      assertEquals(300, (int)report.getHistogram(0).getMin());
      assertTrue(report.getHistogram(0).getStd() > 0.0);
      assertEquals(400, (int)report.getHistogram(0).getMean());
      assertEquals(400, (int)report.getHistogram(0).getMedian());
      assertEquals(500, (int)report.getHistogram(0).getPercentile75());
      assertEquals(500, (int)report.getHistogram(0).getPercentile95());
      assertEquals(500, (int)report.getHistogram(0).getPercentile98());
      assertEquals(500, (int)report.getHistogram(0).getPercentile99());
      assertEquals(500, (int)report.getHistogram(0).getPercentile999());
   }
}
