package com.attribyte.essem.test;

import java.util.Random;

public abstract class RandomMetric {

   /**
    * Creates the metric.
    * @param name The metric name.
    */
   RandomMetric(final String name) {
      this.name = name;
   }

   /**
    * Generates a simulated metric.
    */
   protected abstract void simulate();

   /**
    * The random number generator.
    */
   protected static final Random rnd = new Random();

   /**
    * The metric name.
    */
   public final String name;

}
