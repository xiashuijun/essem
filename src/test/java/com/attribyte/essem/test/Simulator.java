package com.attribyte.essem.test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.List;

public class Simulator {

   public static void main(String[] args) throws Exception {
      if(args.length == 0) {
         System.err.println("Expecting com.attribyte.essem.test.Simulator <config file>");
         System.exit(1);
      }

      final ObjectMapper mapper = new ObjectMapper();
      final JsonFactory parserFactory = new JsonFactory().enable(JsonParser.Feature.ALLOW_COMMENTS);
      byte[] config = Files.toByteArray(new File(args[0]));

      ObjectNode jsonObject = mapper.readTree(parserFactory.createParser(config));

      if(!jsonObject.has("endpoint")) {
         System.err.println("Missing 'endpoint' node");
         System.exit(1);
      }

      URI endpoint = new URI(jsonObject.get("endpoint").asText());
      String endpointUsername = jsonObject.has("endpointUsername") ? jsonObject.get("endpointUsername").asText() : null;
      String endpointPassword = jsonObject.has("endpointPassword") ? jsonObject.get("endpointPassword").asText() : null;

      JsonNode applicationsNode = jsonObject.get("application");
      if(applicationsNode == null) {
         System.err.println("Missing 'application' node");
         System.exit(1);
      }

      final List<FakeApp> apps = Lists.newArrayListWithExpectedSize(64);

      for(JsonNode applicationNode : applicationsNode) {

         if(!applicationNode.has("name")) {
            System.err.println("Missing 'name' node");
            System.exit(1);
         }

         if(!applicationNode.has("reportFrequencySeconds")) {
            System.err.println("Missing 'reportFrequencySeconds' node");
            System.exit(1);
         }

         int reportFrequencySeconds = applicationNode.get("reportFrequencySeconds").intValue();

         String appName = applicationNode.get("name").textValue();

         System.out.println("Processing application: '" + appName + "'");

         JsonNode instancesNode = applicationNode.get("instance");
         if(instancesNode == null || instancesNode.size() == 0) {
            System.err.println("At least one 'instance' must appear");
            System.exit(1);
         }

         boolean realtime = applicationNode.has("realtime") &&
                 applicationNode.get("realtime").textValue().equalsIgnoreCase("true");

         final int simulatePeriodSeconds;

         if(realtime) {
            simulatePeriodSeconds = 0;
         } else if(applicationNode.has("simulatePeriodSeconds")) {
            simulatePeriodSeconds = applicationNode.get("simulatePeriodSeconds").intValue();
         } else {
            simulatePeriodSeconds = 0;
         }

         for(JsonNode instanceNode : instancesNode) {

            if(!instanceNode.has("host")) {
               System.err.println("Missing 'host' node");
               System.exit(1);
            }

            String host = instanceNode.get("host").textValue();
            String instanceName = instanceNode.has("name") ? instanceNode.get("name").textValue() : null;

            if(instanceName != null) {
               System.out.println("Adding instance, '" + instanceName + "' on host '" + host + "'");
            } else {
               System.out.println("Adding on host '" + host + "'");
            }

            FakeApp app = new FakeApp(appName, host, instanceName, applicationNode, endpoint,
                    endpointUsername, endpointPassword, simulatePeriodSeconds, reportFrequencySeconds);
            apps.add(app);
         }
      }

      Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
         @Override
         public void run() {
            System.out.println("Stopping apps...");
            for(FakeApp app : apps) {
               app.stop();
            }
            System.out.println("Stopped!");
         }
      }));

      System.out.println("Starting apps...");

      for(FakeApp app : apps) {
         app.start();
      }

      while(true) {
         System.out.println("Simulation running...");
         System.out.println("Currently simulating metrics at " + new Date(apps.get(0).currTime()));
         Thread.sleep(30000L);
      }
   }
}
