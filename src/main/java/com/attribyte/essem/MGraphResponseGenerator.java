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

import com.attribyte.essem.model.graph.MetricKey;
import com.attribyte.essem.query.Fields;
import com.attribyte.essem.query.GraphQuery;
import com.attribyte.essem.query.HistogramQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.BaseEncoding;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramIterationValue;
import org.attribyte.api.http.Response;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.DataFormatException;

import static com.attribyte.essem.util.Util.getStringField;
import static com.attribyte.essem.util.Util.graphIgnoreProperties;
import static com.attribyte.essem.util.Util.timeUnitFromString;
import static com.attribyte.essem.util.Util.nanosToUnits;

public class MGraphResponseGenerator extends ESResponseGenerator {

   /**
    * The date-time format.
    */
   private static final String DT_FORMAT = "yyyy-MM-dd HH:mm:ss";


   @Override
   public boolean generateGraph(GraphQuery graphQuery,
                                Response esResponse,
                                EnumSet<Option> options,
                                RateUnit rateUnit,
                                HttpServletResponse response) throws IOException {

      ObjectNode esResponseObject = mapper.readTree(parserFactory.createParser(esResponse.getBody().toByteArray()));

      ArrayNode targetGraph = JsonNodeFactory.instance.arrayNode();
      List<String> fields = ImmutableList.copyOf(graphQuery.searchRequest.fields);

      if(graphQuery.isAggregation) {
         JsonNode aggregations = esResponseObject.get("aggregations");
         if(aggregations != null && aggregations.isObject()) {
            String error = parseGraphAggregation(aggregations, fields, options, rateUnit, targetGraph);
            if(error != null) {
               response.sendError(500, error);
               return false;
            } else {
               generateGraph(targetGraph, response);
               return true;
            }
         } else {
            response.sendError(500, "No graph!");
            return false;
         }
      } else {
         parseGraph(esResponseObject, fields, options, rateUnit, targetGraph);
         generateGraph(targetGraph, response);
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

   protected void parseGraph(JsonNode sourceParent,
                             List<String> fields,
                             EnumSet<Option> options,
                             RateUnit rateUnit,
                             ArrayNode targetGraph) {

      DateTimeFormatter parser = ISODateTimeFormat.basicDateTime();
      SimpleDateFormat formatter = new SimpleDateFormat(DT_FORMAT);

      Map<MetricKey, List<ObjectNode>> outputGraphs = Maps.newHashMapWithExpectedSize(4);

      JsonNode hitsObj = sourceParent.get("hits");
      if(hitsObj != null) {
         JsonNode hitsArr = hitsObj.get("hits");
         if(hitsArr != null) {
            for(JsonNode hitObj : hitsArr) {
               JsonNode fieldsObj = hitObj.get("fields");
               if(fieldsObj != null) {
                  ObjectNode outObj = mapper.createObjectNode();
                  DateTime timestamp = parser.parseDateTime(getStringField(fieldsObj, "ts"));
                  outObj.put("timestamp", timestamp.getMillis());
                  outObj.put("date", formatter.format(timestamp.getMillis()));
                  MetricKey key = new MetricKey(
                          getStringField(fieldsObj, "name"),
                          getStringField(fieldsObj, "application"),
                          getStringField(fieldsObj, "host"),
                          getStringField(fieldsObj, "instance")
                  );

                  Iterator<Map.Entry<String, JsonNode>> fieldIter = fieldsObj.fields();
                  while(fieldIter.hasNext()) {
                     Map.Entry<String, JsonNode> currField = fieldIter.next();
                     if(!graphIgnoreProperties.contains(currField.getKey())) {
                        JsonNode currValueNode = currField.getValue();
                        if(currValueNode.isArray() && currValueNode.size() > 0) {
                           setFieldValue(rateUnit, outObj, currField.getKey(), currValueNode.get(0));
                        } else if(!currValueNode.isArray()) {
                           setFieldValue(rateUnit, outObj, currField.getKey(), currValueNode);
                        }
                     }
                  }

                  List<ObjectNode> graph = outputGraphs.get(key);
                  if(graph == null) {
                     graph = Lists.newArrayListWithExpectedSize(1024);
                     outputGraphs.put(key, graph);
                  }
                  graph.add(outObj);
               }
            }
         }
      }

      if(outputGraphs.size() == 1) {
         List<ObjectNode> graphNodes = outputGraphs.values().iterator().next();
         for(ObjectNode outObj : graphNodes) {
            targetGraph.add(outObj);
         }
      } else {
         for(Map.Entry<MetricKey, List<ObjectNode>> graphEntry : outputGraphs.entrySet()) {
            MetricKey key = graphEntry.getKey();
            ObjectNode outputGraphNode = targetGraph.addObject();
            outputGraphNode.put("name", key.name);
            outputGraphNode.put("application", key.application);
            outputGraphNode.put("host", key.host);
            outputGraphNode.put("instance", key.instance);
            ArrayNode currOutputGraph = outputGraphNode.putArray("graph");
            for(ObjectNode outObj : graphEntry.getValue()) {
               currOutputGraph.add(outObj);
            }
         }
      }
   }

   private String parseGraphAggregation(JsonNode sourceParent,
                                        List<String> fields,
                                        EnumSet<Option> options,
                                        RateUnit rateUnit,
                                        ArrayNode targetGraph) {


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

            String error = parseGraphAggregation(bucketObj, fields, options, rateUnit, targetGraph);
            if(error != null) {
               return error;
            }
         }
         return null;
      } else {
         SimpleDateFormat formatter = new SimpleDateFormat(DT_FORMAT);
         boolean allowEmptyBins = options.contains(Option.EMPTY_BINS);

         for(final JsonNode bucketObj : bucketsObj) {
            if(!bucketObj.isObject()) {
               return "Aggregation is invalid";
            }

            JsonNode keyNode = bucketObj.get(KEY_NODE_KEY);
            if(keyNode == null) {
               return "Aggregation is invalid";
            }

            JsonNode docCountNode = bucketObj.get(SAMPLES_KEY);
            long samples = docCountNode != null ? docCountNode.asLong() : 0L;

            if(allowEmptyBins || samples > 0L) {

               ObjectNode sampleObj = targetGraph.addObject();
               sampleObj.put("timestamp", keyNode.asLong());
               sampleObj.put("date", formatter.format(keyNode.asLong()));
               sampleObj.put("samples", samples);

               for(String field : fields) {
                  JsonNode fieldObj = bucketObj.get(field);
                  if(fieldObj != null) {
                     JsonNode valueNode = fieldObj.get("value");
                     if(valueNode != null) {
                        if(!valueNode.isNull()) {
                           setFieldValue(rateUnit, sampleObj, field, valueNode);
                        }
                     }
                  }
               }
            }
         }
         return null;
      }
   }

