package com.attribyte.essem;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.attribyte.essem.Util.parserFactory;

/**
 * Creates a report from JSON as output by:
 * https://github.com/dropwizard/metrics/blob/master/metrics-json/src/main/java/com/codahale/metrics/json/MetricsModule.java
 */
public class JSONReport {

   public static void main(String[] args) throws Exception {
      byte[] reportBytes = Files.toByteArray(new File("/home/matt/metrics.json"));
      ReportProtos.EssemReport report = parseFrom(reportBytes);
      System.out.println(report.toString());
   }

   /**
    * Parses a report from bytes.
    * @param reportBytes The report bytes.
    * @return The report.
    * @throws IOException on parse error.
    */
   public static ReportProtos.EssemReport parseFrom(final byte[] reportBytes) throws IOException {
      return parseBuilderFrom(reportBytes).build();
   }

   /**
    * Parses a report builder from bytes.
    * @param reportBytes The report bytes.
    * @return The report builder.
    * @throws IOException on parse error.
    */
   public static ReportProtos.EssemReport.Builder parseBuilderFrom(final byte[] reportBytes) throws IOException {

      ReportProtos.EssemReport.Builder builder = ReportProtos.EssemReport.newBuilder();
      builder.setRateUnit(ReportProtos.EssemReport.TimeUnit.SECONDS);
      builder.setDurationUnit(ReportProtos.EssemReport.TimeUnit.MILLIS);

      JsonParser parser = parserFactory.createParser(reportBytes);
      if(parser.nextToken() != JsonToken.START_OBJECT) {
         throw new IOException("Invalid report: Expecting an object");
      }

      while(parser.nextToken() != JsonToken.END_OBJECT) {
         String name = parser.getCurrentName();
         switch(name) {
            case "gauges":
               parseGauges(parser, builder);
               break;
            case "counters":
               parseCounters(parser, builder);
               break;
            case "histograms":
               parseHistograms(parser, builder);
               break;
            case "meters":
               parseMeters(parser, builder);
               break;
            case "timers":
               parseTimers(parser, builder);
               break;
         }
      }

      return builder;
   }

   /**
    * Determine if the token may be converted to a string.
    * @param token The token.
    * @return Can the token be converted to a string?
    */
   private static boolean isConvertibleToString(final JsonToken token) {
      switch(token) {
         case VALUE_STRING:
         case VALUE_NUMBER_INT:
         case VALUE_NUMBER_FLOAT:
         case VALUE_TRUE:
         case VALUE_FALSE:
            return true;
         default:
            return false;
      }
   }

   /**
    * Maps possible values of 'units', 'rate_units', and 'duration_units' to
    * a time unit.
    */
   private static ImmutableMap<String, TimeUnit> unitMap =
           ImmutableMap.<String, TimeUnit>builder()
                   .put("events/nanosecond", TimeUnit.NANOSECONDS)
                   .put("events/microsecond", TimeUnit.MICROSECONDS)
                   .put("events/millisecond", TimeUnit.MILLISECONDS)
                   .put("events/second", TimeUnit.SECONDS)
                   .put("events/minute", TimeUnit.MINUTES)
                   .put("events/hour", TimeUnit.HOURS)
                   .put("events/day", TimeUnit.DAYS)
                   .put("calls/nanosecond", TimeUnit.NANOSECONDS)
                   .put("calls/microsecond", TimeUnit.MICROSECONDS)
                   .put("calls/millisecond", TimeUnit.MILLISECONDS)
                   .put("calls/second", TimeUnit.SECONDS)
                   .put("calls/minute", TimeUnit.MINUTES)
                   .put("calls/hour", TimeUnit.HOURS)
                   .put("calls/day", TimeUnit.DAYS)
                   .put("nanosecond", TimeUnit.NANOSECONDS)
                   .put("microsecond", TimeUnit.MICROSECONDS)
                   .put("millisecond", TimeUnit.MILLISECONDS)
                   .put("second", TimeUnit.SECONDS)
                   .put("minute", TimeUnit.MINUTES)
                   .put("hour", TimeUnit.HOURS)
                   .put("day", TimeUnit.DAYS)
                   .put("nanoseconds", TimeUnit.NANOSECONDS)
                   .put("microseconds", TimeUnit.MICROSECONDS)
                   .put("milliseconds", TimeUnit.MILLISECONDS)
                   .put("seconds", TimeUnit.SECONDS)
                   .put("minutes", TimeUnit.MINUTES)
                   .put("hours", TimeUnit.HOURS)
                   .put("days", TimeUnit.DAYS)
                   .build();


