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

package com.attribyte.essem;

import com.attribyte.essem.es.SearchRequest;
import com.attribyte.essem.model.graph.MetricKey;
import com.attribyte.essem.query.GraphQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.attribyte.api.http.Response;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.attribyte.essem.Util.getStringField;
import static com.attribyte.essem.Util.getFieldNode;
import static com.attribyte.essem.Util.graphIgnoreProperties;

public class DefaultResponseGenerator extends ESResponseGenerator {

   @Override
   public boolean generateGraph(GraphQuery graphQuery,
                                Response esResponse,
                                EnumSet<Option> options,
                                HttpServletResponse response) throws IOException {
      ObjectNode jsonObject = mapper.readTree(parserFactory.createParser(esResponse.getBody().toByteArray()));

      ObjectNode responseObject = JsonNodeFactory.instance.objectNode();
      List<String> fields = ImmutableList.copyOf(graphQuery.searchRequest.fields);
      ObjectNode targetMeta = JsonNodeFactory.instance.objectNode();
      ArrayNode targetGraphs = responseObject.putArray("graphs");

      if(graphQuery.range.expression != null) {
         targetMeta.put("range", graphQuery.range.expression);
      }

      if(graphQuery.range.startTimestamp > 0L) {
         targetMeta.put("rangeStartTimestamp", graphQuery.range.startTimestamp);
      }

      if(graphQuery.range.endTimestamp > 0L) {
         targetMeta.put("rangeEndTimestamp", graphQuery.range.endTimestamp);
      }

      JsonNode aggregations = jsonObject.get("aggregations");
      if(aggregations != null && aggregations.isObject()) {
         ArrayNode metaFields = targetMeta.putArray("fields");
         metaFields.add("timestamp");
         metaFields.add("samples");
         for(String field : fields) {
            metaFields.add(field);
         }

         if(graphQuery.downsampleInterval != null) {
            targetMeta.put("downsampledTo", graphQuery.downsampleInterval);
         }

         if(graphQuery.downsampleFunction != null) {
            targetMeta.put("downsampledWith", graphQuery.downsampleFunction);
         }

         String error = parseGraphAggregation(aggregations, fields, targetMeta, targetGraphs);
         if(error == null) {
            generateGraph(responseObject, response);
            return true;
         } else {
            response.sendError(500, error);
            return false;
         }
      } else {
         ArrayNode metaFields = targetMeta.putArray("fields");
         metaFields.add("timestamp");
         metaFields.add("samples");
         for(String field : fields) {
            if(!graphIgnoreProperties.contains(field)) {
               metaFields.add(field);
            }
         }
         parseGraph(jsonObject, fields, targetMeta, targetGraphs);
         generateGraph(responseObject, response);
         return true;
      }
   }

   private void generateGraph(JsonNode responseObject,
                              HttpServletResponse response) throws IOException {
      response.setContentType(JSON_CONTENT_TYPE_HEADER);
      response.setStatus(HttpServletResponse.SC_OK);
      response.getOutputStream().write(responseObject.toString().getBytes("UTF-8"));
      response.getOutputStream().flush();
   }