   @Override
   public boolean generateHistogram(HistogramQuery histogramQuery,
                                    Response esResponse,
                                    EnumSet<Option> options,
                                    HttpServletResponse response) throws IOException {
      response.setContentType("application/json");
      response.setStatus(esResponse.getStatusCode());
      ObjectNode esResponseObject = mapper.readTree(parserFactory.createParser(esResponse.getBody().toByteArray()));
      Histogram totalHistogram = totalHistogram(esResponseObject);
      if(totalHistogram == null) {
         totalHistogram = new Histogram(2);
      }

      ObjectNode responseObject = JsonNodeFactory.instance.objectNode();

      TimeUnit convertUnit = null;
      String units = Strings.nullToEmpty(histogramQuery.units).trim();
      if(!units.isEmpty()) {
         responseObject.put("units", units);
         convertUnit = timeUnitFromString(units, null);
         if(convertUnit != null) {
            responseObject.put("timeUnits", convertUnit.toString().toLowerCase());
         }
      }

      long totalCount = totalHistogram.getTotalCount();
      responseObject.put("totalCount", totalCount);
      responseObject.put("minValue", nanosToUnits(totalHistogram.getMinNonZeroValue(), convertUnit));
      responseObject.put("maxValue", nanosToUnits(totalHistogram.getMaxValue(), convertUnit));
      responseObject.put("mean", nanosToUnits(totalHistogram.getMean(), convertUnit));
      responseObject.put("std", nanosToUnits(totalHistogram.getStdDeviation(), convertUnit));

      ArrayNode targetGraph = responseObject.putArray("bin");

      int linearBucketUnits = (int)(totalHistogram.getMaxValue() - totalHistogram.getMinNonZeroValue())/50;
      responseObject.put("buckets", linearBucketUnits);

//      for(final HistogramIterationValue histogramIterationValue : totalHistogram.percentiles(5)) {
      for(final HistogramIterationValue histogramIterationValue : totalHistogram.linearBucketValues(linearBucketUnits)) {
         toJSON(targetGraph.addObject(), histogramIterationValue, totalCount, convertUnit != null ? TimeUnit.NANOSECONDS.convert(1, convertUnit) : 1L);
      }
      response.setContentType(JSON_CONTENT_TYPE_HEADER);
      response.setStatus(HttpServletResponse.SC_OK);
      response.getOutputStream().write(responseObject.toString().getBytes("UTF-8"));
      response.getOutputStream().flush();
      return true;
   }

