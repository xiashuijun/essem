package com.attribyte.essem.test;

import java.util.Collection;
import java.util.Random;

public class MetricsDriver {

   private static final Random rnd = new Random();

   /**
    * Creates the driver.
    * @param name A name (for logging).
    * @param minPeriodMillis The minimum period between simulated reports.
    * @param maxPeriodMillis The maximum period between simulated reports.
    * @param metrics The metrics to simulate.
    */
   public MetricsDriver(final String name,
                        final int minPeriodMillis,
                        final int maxPeriodMillis,
                        final Collection<RandomMetric> metrics) {

      this.simulator = new Thread(new Runnable() {
         final int range = maxPeriodMillis - minPeriodMillis;

         @Override
         public void run() {
            while(true) {
               try {
                  for(RandomMetric metric : metrics) {
                     metric.simulate();
                  }
                  Thread.sleep(minPeriodMillis + rnd.nextInt(range));
               } catch(InterruptedException ie) {
                  return;
               }
            }
         }
      });
      this.simulator.setDaemon(true);
   }

   /**
    * Starts the generation of simulated metrics.
    */
   public void start() {
      this.simulator.start();
   }

   /**
    * Stops the generation of simulated metrics.
    */
   public void stop() {
      this.simulator.interrupt();
   }

   private final Thread simulator;

}
