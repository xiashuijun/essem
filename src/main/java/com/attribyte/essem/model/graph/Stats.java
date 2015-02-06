package com.attribyte.essem.model.graph;

import com.google.common.base.Objects;

/**
 * Statistics for a field.
 */
public class Stats {

   /**
    * Empty stats.
    */
   public static final Stats EMPTY_STATS = new Stats(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);


   public Stats(final long count, final double min, final double max, final double avg,
                final double sum, final double sumOfSquares, final double variance, final double std) {
      this.count = count;
      this.min = min;
      this.max = max;
      this.avg = avg;
      this.sum = sum;
      this.sumOfSquares = sumOfSquares;
      this.variance = variance;
      this.std = std;
   }

   @Override
   public String toString() {
      return Objects.toStringHelper(this)
              .add("count", count)
              .add("min", min)
              .add("max", max)
              .add("avg", avg)
              .add("sum", sum)
              .add("sum of squares", sumOfSquares)
              .add("variance", variance)
              .add("std", std).toString();
   }

   /**
    * The count of observations.
    */
   public final long count;

   /**
    * The minimum value.
    */
   public final double min;

   /**
    * The maximum value.
    */
   public final double max;

   /**
    * The average value.
    */
   public final double avg;

   /**
    * The sum of all values.
    */
   public final double sum;

   /**
    * The sum of the square of each value.
    */
   public final double sumOfSquares;

   /**
    * The variance.
    */
   public final double variance;

   /**
    * The standard deviation.
    */
   public final double std;
}
