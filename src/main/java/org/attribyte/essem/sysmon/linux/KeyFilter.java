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
