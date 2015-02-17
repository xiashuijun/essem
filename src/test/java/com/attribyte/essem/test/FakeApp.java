package com.attribyte.essem.test;

import org.attribyte.essem.reporter.EssemReporter;
import com.codahale.metrics.Clock;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class FakeApp {

   /**
    * Creates a fake application with metrics from a JSON node.
    * @param name The application name.
    * @param host The application host.
    * @param instance The application instance.
    * @param obj The object node.
    * @param reportURI The essem URI to which metrics are reported.
    * @param endpointUsername The report endpoint username.
    * @param endpointPassword The report endpoint password.
    * @param simulatePeriodSeconds If > 0 reported time in the past will increment by this amount per report.
    * @param reportFrequencySeconds The reporting frequency in seconds.
    * @throws IOException if object is not a valid application.
    */
   public FakeApp(final String name, final String host, final String instance,
                  final JsonNode obj,
                  final URI reportURI,
                  final String endpointUsername,
                  final String endpointPassword,
                  final int simulatePeriodSeconds,
                  int reportFrequencySeconds) throws IOException {

      this.name = name;

      for(JsonNode node : obj.get("gauge")) {
         System.out.println("Registered gauge: " + gaugeFromJSON(extractName(node), node).name);
      }

      for(JsonNode node : obj.get("counter")) {
         System.out.println("Registered counter: " + counterFromJSON(extractName(node), node).name);
      }

      for(JsonNode node : obj.get("meter")) {
         System.out.println("Registered meter: " + meterFromJSON(extractName(node), node).name);
      }

      for(JsonNode node : obj.get("histogram")) {
         System.out.println("Registered histogram: " + histogramFromJSON(extractName(node), node).name);
      }

      for(JsonNode node : obj.get("timer")) {
         System.out.println("Registered timer: " + timerFromJSON(extractName(node), node).name);
      }

      int driverPeriodMillis = extractInt(obj, "driverPeriodMillis");
      driver = new MetricsDriver(name, driverPeriodMillis / 2, driverPeriodMillis * 2, metrics);

      final Clock clock;

      if(simulatePeriodSeconds < 1) {
         clock = new Clock.UserTimeClock();
      } else {
         clock = new Clock() { //Use a backward running clock to populate old data

            long currMillis = System.currentTimeMillis();

            @Override
            public synchronized long getTime() {
               currMillis -= simulatePeriodSeconds * 1000L;
               currTime.set(currMillis);
               return currMillis;
            }

            @Override
            public long getTick() {
               return System.nanoTime();
            }
         };
      }

      this.reporter = EssemReporter.newBuilder(reportURI, registry)
              .forApplication(name)
              .forHost(host)
              .forInstance(instance)
              .withBasicAuthorization(endpointUsername, endpointPassword)
              .withClock(clock)
              .build();

      this.reportFrequencySeconds = reportFrequencySeconds;
   }

   /**
    * Gets the "current" time (being reported).
    * @return The time.
    */
   public long currTime() {
      long ts = currTime.get();
      return ts > 0 ? ts : System.currentTimeMillis();
   }

   private AtomicLong currTime = new AtomicLong();

   /**
    * Starts the fake app.
    */
   public void start() {
      driver.start();
      reporter.start(reportFrequencySeconds, TimeUnit.SECONDS);
   }

   /**
    * Stops the fake app.
    */
   public void stop() {
      driver.stop();
      reporter.stop();
   }

   /**
    * Builds a random gauge from JSON.
    * @param name The gauge name.
    * @param obj The JSON object.
    * @return The gauge.
    * @throws IOException If object is not a valid gauge.
    */
   RandomMetric gaugeFromJSON(String name, JsonNode obj) throws IOException {
      return addMetric(new RandomGauge(registry, name, extractDouble(obj, "min"), extractDouble(obj, "max")));
   }

   /**
    * Builds a random counter from JSON.
    * @param name The counter name.
    * @param obj The JSON object.
    * @return The counter.
    * @throws IOException If object is not a valid counter.
    */
   RandomMetric counterFromJSON(String name, JsonNode obj) throws IOException {
      return addMetric(new RandomCounter(registry, name, extractInt(obj, "max")));
   }

   /**
    * Builds a random histogram from JSON.
    * @param name The histogram name.
    * @param obj The JSON object.
    * @return The histogram.
    * @throws IOException If object is not a valid histogram.
    */
   RandomMetric histogramFromJSON(String name, JsonNode obj) throws IOException {
      return addMetric(new RandomHistogram(registry, name, extractInt(obj, "min"), extractInt(obj, "max")));
   }

   /**
    * Builds a random meter from JSON.
    * @param name The meter name.
    * @param obj The JSON object.
    * @return The meter.
    * @throws IOException If object is not a valid meter.
    */
   RandomMetric meterFromJSON(String name, JsonNode obj) throws IOException {
      return addMetric(new RandomMeter(registry, name, extractInt(obj, "maxEvents")));
   }

   /**
    * Builds a random timer from JSON.
    * @param name The timer name.
    * @param obj The JSON object.
    * @return The timer.
    * @throws IOException If object is not a valid timer.
    */
   RandomMetric timerFromJSON(String name, JsonNode obj) throws IOException {
      return addMetric(new RandomTimer(registry, name,
              extractInt(obj, "minTimeMillis"),
              extractInt(obj, "maxTimeMillis"),
              extractInt(obj, "maxEvents")));
   }

   /**
    * Extracts the name from a node.
    * @param obj The object node.
    * @return The name, if set.
    * @throws IOException If name is not present.
    */
   String extractName(JsonNode obj) throws IOException {
      if(obj.has("name")) {
         String name = Strings.nullToEmpty(obj.get("name").textValue()).trim();
         if(name.length() > 0) {
            if(names.contains(name)) {
               throw new IOException("Duplicate 'name' for " + obj.toString());
            } else {
               names.add(name);
               return name;
            }
         } else {
            throw new IOException("Expecting 'name' for " + obj.toString());
         }
      } else {
         throw new IOException("Expecting 'name' for " + obj.toString());
      }
   }

   /**
    * Extracts a named double value.
    * @param obj The object.
    * @param name The property name.
    * @return The double value.
    * @throws IOException If property is not present or is not a number.
    */
   double extractDouble(JsonNode obj, String name) throws IOException {
      if(obj.has(name)) {
         if(obj.get(name).isNumber()) {
            return obj.get(name).asDouble();
         } else {
            throw new IOException("Expecting '" + name + "' to be a number for " + obj.toString());
         }
      } else {
         throw new IOException("Expecting '" + name + "' for " + obj.toString());
      }
   }

   /**
    * Extracts a named integer value.
    * @param obj The object.
    * @param name The property name.
    * @return The integer value.
    * @throws IOException If property is not present or is not an integer.
    */
   int extractInt(JsonNode obj, String name) throws IOException {
      if(obj.has(name)) {
         if(obj.get(name).isIntegralNumber()) {
            return obj.get(name).asInt();
         } else {
            throw new IOException("Expecting '" + name + "' to be an integer for " + obj.toString());
         }
      } else {
         throw new IOException("Expecting '" + name + "' for " + obj.toString());
      }
   }

   /**
    * Adds a metric to the collection.
    * @param metric The metric to add.
    * @return The input metric.
    */
   RandomMetric addMetric(final RandomMetric metric) {
      metrics.add(metric);
      return metric;
   }

   final String name;

   /**
    * The metrics reporter.
    */
   final EssemReporter reporter;

   /**
    * The reporting frequency in seconds.
    */
   final int reportFrequencySeconds;

   /**
    * The random metrics driver.
    */
   final MetricsDriver driver;

   /**
    * A collection of all metrics.
    */
   final List<RandomMetric> metrics = Lists.newArrayListWithExpectedSize(128);

   /**
    * The registry for all metrics.
    */
   final MetricRegistry registry = new MetricRegistry();

   /**
    * A set of all names.
    */
   final Set<String> names = Sets.newHashSet();
}