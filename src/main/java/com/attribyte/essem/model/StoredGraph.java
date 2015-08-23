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

package com.attribyte.essem.model;

import com.attribyte.essem.model.graph.MetricKey;
import static com.attribyte.essem.util.Util.getStringField;
import static com.attribyte.essem.util.Util.getLongField;
import static com.attribyte.essem.util.Util.toValidIdentifier;

import com.attribyte.essem.util.Util;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.attribyte.util.URIEncoder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A stored graph.
 * <p>
 *    A stored graph is uniquely identified by index, user, metric key, range and downsampling function.
 *    An optional title is allowed.
 * </p>
 */
public class StoredGraph {

   /**
    * Builds immutable stored graphs.
    */
   public static class Builder {

      /**
       * Creates an empty builder.
       */
      public Builder() {
      }

      /**
       * Creates a builder starting with the fields of a previously created graph.
       * @param graph The previously created graph.
       */
      public Builder(final StoredGraph graph) {
         this.uid = graph.uid;
         this.index = graph.index;
         this.key = graph.key;
         this.range = graph.range;
         this.startTimestamp = graph.startTimestamp;
         this.endTimestamp = graph.endTimestamp;
         this.downsampleFn = graph.downsampleFn;
         this.rateUnit = graph.rateUnit;
         this.createTime = graph.createTime;
         this.title = graph.title;
         this.description = graph.description;
         this.xLabel = graph.xLabel;
         this.yLabel = graph.yLabel;
      }

      public Builder setUserId(final String uid) {
         this.uid = uid;
         return this;
      }

      public Builder setIndex(final String index) {
         this.index = index;
         return this;
      }

      public Builder setApplication(final String application) {
         this.application = application;
         return this;
      }

      public Builder setHost(final String host) {
         this.host = host;
         return this;
      }

      public Builder setInstance(final String instance) {
         this.instance = instance;
         return this;
      }

      public Builder setName(final String name) {
         this.name = name;
         return this;
      }

      public Builder setField(final String field) {
         this.field = field;
         return this;
      }

      public Builder setCreateTime(final Date createTime) {
         this.createTime = createTime;
         return this;
      }

      public Builder setTitle(final String title) {
         this.title = title;
         return this;
      }

      public Builder setDescription(final String description) {
         this.description = description;
         return this;
      }

      public Builder setXLabel(final String xLabel) {
         this.xLabel = xLabel;
         return this;
      }

      public Builder setYLabel(final String yLabel) {
         this.yLabel = yLabel;
         return this;
      }

      public Builder setRangeName(final String range) {
         this.range = range;
         return this;
      }

      public Builder setStartTimestamp(final long startTimestamp) {
         this.startTimestamp = startTimestamp;
         return this;
      }

      public Builder setEndTimestamp(final long endTimestamp) {
         this.endTimestamp = endTimestamp;
         return this;
      }

      public Builder setDownsampleFn(final String downsampleFn) {
         this.downsampleFn = downsampleFn;
         return this;
      }

      public Builder setRateUnit(final String rateUnit) {
         this.rateUnit = rateUnit;
         return this;
      }

      public Builder setMetricKey(final MetricKey key) {
         this.key = key;
         return this;
      }

      public Builder setTags(final Set<String> tags) {
         this.tags.addAll(tags);
         return this;
      }

      public Builder addTag(final String tag) {
         this.tags.add(tag);
         return this;
      }

      /**
       * Does this builder have an absolute range specified?
       * @return Is the range absolute?
       */
      public boolean hasAbsoluteRange() {
         return startTimestamp > 0 && endTimestamp > 0;
      }

      /**
       * Gets the current absolute range.
       * @return The range in milliseconds.
       */
      public long getRangeMillis() {
         return endTimestamp - startTimestamp;
      }

      /**
       * Builds a stored graph.
       * @return The graph.
       */
      public StoredGraph build() {
         MetricKey key = this.key != null ? this.key : new MetricKey(name, application, host, instance, field);
         return new StoredGraph(index, uid, key, range, startTimestamp, endTimestamp, downsampleFn, rateUnit,
                 title, description, xLabel, yLabel, tags, createTime);
      }

