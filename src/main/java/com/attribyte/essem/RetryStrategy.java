/*
 * Copyright 2014 Attribyte, LLC
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

package com.attribyte.essem;

import javax.servlet.ServletException;
import java.util.Properties;

/**
 * Allows custom retry strategy like exponential back-off.
 */
public interface RetryStrategy {

   /**
    * A retry strategy that never retries.
    */
   public static class Never implements RetryStrategy {

      @Override
      public long backoffMillis(final int numAttempts) {
         return -1L;
      }

      @Override
      public void init(Properties props) throws ServletException {
         //Nothing to do...
      }
   }

   /**
    * A retry strategy that uses exponential back-off.
    */
   public static class ExponentialBackoff implements RetryStrategy {

      /**
       * The key for maximum attempts before abandoning retry ('maxAttempts').
       */
      public static String MAX_ATTEMPTS_KEY = "maxAttempts";

      /**
       * The default number of attempts before abandoning retry (10).
       */
      public static int DEFAULT_MAX_ATTEMPTS = 10;

      /**
       * The key for the delay interval ('delayIntervalMillis').
       */
      public static String DELAY_INTERVAL_MILLIS_KEY = "delayIntervalMillis";

      /**
       * The default delay interval (500).
       */
      public static int DEFAULT_DELAY_INTERVAL_MILLIS = 500;

      /**
       * Creates an uninitialized instance.
       */
      public ExponentialBackoff() {
      }

      /**
       * Creates exponential back-off with specified maximum attempts and
       * delay interval.
       * @param maxAttempts The maximum number of attempts.
       * @param delayIntervalMillis The delay interval in milliseconds.
       */
      public ExponentialBackoff(final int maxAttempts, final long delayIntervalMillis) {
         this.maxAttempts = maxAttempts;
         this.delayIntervalMillis = delayIntervalMillis;
      }

      @Override
      public long backoffMillis(final int numAttempts) {
         return numAttempts < maxAttempts ? ((long)Math.pow(2, numAttempts) * delayIntervalMillis) : -1L;
      }

      @Override
      public void init(Properties props) throws ServletException {
         this.maxAttempts = Integer.parseInt(props.getProperty(MAX_ATTEMPTS_KEY, Integer.toString(DEFAULT_MAX_ATTEMPTS)));
         this.delayIntervalMillis = Integer.parseInt(props.getProperty(DELAY_INTERVAL_MILLIS_KEY, Long.toString(DEFAULT_DELAY_INTERVAL_MILLIS)));
      }

      private int maxAttempts = 0;
      private long delayIntervalMillis = 0L;
   }

   /**
    * Specifies the number of milliseconds before retry is attempted.
    * @param numAttempts The current number of attempts.
    * @return The number of milliseconds. If less than zero, retry will not be attempted.
    */
   public long backoffMillis(final int numAttempts);

   /**
    * Initialize from properties.
    * @param props The properties.
    * @throws ServletException on initialization error.
    */
   public void init(Properties props) throws ServletException;
}
