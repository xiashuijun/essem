package com.attribyte.essem;

import com.attribyte.essem.model.graph.FloatPoint;
import com.attribyte.essem.model.graph.Graph;
import com.attribyte.essem.model.graph.IntPoint;
import com.attribyte.essem.model.graph.MetricKey;
import com.attribyte.essem.model.graph.Point;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.List;
import java.util.Map;

import static com.attribyte.essem.util.Util.getStringField;
import static com.attribyte.essem.util.Util.getFieldNode;
import static com.attribyte.essem.util.Util.graphIgnoreProperties;
import static com.attribyte.essem.util.Util.integralNumberFields;

/**
 * Parse graphs from ES responses that result from <code>GraphQuery</code>.
 */
public class GraphParser {

   /**
    * Parses a graph response (with no aggregation).
    * @param esObject The ES response object.
    * @param fields The fields in the desired order.
    * @return A map of graph vs key (that includes metric name, application, host, instance, and field).
    */
   public static Map<MetricKey, Graph> parseGraph(ObjectNode esObject, List<String> fields) {

      DateTimeFormatter parser = ISODateTimeFormat.basicDateTime();

      Map<MetricKey, List<Point>> outputGraphs = Maps.newHashMapWithExpectedSize(4);

      JsonNode hitsObj = esObject.get("hits");
      if(hitsObj != null) {
         JsonNode hitsArr = hitsObj.get("hits");
         if(hitsArr != null) {

            for(JsonNode hitObj : hitsArr) {
               JsonNode fieldsObj = hitObj.get("fields");
               if(fieldsObj != null) {
                  MetricKey keyProto = new MetricKey(
                          getStringField(fieldsObj, "name"),
                          getStringField(fieldsObj, "application"),
                          getStringField(fieldsObj, "host"),
                          getStringField(fieldsObj, "instance")
                  );

                  long timestamp = parser.parseDateTime(getStringField(fieldsObj, "ts")).getMillis();

                  for(String field : fields) {
                     if(!graphIgnoreProperties.contains(field)) {

                        MetricKey key = new MetricKey(keyProto, field);
                        List<Point> points = outputGraphs.get(key);
                        if(points == null) {
                           points = Lists.newArrayListWithExpectedSize(1024);
                           outputGraphs.put(key, points);
                        }

                        JsonNode fieldNode = getFieldNode(fieldsObj, field);
                        if(integralNumberFields.contains(field)) {
                           if(fieldNode.canConvertToLong()) {
                              points.add(new IntPoint(timestamp, fieldNode.longValue()));
                           }
                        } else {
                           if(fieldNode.isNumber()) {
                              points.add(new FloatPoint(timestamp, fieldNode.doubleValue()));
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      Map<MetricKey, Graph> graphMap = Maps.newHashMapWithExpectedSize(outputGraphs.size());
      for(Map.Entry<MetricKey, List<Point>> graph : outputGraphs.entrySet()) {
         graphMap.put(graph.getKey(), new Graph(graph.getValue()));
      }

      return graphMap;
   }
}
