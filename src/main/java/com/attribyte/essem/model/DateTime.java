package com.attribyte.essem.model;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Date;

public class DateTime {

   /**
    * Format as yyyyMMdd'T'HHmmssZ
    */
   public static final DateTimeFormatter standardFormatter = ISODateTimeFormat.basicDateTime().withZoneUTC();

   /**
    * Gets the date in standard format.
    * @param date The date.
    * @return The date in standard format.
    */
   public static String standardFormat(final Date date) {
      return standardFormatter.print(date.getTime());
   }

   /**
    * Gets the timestamp in standard format.
    * @param timestamp The timestamp.
    * @return The date in standard format.
    */
   public static String standardFormat(final long timestamp) {
      return standardFormatter.print(timestamp);
   }

   /**
    * Parse from standard format to a timestamp.
    * @param str The formatted string.
    * @return The timestamp.
    */
   public static long fromStandardFormat(final String str) {
      return standardFormatter.parseMillis(str);
   }
}