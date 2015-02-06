package com.attribyte.essem.test;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;

public class RandomHistogram extends RandomMetric {

   /**
    * Creates the meter.
    * @param registry The metric registry.
    * @param name The meter name.
    * @param minValue The minimum value.
    * @param maxValue The maximum value.
    */
   public RandomHistogram(final MetricRegistry registry, final String name,
                          final int minValue, final int maxValue) {
      super(name);
      this.minValue = minValue;
      this.range = maxValue - minValue;
      this.histogram = registry.histogram(name);
   }

   @Override
   public void simulate() {
      histogram.update(minValue + rnd.nextInt(range));
   }

   private final Histogram histogram;
   private final int minValue;
   private final int range;
}
