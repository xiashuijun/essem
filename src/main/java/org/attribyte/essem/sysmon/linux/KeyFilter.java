/*
 * Copyright 2015 Attribyte, LLC
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

package org.attribyte.essem.sysmon.linux;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public interface KeyFilter {

   /**
    * Should the name be accepted?
    * @return The name.
    */
   public boolean accept(final String str);

   /**
    * A filter that accepts only names in a set.
    */
   public static class AcceptSet implements KeyFilter {

      /**
       * Creates the filter.
       * @param allowed The allowed names.
       */
      public AcceptSet(final Set<String> allowed) {
         this.allowed = ImmutableSet.copyOf(allowed);
      }

      @Override
      public boolean accept(final String str) {
         return allowed.contains(str);
      }

      private final ImmutableSet<String> allowed;
   }

   /**
    * Accepts all keys.
    */
   public static KeyFilter acceptAll = new KeyFilter() {
      @Override
      public boolean accept(final String str) {
         return true;
      }
   };

   /**
    * Accepts no keys.
    */
   public static KeyFilter acceptNone = new KeyFilter() {
      @Override
      public boolean accept(final String str) {
         return false;
      }
   };

}
