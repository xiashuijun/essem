package com.attribyte.essem.model;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;
import java.io.StringWriter;

/**
 * A single-metric graph.
 */
public class MetricGraph {

   /**
    * Graph labels.
    */
   public static class Labels {

      /**
       * Create labels.
       * @param x The x-axis label.
       * @param y The y-axis label.
       */
      public Labels(final String x, final String y) {
         this.x = x;
         this.y = y;
      }

      /**
       * The x-axis label.
       */
      public final String x;

      /**
       * The y-axis labels.
       */
      public final String y;
   }

   /**
    * The default labels for timers.
    */
   public static final Labels DEFAULT_TIMER_LABLES = new Labels("Time", "Millis");

   /**
    * The default labels for meters.
    */
   public static final Labels DEFAULT_METER_LABLES = new Labels("Time", "Per Second");

   /**
    * The default labels for all other metrics.
    */
   public static final Labels DEFAULT_OTHER_LABLES = new Labels("Time", "");

   /**
    * Creates a graph from a prototype with a metric.
    * @param proto The prototype.
    * @param metric The metric.
    */
   public MetricGraph(MetricGraph proto, final Metric metric) {
      this.field = proto.field;
      this.title = proto.title;
      this.shortTitle = proto.shortTitle;
      this.labels = proto.labels;
      this.active = proto.active;
      this.metric = metric;
   }

   /**
    * Creates a metric graph from a prototype with a metric and active status specified.
    * @param proto The prototype.
    * @param metric The metric.
    * @param active The active status.
    */
   public MetricGraph(MetricGraph proto, final Metric metric, final boolean active) {
      this.field = proto.field;
      this.title = proto.title;
      this.shortTitle = proto.shortTitle;
      this.labels = proto.labels;
      this.active = active;
      this.metric = metric;
   }

   /**
    * Creates a metric graph prototype.
    * @param field The field name.
    * @param title The title.
    * @param labels The labels.
    * @param active The active status.
    */
   public MetricGraph(final String field, final String title, final String shortTitle,
                      final Labels labels, final boolean active) {
      this.metric = null;
      this.field = field;
      this.shortTitle = shortTitle;
      this.title = title;
      this.labels = labels;
      this.active = active;
   }

   /**
    * Gets the default labels for a metric type.
    * @param type The metric type.
    * @return The default labels.
    */
   public final Labels getDefaultLabels(final Metric.Type type) {
      switch(type) {
         case TIMER: return DEFAULT_TIMER_LABLES;
         case METER: return DEFAULT_METER_LABLES;
         default: return DEFAULT_OTHER_LABLES;
      }
   }

   static final JsonFactory parserFactory = new JsonFactory().enable(JsonParser.Feature.ALLOW_COMMENTS);

   /**
    * Gets the graph as a JSON string.
    * @return The JSON string.
    * @throws IOException on generation error.
    */
   public final String getAsJSON() throws IOException {

      StringWriter writer = new StringWriter();
      JsonGenerator generator = parserFactory.createGenerator(writer);

      generator.writeStartObject();
      if(metric != null) {
         generator.writeStringField("name", metric.name);
         generator.writeStringField("name_hash", metric.nameHash);
         generator.writeStringField("mtype", metric.type.toString());
      }
      generator.writeStringField("field", field != null ? field : "");
      generator.writeStringField("title", title != null ? title : "");
      generator.writeStringField("shortTitle", shortTitle != null ? shortTitle : "");
      if(labels != null) {
         generator.writeStringField("x_label", labels.x != null ? labels.x : "");
         generator.writeStringField("y_label", labels.x != null ? labels.y : "");
      } else {
         generator.writeStringField("x_label", "");
         generator.writeStringField("y_label", "");
      }
      generator.writeEndObject();
      generator.flush();
      generator.close();
      return writer.toString();
   }


   public final Metric metric;
   public final String field;
   public final String shortTitle;
   public final String title;
   public final Labels labels;
   public final boolean active;
}
