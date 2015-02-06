package com.attribyte.essem.model;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.Properties;

public class MonitoredApplication {

   /**
    * Creates a monitored application with additional properties.
    * @param props The properties (index, app).
    * @throws java.io.IOException on invalid properties.
    */
   public MonitoredApplication(final Properties props) throws IOException {

      this.index = props.getProperty("index", "").trim();
      if(this.index.length() == 0) throw new IOException("The 'index' must be specified");

      this.application = props.getProperty("app", "").trim();
      if(this.application.length() == 0) throw new IOException("The 'app' must be specified");

      ImmutableMap.Builder<String, String> propertiesBuilder = ImmutableMap.builder();

      for(String propertyName : props.stringPropertyNames()) {
         switch(propertyName) {
            case "index":
            case "app":
               break;
            default:
               String value = Strings.nullToEmpty(props.getProperty(propertyName)).trim();
               if(value.length() > 0) {
                  propertiesBuilder.put(propertyName, value);
               }
         }
      }

      this.properties = propertiesBuilder.build();
   }

   @Override
   public String toString() {
      return index + "." + application;
   }

   /**
    * The index name.
    */
   public final String index;

   /**
    * The application name.
    */
   public final String application;

   /**
    * Additional properties associated with the key.
    */
   public final ImmutableMap<String, String> properties;
}