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

package com.attribyte.essem.model;

/**
 * Sort order.
 */
public enum Sort {

   /**
    * Sort in ascending order.
    */
   ASC,

   /**
    * Sort in descending order.
    */
   DESC;

   /**
    * Gets a sort from a string value.
    * @param str The string value.
    * @param defaultSort The default sort if value is not defined or invalid.
    * @return The sort.
    */
   public static final Sort fromString(final String str, final Sort defaultSort) {
      if(str == null) {
         return defaultSort;
      } else if(str.equalsIgnoreCase("asc")) {
         return ASC;
      } else if(str.equalsIgnoreCase("desc")) {
         return DESC;
      } else {
         return defaultSort;
      }
   }
}