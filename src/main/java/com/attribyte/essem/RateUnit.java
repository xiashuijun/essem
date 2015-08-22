package com.attribyte.essem;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

/**
 * Rate unit for conversion.
 */
public enum RateUnit {

   /**
    * Per second (default).
    */
   PER_SECOND(1.0),

   /**
    * Per minute.
    */
   PER_MINUTE(60.0),

   /**
    * Per hour.
    */
   PER_HOUR(3600.0);

   RateUnit(final double mult) {
      this.mult = mult;
   }

   /**
    * The multiplier for conversion from the default unit.
    */
   public final double mult;

   /**
    * Map strings to rate units.
    */
   private static ImmutableMap<String, RateUnit> unitMap = ImmutableMap.of(
           "", PER_SECOND,
           "persecond", PER_SECOND,
           "perminute", PER_MINUTE,
           "perhour", PER_HOUR
   );

   /**
    * Gets a rate unit from a string.
    * @param str The string.
    * @return The rate unit or the default if string is not recognized.
    */
   public static RateUnit fromString(final String str) {
      RateUnit unit = unitMap.get(Strings.nullToEmpty(str).trim().toLowerCase());
      return unit != null ? unit : ResponseGenerator.RAW_RATE_UNIT;
   }
}
