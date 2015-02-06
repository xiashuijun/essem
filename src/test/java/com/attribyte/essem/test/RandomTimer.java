package com.attribyte.essem.test;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import java.util.concurrent.TimeUnit;

public class RandomTimer extends RandomMetric {

   /**
    * Creates the meter.
    * @param registry The metric registry.
    * @param name The meter name.
    * @param minTimeMillis The minimum reported time in milliseconds.
    * @param maxTimeMillis The maximum reported time in milliseconds.
    * @param maxEvents The maximum number events reported per simulated call.
    */
   public RandomTimer(final MetricRegistry registry, final String name,
                      final int minTimeMillis,
                      final int maxTimeMillis,
                      final int maxEvents) {
      super(name);
      this.minTimeMillis = minTimeMillis;
      this.range = maxTimeMillis - minTimeMillis;
      this.timer = registry.timer(name);
      this.maxEvents = maxEvents;
   }

   @Override
   public void simulate() {
      int count = rnd.nextInt(maxEvents) + 1;
      for(int i = 0; i < count; i++) {
         timer.update(minTimeMillis + rnd.nextInt(range), TimeUnit.MILLISECONDS);
      }
   }

   private final Timer timer;
   private final int minTimeMillis;
   private final int range;
   private final int maxEvents;
}