package com.attribyte.essem.test;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

public class RandomGauge extends RandomMetric {

   /**
    * Creates the gauge.
    * @param registry The metric registry.
    * @param name The meter name.
    * @param minValue The minimum value.
    * @param maxValue The maximum value.
    */
   public RandomGauge(final MetricRegistry registry, final String name,
                      final double minValue, final double maxValue) {
      super(name);
      registry.register(name, new Gauge<Double>() {
         private final double range = maxValue - minValue;

         public Double getValue() {
            return minValue + RandomMetric.rnd.nextDouble() * range;
         }
      });
   }

   public void simulate() {
   }
}