   protected void parseGraph(JsonNode sourceParent, List<String> fields,
                             ObjectNode targetMeta, ArrayNode targetGraph) {

      DateTimeFormatter parser = ISODateTimeFormat.basicDateTime();

      Map<MetricKey, ArrayNode> outputGraphs = Maps.newHashMapWithExpectedSize(4);

      JsonNode hitsObj = sourceParent.get("hits");
      if(hitsObj != null) {
         JsonNode hitsArr = hitsObj.get("hits");
         if(hitsArr != null) {

            for(JsonNode hitObj : hitsArr) {
               JsonNode fieldsObj = hitObj.get("fields");
               if(fieldsObj != null) {
                  MetricKey key = new MetricKey(
                          getStringField(fieldsObj, "name"),
                          getStringField(fieldsObj, "application"),
                          getStringField(fieldsObj, "host"),
                          getStringField(fieldsObj, "instance")
                  );

                  ArrayNode samplesArr = outputGraphs.get(key);
                  if(samplesArr == null) {
                     ObjectNode graphObj = targetGraph.addObject();
                     addMeta(key, graphObj, targetMeta);
                     samplesArr = graphObj.putArray("samples");
                     outputGraphs.put(key, samplesArr);
                  }

                  ArrayNode sampleArr = samplesArr.addArray();

                  DateTime timestamp = parser.parseDateTime(getStringField(fieldsObj, "ts"));
                  sampleArr.add(timestamp.getMillis());
                  sampleArr.add(1); //Samples..

                  for(String field : fields) {
                     if(!graphIgnoreProperties.contains(field)) {
                        JsonNode fieldNode = getFieldNode(fieldsObj, field);
                        if(fieldNode != null) {
                           sampleArr.add(fieldNode);
                        } else {
                           sampleArr.addNull();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   protected String parseGraphAggregation(JsonNode sourceParent, List<String> fields,
                                          ObjectNode targetMeta, ArrayNode targetGraph) {

      Iterator<Map.Entry<String, JsonNode>> iter = sourceParent.fields();
      Map.Entry<String, JsonNode> aggregation = null;

      while(iter.hasNext()) {
         aggregation = iter.next();
         if(aggregation.getValue().isObject()) {
            break;
         } else {
            aggregation = null;
         }
      }

      if(aggregation == null) {
         return "Aggregation is invalid";
      }

      String bucketName = aggregation.getKey();

      JsonNode obj = aggregation.getValue();
      JsonNode bucketsObj = obj.get("buckets");
      if(bucketsObj == null || !bucketsObj.isArray()) {
         return "Aggregation is invalid";
      }

      if(keyComponents.contains(bucketName)) {
         for(final JsonNode bucketObj : bucketsObj) {
            if(!bucketObj.isObject()) {
               return "Aggregation is invalid";
            }

            JsonNode keyNode = bucketObj.get(KEY_NODE_KEY);
            if(keyNode == null) {
               return "Aggregation is invalid";
            }

            targetMeta.put(translateBucketName(bucketName), keyNode.asText());
            String error = parseGraphAggregation(bucketObj, fields, targetMeta.deepCopy(), targetGraph);
            if(error != null) {
               return error;
            }
         }
         return null;
      } else {
         ObjectNode graphObj = targetGraph.addObject();
         addAggregationMeta(graphObj, targetMeta);

         ArrayNode samplesArr = graphObj.putArray("samples");

         for(final JsonNode bucketObj : bucketsObj) {
            if(!bucketObj.isObject()) {
               return "Aggregation is invalid";
            }

            JsonNode keyNode = bucketObj.get(KEY_NODE_KEY);
            if(keyNode == null) {
               return "Aggregation is invalid";
            }

            ArrayNode sampleArr = samplesArr.addArray();
            sampleArr.add(keyNode.asLong());
            JsonNode docCountNode = bucketObj.get(SAMPLES_KEY);
            long samples = docCountNode != null ? docCountNode.asLong() : 0L;
            sampleArr.add(samples);

            for(String field : fields) {
               JsonNode fieldObj = bucketObj.get(field);
               if(fieldObj != null) {
                  JsonNode valueNode = fieldObj.get("value");
                  if(valueNode != null) {
                     sampleArr.add(valueNode);
                  } else {
                     sampleArr.add(0L);
                  }
               } else {
                  sampleArr.add(0L);
               }
            }
         }

         return null;
      }
   }

   private static final Joiner keyJoiner = Joiner.on('.').skipNulls();

   /**
    * Adds the metadata to a graph built from an ES aggregation.
    * @param graphObj The graph object.
    * @param targetMeta The target (input) meta.
    */
   private void addAggregationMeta(final ObjectNode graphObj, final ObjectNode targetMeta) {
      ObjectNode meta = targetMeta.deepCopy();

      List<String> metaList = Lists.newArrayListWithExpectedSize(4);
      if(meta.has("application")) {
         metaList.add(meta.get("application").asText());
      }
      if(meta.has("host")) {
         metaList.add(meta.get("host").asText());
      }

      if(meta.has("instance")) {
         metaList.add(meta.get("instance").asText());
      }

      if(meta.has("name")) {
         metaList.add(meta.get("name").asText());
      }

      meta.put("key", keyJoiner.join(metaList));

      graphObj.set("meta", meta);
   }

   /**
    * Adds the metadata to a graph built from an ES aggregation.
    * @param key The graph key.
    * @param graphObj The node to which the meta will be added.
    * @param targetMeta The target (input) meta.
    */
   private void addMeta(final MetricKey key, final ObjectNode graphObj,
                        final ObjectNode targetMeta) {
      ObjectNode meta = targetMeta.deepCopy();

      List<String> metaList = Lists.newArrayListWithExpectedSize(4);
      if(!Strings.isNullOrEmpty(key.application)) {
         meta.put("application", key.application);
         metaList.add(key.application);
      }
      if(!Strings.isNullOrEmpty(key.host)) {
         meta.put("host", key.host);
         metaList.add(key.host);
      }
      if(!Strings.isNullOrEmpty(key.instance)) {
         meta.put("instance", key.instance);
         metaList.add(key.instance);
      }
      if(!Strings.isNullOrEmpty(key.name)) {
         meta.put("name", key.name);
         metaList.add(key.name);
      }

      meta.put("key", keyJoiner.join(metaList));

      graphObj.set("meta", meta);
   }

}