      private String uid;
      private String index;
      private String application;
      private String host;
      private String instance;
      private MetricKey key;
      private String name;
      private String field;
      private Date createTime;
      private String title;
      private String description;
      private String xLabel;
      private String yLabel;
      private String range;
      private long startTimestamp;
      private long endTimestamp;
      private String downsampleFn;
      private String rateUnit;
      private Set<String> tags = Sets.newHashSet();
   }

   /**
    * A comparator that sorts by create time (ascending).
    */
   public static final Comparator<StoredGraph> createTimeComparator = new Comparator<StoredGraph>() {
      @Override
      public int compare(final StoredGraph o1, final StoredGraph o2) {
         return o1.createTime.compareTo(o2.createTime);
      }
   };

   /**
    * Parses the components of a graph from request parameters.
    * @param request The request.
    * @param app The application name to use (overrides any parameter).
    * @return A parsed graph builder with all parameters set, or null if key is invalid.
    */
   public static StoredGraph.Builder parseGraph(final HttpServletRequest request, String app) {
      if(app == null) {
         app = request.getParameter("app");
         if(app == null) app = request.getParameter("application");
      }
      MetricKey key = MetricKey.parseKey(request, app);
      if(!key.isValidIdentifier()) {
         return null;
      }

      StoredGraph.Builder graphBuilder = new StoredGraph.Builder();
      graphBuilder.setMetricKey(key);
      graphBuilder.setRangeName(request.getParameter("range"));

      String startTimestampStr = Strings.nullToEmpty(request.getParameter("startTimestamp"));
      if(!startTimestampStr.isEmpty()) {
         graphBuilder.setStartTimestamp(Long.parseLong(startTimestampStr));
      }

      String endTimestampStr = Strings.nullToEmpty(request.getParameter("endTimestamp"));
      if(!endTimestampStr.isEmpty()) {
         graphBuilder.setEndTimestamp(Long.parseLong(endTimestampStr));
      }

      graphBuilder.setDownsampleFn(request.getParameter("downsampleFn"));
      graphBuilder.setRateUnit(request.getParameter("rateUnit"));
      graphBuilder.setTitle(toValidIdentifier(request.getParameter("title")));
      graphBuilder.setDescription(toValidIdentifier(request.getParameter("description")));
      graphBuilder.setXLabel(toValidIdentifier(request.getParameter("xLabel")));
      graphBuilder.setYLabel(toValidIdentifier(request.getParameter("yLabel")));

      String[] tags = request.getParameterValues("tag");
      if(tags != null && tags.length > 0) {
         graphBuilder.setTags(ImmutableSet.copyOf(tags));
      } else if(request.getParameter("tagString") != null) {
         for(String tag : Util.csvSplitter.split(request.getParameter("tagString"))) {
            graphBuilder.addTag(toValidIdentifier(tag));
         }
      }

      return graphBuilder;
   }

   /**
    * Hash function used to create the id.
    */
   private static final HashFunction hashFunction = Hashing.murmur3_128();

   /**
    * The JSON parser factory.
    */
   private static final JsonFactory parserFactory = new JsonFactory().enable(JsonParser.Feature.ALLOW_COMMENTS);

