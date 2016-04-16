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

package com.attribyte.essem.es;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

public interface BucketAggregation extends Aggregation {

   /**
    * Standard bucket orders.
    */
   public enum Order {
      /**
       * By count, descending.
       */
      DOC_COUNT_DESC,

      /**
       * By count, ascending.
       */
      DOC_COUNT_ASC,

      /**
       * By term ascending.
       */
      TERM_ASC,

      /**
       * By term, descending.
       */
      TERM_DESC,

      /**
       * By term ascending.
       */
      KEY_ASC,

      /**
       * By term, descending.
       */
      KEY_DESC;

      /**
       * Gets the order from a string value.
       * @param str The string.
       * @return The order or <code>DOC_COUNT_DESC</code> if string is not supplied or invalid.
       */
      public static final Order fromString(final String str) {
         return fromString(str, Order.DOC_COUNT_DESC);
      }

      /**
       * Gets the order from a string value.
       * @param str The string.
       * @param defaultValue The default value.
       * @return The order or the specified default if string is not supplied or invalid.
       */
      public static final Order fromString(final String str, final Order defaultValue) {
         Order order = orderMap.get(Strings.nullToEmpty(str).trim().toLowerCase());
         return order != null ? order : defaultValue;
      }

      private static final ImmutableMap<String, Order> orderMap =
              ImmutableMap.<String, Order>builder()
                      .put("count:desc", Order.DOC_COUNT_DESC)
                      .put("count:asc", Order.DOC_COUNT_ASC)
                      .put("term:asc", Order.TERM_ASC)
                      .put("term:desc", Order.TERM_DESC)
                      .put("key:asc", Order.KEY_ASC)
                      .put("key:desc", Order.KEY_DESC).build();
   }
}
