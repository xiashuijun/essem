package com.attribyte.essem.test;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

public class RandomCounter extends RandomMetric {

   /**
    * Creates the counter.
    * @param registry The metric registry.
    * @param name The meter name.
    * @param maxValue The maximum reported value for the counter.
    */
   public RandomCounter(final MetricRegistry registry, final String name,
                        final int maxValue) {
      super(name);
      this.maxValue = maxValue;
      this.counter = registry.counter(name);
      this.counter.inc(rnd.nextInt(maxValue));
   }

   @Override
   public void simulate() {
      long currCount = counter.getCount();
      if(currCount == 0L) {
         counter.inc(rnd.nextInt(maxValue));
      } else if(currCount >= maxValue) {
         counter.dec(rnd.nextInt(maxValue));
      } else {
         boolean inc = rnd.nextBoolean();
         if(inc) {
            counter.inc(rnd.nextInt(maxValue - (int)currCount));
         } else {
            counter.dec(rnd.nextInt((int)currCount));
         }
      }
   }

   private final Counter counter;
   private final int maxValue;
}
