package com.attribyte.essem.model.graph;

import com.google.common.base.Objects;

import java.text.NumberFormat;

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

   /**
    * Scale the stats by a multiplier.
    * @param mult The multiplier.
    * @return The new stats.
    */
   public final Stats scale(final double mult) {
      return new Stats(count,
              min * mult,
              max * mult,
              avg * mult,
              sum * mult,
              sumOfSquares * mult * mult,
              variance * mult * mult,
              std * mult);
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
    * Gets the minimum value with formatting.
    * @return The formatted value.
    */
   public String getFormattedMin() {
      return formatNumber(min);
   }

   /**
    * The maximum value.
    */
   public final double max;

   /**
    * Gets the maximum value with formatting.
    * @return The formatted value.
    */
   public String getFormattedMax() {
      return formatNumber(max);
   }

   /**
    * The average value.
    */
   public final double avg;

   /**
    * Gets the average value with formatting.
    * @return The formatted value.
    */
   public String getFormattedAvg() {
      return formatNumber(avg);
   }

   /**
    * The sum of all values.
    */
   public final double sum;

   /**
    * Gets the sum of values with formatting.
    * @return The formatted value.
    */
   public String getFormattedSum() {
      return formatNumber(sum);
   }

   /**
    * The sum of the square of each value.
    */
   public final double sumOfSquares;

   /**
    * Gets the sum of squares value with formatting.
    * @return The formatted value.
    */
   public String getFormattedSumOfSquares() {
      return formatNumber(sumOfSquares);
   }

   /**
    * The variance.
    */
   public final double variance;

   /**
    * Gets the variance value with formatting.
    * @return The formatted value.
    */
   public String getFormattedVariance() {
      return formatNumber(variance);
   }

   /**
    * The standard deviation.
    */
   public final double std;

   /**
    * Gets the standard deviation value with formatting.
    * @return The formatted value.
    */
   public String getFormattedStd() {
      return formatNumber(std);
   }

   private String formatNumber(final double num) {
      NumberFormat nf = NumberFormat.getNumberInstance();
      nf.setMaximumFractionDigits(3);
      nf.setMinimumIntegerDigits(1);
      return nf.format(num);
   }
}