   /**
    * Creates a key.
    * @param index The associated index.
    * @param uid The user id.
    * @param key The key.
    * @param createTime The create time.
    */
   public StoredGraph(final String index, final String uid, final MetricKey key,
                      final String rangeName,
                      final long startTimestamp, final long endTimestamp,
                      final String downsampleFn,
                      final String rateUnit,
                      final String title, final String description,
                      final String xLabel, final String yLabel,
                      final Collection<String> tags,
                      final Date createTime) {
      this.index = Strings.nullToEmpty(index);
      this.uid = Strings.nullToEmpty(uid);
      this.key = key;
      this.createTime = createTime != null ? createTime : new Date();
      this.range = Strings.nullToEmpty(rangeName);
      this.downsampleFn = Strings.nullToEmpty(downsampleFn);
      this.rateUnit = Strings.nullToEmpty(rateUnit);
      this.startTimestamp = startTimestamp;
      this.endTimestamp = endTimestamp;

      final Hasher hasher = hashFunction.newHasher();
      hasher.putString(this.index)
              .putString(this.uid)
              .putString(key.application)
              .putString(key.host)
              .putString(key.instance)
              .putString(key.name)
              .putString(key.field)
              .putString(this.downsampleFn)
              .putString(this.rateUnit);

      if(startTimestamp > 0L && endTimestamp > 0L) {
         hasher.putLong(startTimestamp);
         hasher.putLong(endTimestamp);
      } else {
         hasher.putString(this.range);
      }

      this.id = hasher.hash().toString();
      this.sid = this.id.substring(0, 8);

      this.title = Strings.nullToEmpty(title);
      this.description = Strings.nullToEmpty(description);
      this.xLabel = Strings.nullToEmpty(xLabel);
      this.yLabel = Strings.nullToEmpty(yLabel);
      this.tags = tags != null ? ImmutableList.copyOf(cleanTags(tags)) : ImmutableList.<String>of();
      this.tagString = buildTagString();
   }

   /**
    * Cleans the list of tags.
    * - Trims
    * - Removes duplicates
    * - Removes tags that are invalid identifiers.
    * @param tags The colleciton of tags.
    * @return The clean set of tags.
    */
   private Set<String> cleanTags(final Collection<String> tags) {
      if(tags == null || tags.size() == 0) {
         return ImmutableSet.of();
      } else {
         Set<String> cleanedTags = Sets.newHashSetWithExpectedSize(tags.size());
         for(String tag : tags) {
            tag = tag.trim();
            cleanedTags.add(toValidIdentifier(tag));
         }
         return cleanedTags;
      }
   }

   /**
    * Gets this stored graph as a JSON object.
    * @return The JSON as a string.
    * @throws IOException on JSON generation error.
    */
   public final String getAsJSON() throws IOException {
      StringWriter writer = new StringWriter();
      JsonGenerator generator = parserFactory.createGenerator(writer);
      generateJSON(generator);
      generator.flush();
      generator.close();
      return writer.toString();
   }

   /**
    * Creates a stored graph from JSON.
    * @param obj The JSON object.
    * @return The stored graph.
    * @throws IOException on parse error.
    */
   public static final StoredGraph fromJSON(final JsonNode obj) throws IOException {
      String uid = getStringField(obj, "uid");
      String index = getStringField(obj, "index");
      String application = getStringField(obj, "application");
      String host = getStringField(obj, "host");
      String instance = getStringField(obj, "instance");
      String name = getStringField(obj, "name");
      String field = getStringField(obj, "field");
      MetricKey key = new MetricKey(name, application, host, instance, field);

      String downsampleFn = getStringField(obj, "downsampleFn");
      String rateUnit = getStringField(obj, "rateUnit");
      String range = getStringField(obj, "range");
      long startTimestamp = getLongField(obj, "startTimestamp", 0);
      long endTimestamp = getLongField(obj, "endTimestamp", 0);

      String title = getStringField(obj, "title");
      String description = getStringField(obj, "description");
      String xLabel = getStringField(obj, "xLabel");
      String yLabel = getStringField(obj, "yLabel");

      Set<String> tags = null;
      if(obj.has("tag")) {
         tags = Sets.newHashSetWithExpectedSize(8);
         JsonNode tagNode = obj.get("tag");
         if(tagNode.isArray()) {
            for(JsonNode arrNode : tagNode) {
               tags.add(arrNode.textValue());
            }
         } else if(tagNode.isTextual()) {
            tags.add(tagNode.textValue());
         }
      }

      String createdString = getStringField(obj, "created");
      Date createTime = Strings.isNullOrEmpty(createdString) ? new Date() : new Date(DateTime.fromStandardFormat(createdString));

      return new StoredGraph(index, uid, key, range, startTimestamp, endTimestamp, downsampleFn,
              rateUnit, title, description, xLabel, yLabel, tags, createTime);
   }