   /**
    * Convert rates to the internal units (seconds).
    * @param rate The rate.
    * @param reportUnit The reported unit.
    * @return The converted rate.
    */
   private static double rateToInternal(final double rate, final TimeUnit reportUnit) {
      if(reportUnit == TimeUnit.SECONDS) {
         return rate;
      } else {
         return 1.0 / reportUnit.toNanos(1L) * (double)TimeUnit.SECONDS.toNanos(1L) * rate;
      }
   }

   /**
    * Convert durations to the internal units (milliseconds).
    * @param duration The duration.
    * @param reportUnit The reported unit.
    * @return The converted duration.
    */
   private static double durationToInternal(final double duration, final TimeUnit reportUnit) {
      if(reportUnit == TimeUnit.MILLISECONDS) {
         return duration;
      } else {
         return duration * reportUnit.toNanos(1L) / (double)TimeUnit.MILLISECONDS.toNanos(1L);
      }
   }

   private static void parseGauges(final JsonParser parser,
                                   final ReportProtos.EssemReport.Builder builder) throws IOException {

      if(parser.nextToken() != JsonToken.START_OBJECT) {
         throw new IOException("Invalid report: Expecting 'gauges' value to be an object");
      }

      while(parser.nextToken() != JsonToken.END_OBJECT) {
         String gaugeName = parser.getCurrentName();
         if(parser.nextToken() != JsonToken.START_OBJECT) {
            throw new IOException("Invalid report: Expecting an object for '" + gaugeName + "'");
         }

         while(parser.nextToken() != JsonToken.END_OBJECT) {
            String propertyName = parser.getCurrentName();
            JsonToken tok = parser.nextToken();
            if(tok.isStructStart()) {
               throw new IOException("Unexpected object while processing '" + gaugeName + "'");
            }
            switch(propertyName) {
               case "value":
                  if(tok.isNumeric()) {
                     builder.addGaugeBuilder().setName(gaugeName).setValue(parser.getValueAsDouble());
                  } else if(isConvertibleToString(tok)) {
                     builder.addGaugeBuilder().setName(gaugeName).setComment(parser.getValueAsString());
                  }
                  break;
            }
         }
      }
   }

   private static void parseCounters(final JsonParser parser,
                                     final ReportProtos.EssemReport.Builder builder) throws IOException {

      if(parser.nextToken() != JsonToken.START_OBJECT) {
         throw new IOException("Invalid report: Expecting 'counters' value to be an object");
      }

      while(parser.nextToken() != JsonToken.END_OBJECT) {
         String counterName = parser.getCurrentName();
         if(parser.nextToken() != JsonToken.START_OBJECT) {
            throw new IOException("Invalid report: Expecting an object for '" + counterName + "'");
         }

         while(parser.nextToken() != JsonToken.END_OBJECT) {
            String propertyName = parser.getCurrentName();
            switch(propertyName) {
               case "count":
                  switch(parser.nextToken()) {
                     case VALUE_NUMBER_INT:
                        builder.addCounterBuilder().setName(counterName).setCount(parser.getLongValue());
                        break;
                     case VALUE_EMBEDDED_OBJECT:
                        throw new IOException("Unexpected object while processing '" + counterName + "'");
                  }
                  break;
            }
         }
      }

   }

