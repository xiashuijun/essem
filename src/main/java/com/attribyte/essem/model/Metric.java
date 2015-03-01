/*
 * Copyright 2014, 2015 Attribyte, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package com.attribyte.essem.model;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * A metric.
 */
public class Metric {

   /**
    * Keeps only metrics with the specified type.
    * @param metrics The metrics.
    * @param types The types to keep.
    * @return The filtered list of metrics.
    */
   public static List<Metric> keep(final List<Metric> metrics, final EnumSet<Type> types) {
      if(metrics == null || metrics.size() == 0) {
         return metrics;
      } else {
         List<Metric> filtered = Lists.newArrayListWithCapacity(metrics.size());
         for(Metric metric : metrics) {
            if(types.contains(metric.type)) {
               filtered.add(metric);
            }
         }
         return filtered;
      }
   }

   /**
    * Create a comparator that compares metrics first by priority (highest first) and then by name if priority
    * is the same.
    * @param priorityFn A function that returns priority for a metric.
    */
   public static final Comparator<Metric> priorityComparator(final Function<Metric, Integer> priorityFn) {
      return new Comparator<Metric>() {
         @Override
         public int compare(final Metric o1, final Metric o2) {
            final Integer p1 = priorityFn.apply(o1);
            final Integer p2 = priorityFn.apply(o2);
            final int priortiy1 = p1 != null ? p1 : 0;
            final int priority2 = p2 != null ? p2 : 0;
            return priortiy1 == priority2 ? o1.name.compareTo(o2.name) : priortiy1 > priority2 ? -1 : 1;
         }
      };
   }

   /**
    * Compares metrics by name.
    */
   public static final Comparator<Metric> alphaComparator = new Comparator<Metric>() {
      @Override
      public int compare(final Metric o1, final Metric o2) {
         return o1.name.compareTo(o2.name);
      }
   };

   /**
    * Compares metrics by name.
    */
   public static final Comparator<Metric> systemLastAlphaComparator = new Comparator<Metric>() {
      @Override
      public int compare(final Metric o1, final Metric o2) {
         boolean firstIsSystem = o1.name.startsWith("system.") || o1.name.startsWith("jvm.");
         boolean secondIsSystem = o2.name.startsWith("system.") || o2.name.startsWith("jvm.");

         if(firstIsSystem == secondIsSystem) {
            return o1.name.compareTo(o2.name);
         } else {
            return firstIsSystem ? 1 : -1;
         }
      }
   };


   /**
    * Metric types.
    */
   public static enum Type {

      /**
       * An instantaneous value.
       */
      GAUGE,

      /**
       * A count.
       */
      COUNTER,

      /**
       * The distribution of a value.
       */
      HISTOGRAM,

      /**
       * A rate.
       */
      METER,

      /**
       * Throughput and timing distribution.
       */
      TIMER,

      /**
       * Type is unknown.
       */
      UNKNOWN;

      /**
       * Gets the type from a string.
       * @param str The string.
       * @return The type or <code>UNKNOWN</code> if not recognized as a type.
       */
      public static Type fromString(final String str) {
         Type type = typeMap.get(Strings.nullToEmpty(str).toLowerCase());
         return type != null ? type : UNKNOWN;
      }

      private static final ImmutableMap<String, Type> typeMap = ImmutableMap.of(
              "gauge", GAUGE,
              "counter", COUNTER,
              "histogram", HISTOGRAM,
              "meter", METER,
              "timer", TIMER);
   }

   /**
    * Use to create a super-likely-to-be-unique hash of the name.
    */
   private static final HashFunction hasher = Hashing.sha1();


   /**
    * Creates a metric.
    * @param name The name.
    * @param type The type.
    */
   public Metric(final String name, final Type type) {
      this.name = name;
      this.type = type;
      this.nameHash = hasher.newHasher().putString(name).putString(type.toString()).hash().toString().substring(0, 12);
   }

   @Override
   public int hashCode() {
      return Objects.hash(name, type);
   }

   @Override
   public boolean equals(Object o) {
      if(o instanceof Metric) {
         Metric other = (Metric)o;
         return other.type == type && other.name.equals(name);
      } else {
         return false;
      }
   }

   /**
    * Is this metric a gauge.
    * @return Is it a gauge?
    */
   public boolean isGauge() {
      return type == Type.GAUGE;
   }

   /**
    * Is this metric a counter.
    * @return Is it a counter?
    */
   public boolean isCounter() {
      return type == Type.COUNTER;
   }

   /**
    * Is this metric a histogram.
    * @return Is it a histogram?
    */
   public boolean isHistogram() {
      return type == Type.HISTOGRAM;
   }

   /**
    * Is this metric a meter.
    * @return Is it a meter?
    */
   public boolean isMeter() {
      return type == Type.METER;
   }

   /**
    * Is this metric a timer.
    * @return Is it a timer?
    */
   public boolean isTimer() {
      return type == Type.TIMER;
   }

   /**
    * Is this metric multi-field.
    * @return Is the metric a meter, timer or histogram?
    */
   public boolean isMultiField() {
      return !isSingleField();
   }

   /**
    * Does this metric have a single field (count/value).
    * @return Is the metric a counter or gauge?
    */
   public boolean isSingleField() {
      return type == Type.GAUGE || type == Type.COUNTER;
   }

   /**
    * A hash of the name.
    */
   public final String nameHash;

   /**
    * The metric name.
    */
   public final String name;

   /**
    * The metric type.
    */
   public final Type type;
}