   /**
    * Generates JSON for this key.
    * @param generator The generator.
    * @throws java.io.IOException on generation error.
    */
   public void generateJSON(final JsonGenerator generator) throws IOException {
      generator.writeStartObject();
      generator.writeStringField("id", id);
      generator.writeStringField("uid", uid);
      generator.writeStringField("index", index);
      generator.writeStringField("application", key.application);
      generator.writeStringField("host", key.host);
      generator.writeStringField("instance", key.instance);
      generator.writeStringField("name", key.name);
      generator.writeStringField("field", key.field);
      generator.writeStringField("range", range);
      generator.writeNumberField("startTimestamp", startTimestamp);
      generator.writeNumberField("endTimestamp", endTimestamp);
      generator.writeStringField("downsampleFn", downsampleFn);
      if(rateUnit != null) generator.writeStringField("rateUnit", rateUnit);
      generator.writeStringField("title", title);
      generator.writeStringField("description", description);
      generator.writeStringField("xLabel", xLabel);
      generator.writeStringField("yLabel", yLabel);

      generator.writeArrayFieldStart("tag");
      for(String tag : tags) {
         generator.writeString(tag);
      }
      generator.writeEndArray();

      generator.writeStringField("created", DateTime.standardFormat(this.createTime));
      generator.writeEndObject();
   }

   @Override
   public boolean equals(final Object o) {
      if(o instanceof StoredGraph) {
         StoredGraph other = (StoredGraph)o;
         return id.equals(other.id);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return id.hashCode();
   }

   /**
    * Gets a comma-separated list of tags, if any.
    * @return The tag string.
    */
   private String buildTagString() {
      if(tags.size() > 0) {
         StringBuilder buf = new StringBuilder();
         Iterator<String> iter = tags.iterator();
         buf.append(iter.next());
         while(iter.hasNext()) {
            buf.append(", ").append(iter.next());
         }
         return buf.toString();
      } else {
         return "";
      }
   }

   /**
    * Gets the tags appropriately encoded for a query string.
    * @return The list of tags.
    */
   public List<String> getTagsForQuery() {
      if(tags.size() > 0) {
         List<String> escapedTags = Lists.newArrayListWithCapacity(tags.size());
         for(String tag : tags) {
            escapedTags.add(URIEncoder.encodeQueryString(tag));
         }
         return escapedTags;
      } else {
         return ImmutableList.of();
      }
   }

   /**
    * Does this graph show a rate field?
    * @return Does the graph show a rate?
    */
   public boolean isRate() {
      return key.field.endsWith("Rate");
   }

   /**
    * The index.
    */
   public final String index;

   /**
    * The user id.
    */
   public final String uid;

   /**
    * The unique key id.
    */
   public final String id;

   /**
    * A "short" id that should be OK most of the time.
    */
   public final String sid;

   /**
    * The metric key.
    */
   public final MetricKey key;

   /**
    * The create time.
    */
   public final Date createTime;

   /**
    * An optional title.
    */
   public final String title;

   /**
    * An optional description.
    */
   public final String description;

   /**
    * An optional x-axis label.
    */
   public final String xLabel;

   /**
    * An optional y-axis label.
    */
   public final String yLabel;

   /**
    * An optional named-range.
    */
   public final String range;

   /**
    * An optional start timestamp.
    */
   public final long startTimestamp;

   /**
    * An optional end timestamp.
    */
   public final long endTimestamp;

   /**
    * An optional downsample function.
    */
   public final String downsampleFn;

   /**
    * An optional unit for rates.
    */
   public final String rateUnit;

   /**
    * Tags added to the graph.
    */
   public final ImmutableList<String> tags;

   /**
    * A comma-separated list of tags.
    */
   public final String tagString;

   /**
    * Gets a text description of the rate unit.
    * @return The text.
    */
   public final String getRateUnitText() {
      if(rateUnit != null) {
         switch(rateUnit.toLowerCase()) {
            case "persecond" : return "/s";
            case "perminute" : return "/min";
            case "perhour" : return "/hour";
            default : return "";
         }
      } else {
         return "";
      }
   }

}