   private static void parseHistograms(final JsonParser parser,
                                       final ReportProtos.EssemReport.Builder builder) throws IOException {

      if(parser.nextToken() != JsonToken.START_OBJECT) {
         throw new IOException("Invalid report: Expecting 'histograms' value to be an object");
      }

      while(parser.nextToken() != JsonToken.END_OBJECT) {
         String histogramName = parser.getCurrentName();
         if(parser.nextToken() != JsonToken.START_OBJECT) {
            throw new IOException("Invalid report: Expecting an object for '" + histogramName + "'");
         }

         ReportProtos.EssemReport.Histogram.Builder histogram = builder.addHistogramBuilder().setName(histogramName);

         while(parser.nextToken() != JsonToken.END_OBJECT) {
            String propertyName = parser.getCurrentName();
            JsonToken tok = parser.nextToken();
            if(tok.isNumeric()) {
               switch(propertyName) {
                  case "count":
                     switch(tok) {
                        case VALUE_NUMBER_INT:
                        case VALUE_NUMBER_FLOAT:
                           histogram.setCount(parser.getLongValue());
                           break;
                     }
                     break;
                  case "max":
                     histogram.setMax(parser.getDoubleValue());
                     break;
                  case "mean":
                     histogram.setMean(parser.getDoubleValue());
                     break;
                  case "min":
                     histogram.setMin(parser.getDoubleValue());
                     break;
                  case "p50":
                     histogram.setMedian(parser.getDoubleValue());
                     break;
                  case "p75":
                     histogram.setPercentile75(parser.getDoubleValue());
                     break;
                  case "p95":
                     histogram.setPercentile95(parser.getDoubleValue());
                     break;
                  case "p98":
                     histogram.setPercentile98(parser.getDoubleValue());
                     break;
                  case "p99":
                     histogram.setPercentile99(parser.getDoubleValue());
                     break;
                  case "p999":
                     histogram.setPercentile999(parser.getDoubleValue());
                     break;
                  case "stddev":
                     histogram.setStd(parser.getDoubleValue());
                     break;
               }
            } else if(tok.isStructStart()) {
               throw new IOException("Unexpected object while processing '" + histogramName + "'");
            }
         }
      }
   }

   private static void parseMeters(final JsonParser parser,
                                   final ReportProtos.EssemReport.Builder builder) throws IOException {

      if(parser.nextToken() != JsonToken.START_OBJECT) {
         throw new IOException("Invalid report: Expecting 'meters' value to be an object");
      }

      while(parser.nextToken() != JsonToken.END_OBJECT) {
         String meterName = parser.getCurrentName();
         if(parser.nextToken() != JsonToken.START_OBJECT) {
            throw new IOException("Invalid report: Expecting an object for '" + meterName + "'");
         }

         ReportProtos.EssemReport.Meter.Builder meter = builder.addMeterBuilder().setName(meterName);

         String units = null;

         while(parser.nextToken() != JsonToken.END_OBJECT) {
            String propertyName = parser.getCurrentName();
            JsonToken tok = parser.nextToken();
            if(tok.isNumeric()) {
               switch(propertyName) {
                  case "m1_rate":
                     meter.setOneMinuteRate(parser.getDoubleValue());
                     break;
                  case "m5_rate":
                     meter.setFiveMinuteRate(parser.getDoubleValue());
                     break;
                  case "m15_rate":
                     meter.setFifteenMinuteRate(parser.getDoubleValue());
                     break;
                  case "mean_rate":
                     meter.setMeanRate(parser.getDoubleValue());
                     break;
                  case "count":
                     switch(tok) {
                        case VALUE_NUMBER_INT:
                        case VALUE_NUMBER_FLOAT:
                           meter.setCount(parser.getLongValue());
                           break;
                     }
                     break;
               }
            } else if(propertyName.equals("units")) {
               units = parser.getValueAsString();
            } else if(tok.isStructStart()) {
               throw new IOException("Unexpected object while processing '" + meterName + "'");
            }
         }

         if(units != null) {
            TimeUnit rateUnit = unitMap.get(units);
            if(rateUnit != null && rateUnit != TimeUnit.SECONDS) {
               meter.setOneMinuteRate(rateToInternal(meter.getOneMinuteRate(), rateUnit));
               meter.setFiveMinuteRate(rateToInternal(meter.getFiveMinuteRate(), rateUnit));
               meter.setFifteenMinuteRate(rateToInternal(meter.getFifteenMinuteRate(), rateUnit));
               meter.setMeanRate(rateToInternal(meter.getMeanRate(), rateUnit));
            } else if(rateUnit == null) {
               throw new IOException("Invalid units: '" + units + "'");
            }
         }
      }
   }

