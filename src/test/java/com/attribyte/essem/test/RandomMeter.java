package com.attribyte.essem.test;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

public class RandomMeter extends RandomMetric {

   /**
    * Creates the meter.
    * @param registry The metric registry.
    * @param name The meter name.
    * @param maxEvents The maximum number of events to record per simulation.
    */
   public RandomMeter(final MetricRegistry registry,
                      final String name,
                      final int maxEvents) {
      super(name);
      this.meter = registry.meter(name);
      this.maxEvents = maxEvents;
   }

   @Override
   public void simulate() {
      int count = rnd.nextInt(maxEvents) + 1;
      meter.mark(count);
   }

   private final Meter meter;
   private final int maxEvents;
}
