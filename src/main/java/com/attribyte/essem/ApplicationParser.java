package com.attribyte.essem;

import com.attribyte.essem.model.Application;
import com.attribyte.essem.model.Host;
import com.attribyte.essem.model.Metric;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Parse applications and associated metrics from ES responses.
 */
public class ApplicationParser {

   /**
    * Parses applications from the ES aggregation response to <code>ApplicationsQuery</code>.
    * @param index The source index.
    * @param esObject The ES response object.
    * @return The list of applications.
    * @throws IOException on invalid JSON.
    */
   static List<Application> parseApplications(final String index, final ObjectNode esObject) throws IOException {

      JsonNode aggregationsObj = esObject.get("aggregations");
      if(aggregationsObj == null) {
         throw new IOException("Expecting 'aggregations' in ES response");
      }

      JsonNode applicationObj = aggregationsObj.get("application");
      if(applicationObj == null) {
         throw new IOException("Expecting 'application' in ES response");
      }

      JsonNode applicationBucketsObj = applicationObj.get("buckets");
      if(applicationBucketsObj == null) {
         throw new IOException("Expecting 'application/buckets' in ES response");
      }

      List<Application> applications = Lists.newArrayListWithExpectedSize(16);
      Set<String> tmpInstances = Sets.newHashSetWithExpectedSize(16);
      Set<Host> tmpHosts = Sets.newHashSetWithExpectedSize(16);

      for(JsonNode applicationBucket : applicationBucketsObj) {
         if(applicationBucket.has("key")) {
            String applicationName = applicationBucket.get("key").asText();
            JsonNode hostObj = applicationBucket.get("host");
            if(hostObj != null) {
               JsonNode hostBucketsObj = hostObj.get("buckets");
               if(hostBucketsObj != null) {
                  tmpHosts.clear();
                  for(JsonNode hostBucket : hostBucketsObj) {
                     if(hostBucket.has("key")) {
                        String host = hostBucket.get("key").asText();
                        JsonNode instanceObj = hostBucket.get("instance");
                        if(instanceObj != null) {
                           JsonNode instanceBucketsObj = instanceObj.get("buckets");
                           if(instanceBucketsObj != null) {
                              tmpInstances.clear();
                              for(JsonNode instanceBucket : instanceBucketsObj) {
                                 if(instanceBucket.has("key")) {
                                    String instance = instanceBucket.get("key").asText();
                                    tmpInstances.add(instance);
                                 }
                              }
                           }
                        }
                        tmpHosts.add(new Host(host, tmpInstances));
                     }
                  }
               }
            }
            applications.add(new Application(applicationName, index, tmpHosts));
         }
      }

      return applications;
   }

   /**
    * Parses metrics from the ES aggregation response to <code>ApplicationMetricsQuery</code>.
    * @param esObject The ES response object.
    * @return The list of metrics.
    * @throws IOException on invalid JSON.
    */
   static List<Metric> parseMetrics(final ObjectNode esObject) throws IOException {

      JsonNode aggregationsObj = esObject.get("aggregations");
      if(aggregationsObj == null) {
         throw new IOException("Expecting 'aggregations' in ES response");
      }

      JsonNode typeObj = aggregationsObj.get("_type");
      if(typeObj == null) {
         throw new IOException("Expecting '_type' in ES response");
      }

      JsonNode typeBucketsObj = typeObj.get("buckets");
      if(typeBucketsObj == null) {
         throw new IOException("Expecting '_type/buckets' in ES response");
      }

      List<Metric> metrics = Lists.newArrayListWithExpectedSize(128);

      for(JsonNode typeBucket : typeBucketsObj) {
         if(typeBucket.has("key")) {
            Metric.Type type = Metric.Type.fromString(typeBucket.get("key").asText());
            JsonNode namesObj = typeBucket.get("name");
            if(namesObj != null) {
               JsonNode nameBucketsObj = namesObj.get("buckets");
               if(nameBucketsObj != null) {
                  for(JsonNode nameBucket : nameBucketsObj) {
                     if(nameBucket.has("key")) {
                        metrics.add(new Metric(nameBucket.get("key").asText(), type));
                     }
                  }
               }
            }
         }
      }

      return metrics;
   }

}