   private static void parseTimers(final JsonParser parser,
                                   final ReportProtos.EssemReport.Builder builder) throws IOException {

      if(parser.nextToken() != JsonToken.START_OBJECT) {
         throw new IOException("Invalid report: Expecting 'timers' value to be an object");
      }

      while(parser.nextToken() != JsonToken.END_OBJECT) {
         String timerName = parser.getCurrentName();
         if(parser.nextToken() != JsonToken.START_OBJECT) {
            throw new IOException("Invalid report: Expecting an object for '" + timerName + "'");
         }

         ReportProtos.EssemReport.Timer.Builder timer = builder.addTimerBuilder().setName(timerName);

         String rateUnitStr = null;
         String durationUnitStr = null;

         while(parser.nextToken() != JsonToken.END_OBJECT) {
            String propertyName = parser.getCurrentName();
            JsonToken tok = parser.nextToken();
            if(tok.isNumeric()) {
               switch(propertyName) {
                  case "m1_rate":
                     timer.setOneMinuteRate(parser.getDoubleValue());
                     break;
                  case "m5_rate":
                     timer.setFiveMinuteRate(parser.getDoubleValue());
                     break;
                  case "m15_rate":
                     timer.setFifteenMinuteRate(parser.getDoubleValue());
                     break;
                  case "mean_rate":
                     timer.setMeanRate(parser.getDoubleValue());
                     break;
                  case "count":
                     switch(tok) {
                        case VALUE_NUMBER_INT:
                        case VALUE_NUMBER_FLOAT:
                           timer.setCount(parser.getLongValue());
                           break;
                     }
                     break;
                  case "max":
                     timer.setMax(parser.getDoubleValue());
                     break;
                  case "mean":
                     timer.setMean(parser.getDoubleValue());
                     break;
                  case "min":
                     timer.setMin(parser.getDoubleValue());
                     break;
                  case "p50":
                     timer.setMedian(parser.getDoubleValue());
                     break;
                  case "p75":
                     timer.setPercentile75(parser.getDoubleValue());
                     break;
                  case "p95":
                     timer.setPercentile95(parser.getDoubleValue());
                     break;
                  case "p98":
                     timer.setPercentile98(parser.getDoubleValue());
                     break;
                  case "p99":
                     timer.setPercentile99(parser.getDoubleValue());
                     break;
                  case "p999":
                     timer.setPercentile999(parser.getDoubleValue());
                     break;
                  case "stddev":
                     timer.setStd(parser.getDoubleValue());
                     break;
               }
            } else if(propertyName.equals("rate_units")) {
               rateUnitStr = parser.getValueAsString();
            } else if(propertyName.equals("duration_units")) {
               durationUnitStr = parser.getValueAsString();
            } else if(tok.isStructStart()) {
               throw new IOException("Unexpected object while processing '" + timerName + "'");
            }
         }

         if(rateUnitStr != null) {
            TimeUnit rateUnit = unitMap.get(rateUnitStr);
            if(rateUnit != null && rateUnit != TimeUnit.SECONDS) {
               timer.setOneMinuteRate(rateToInternal(timer.getOneMinuteRate(), rateUnit));
               timer.setFiveMinuteRate(rateToInternal(timer.getFiveMinuteRate(), rateUnit));
               timer.setFifteenMinuteRate(rateToInternal(timer.getFifteenMinuteRate(), rateUnit));
               timer.setMeanRate(rateToInternal(timer.getMeanRate(), rateUnit));
            } else if(rateUnit == null) {
               throw new IOException("Invalid units: '" + rateUnitStr + "'");
            }
         }

         if(durationUnitStr != null) {
            TimeUnit durationUnit = unitMap.get(durationUnitStr);
            if(durationUnit != null && durationUnit != TimeUnit.MILLISECONDS) {
               timer.setMax(durationToInternal(timer.getMax(), durationUnit));
               timer.setMean(durationToInternal(timer.getMean(), durationUnit));
               timer.setMin(durationToInternal(timer.getMin(), durationUnit));
               timer.setMedian(durationToInternal(timer.getMedian(), durationUnit));
               timer.setPercentile75(durationToInternal(timer.getPercentile75(), durationUnit));
               timer.setPercentile95(durationToInternal(timer.getPercentile95(), durationUnit));
               timer.setPercentile98(durationToInternal(timer.getPercentile98(), durationUnit));
               timer.setPercentile99(durationToInternal(timer.getPercentile99(), durationUnit));
               timer.setPercentile999(durationToInternal(timer.getPercentile999(), durationUnit));
            } else if(durationUnit == null) {
               throw new IOException("Invalid units: '" + durationUnitStr + "'");
            }
         }
      }
   }
}