   /**
    * Builds a HDR histogram from a histogram query.
    * @param esResponseObject The ES response.
    * @return The histogram or {@code null}, if none.
    * @throws IOException on JSON error.
    */
   static Histogram totalHistogram(ObjectNode esResponseObject) throws IOException {
      Histogram totalHistogram = null;
      for(JsonNode valuesNode : esResponseObject.findValues(Fields.HDR_HISTOGRAM_FIELD)) {
         if(valuesNode.isArray() && valuesNode.size() > 0) {
            String encoded = valuesNode.get(0).asText();
            byte[] decoded = BaseEncoding.base64().decode(encoded);
            try {
               Histogram histogram = Histogram.decodeFromCompressedByteBuffer(ByteBuffer.wrap(decoded), 5000);
               if(totalHistogram == null) {
                  totalHistogram = histogram;
               } else {
                  totalHistogram.add(histogram);
               }
            } catch(DataFormatException de) {
               //TODO...
            }
         }
      }

      return totalHistogram;
   }

   /**
    * Writes a {@code HistogramIterationValue} to JSON.
    * @param parent The parent node.
    * @param value The value.
    * @param totalCount The total count for the histogram.
    * @param scalingDivisor Scale the values by dividing by this value.
    */
   private static void toJSON(final ObjectNode parent,
                              final HistogramIterationValue value,
                              final long totalCount,
                              final long scalingDivisor) {
      parent.put("minValue", value.getValueIteratedFrom()/scalingDivisor);
      parent.put("maxValue", value.getValueIteratedTo()/scalingDivisor);
      parent.put("percentile", value.getPercentileLevelIteratedTo());
      parent.put("count", value.getCountAddedInThisIterationStep());
      parent.put("probability", (double)value.getCountAddedInThisIterationStep()/(double)totalCount);
      parent.put("cumulativeCount", value.getTotalCountToThisValue());
      parent.put("cumulativeProbability", (double)value.getTotalCountToThisValue()/(double)totalCount);
      parent.put("cumulativeValue", value.getTotalValueToThisValue()/scalingDivisor);
   }

   /**
    * Sets a field value, converting rates, if requested.
    * @param rateUnit The rate unit.
    * @param parentObj The parent to which the field is added.
    * @param field The field name.
    * @param valueNode The input value node.
    */
   private static void setFieldValue(RateUnit rateUnit, ObjectNode parentObj, String field, JsonNode valueNode) {

      if(rateUnit == ResponseGenerator.RAW_RATE_UNIT) {
         parentObj.set(field, valueNode);
      } else {
         if(!Fields.rateFields.contains(field)) {
            parentObj.set(field, valueNode);
         } else {
            parentObj.put(field, valueNode.doubleValue() * rateUnit.mult);
         }
      }
   }
}