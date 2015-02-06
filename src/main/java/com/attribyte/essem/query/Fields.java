package com.attribyte.essem.query;

/**
 * Contants that define the name of fields stored in ES.
 */
public class Fields {

   public static final String TIMESTAMP_FIELD = "ts";

   public static final String GAUGE_TYPE = "gauge";
   public static final String COUNTER_TYPE = "counter";
   public static final String METER_TYPE = "meter";
   public static final String HISTOGRAM_TYPE = "histogram";
   public static final String TIMER_TYPE = "timer";

   public static final String APPLICATION_FIELD = "application";
   public static final String HOST_FIELD = "host";
   public static final String INSTANCE_FIELD = "instance";
   public static final String NAME_FIELD = "name";

   public static final String COUNT_FIELD = "count";

   public static final String VALUE_FIELD = "value";
   public static final String COMMENT_FIELD = "comment";


   public static final String ONE_MINUTE_RATE_FIELD = "m1Rate";
   public static final String FIVE_MINUTE_RATE_FIELD = "m5Rate";
   public static final String FIFTEEN_MINUTE_RATE_FIELD = "m15Rate";
   public static final String MEAN_RATE_FIELD = "meanRate";

   public static final String MAX_FIELD = "max";
   public static final String MIN_FIELD = "min";
   public static final String MEAN_FIELD = "mean";
   public static final String P50_FIELD = "p50";
   public static final String P75_FIELD = "p75";
   public static final String P95_FIELD = "p95";
   public static final String P98_FIELD = "p98";
   public static final String P99_FIELD = "p99";
   public static final String P999_FIELD = "p999";
   public static final String STD_FIELD = "std";

   public static final String TYPE_FIELD = "_type";

}
