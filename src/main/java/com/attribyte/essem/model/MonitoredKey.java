package com.attribyte.essem.model;

import com.attribyte.essem.model.graph.MetricKey;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.Properties;

public class MonitoredKey {

   /**
    * Creates the monitored key from properties.
    * @param props The properties (index, app, host, instance, name).
    * @throws java.io.IOException on invalid properties.
    */
   public MonitoredKey(final Properties props) throws IOException {

      MetricKey.Builder builder = MetricKey.builder();

      this.index = props.getProperty("index", "").trim();
      if(this.index.length() == 0) throw new IOException("The 'index' must be specified");

      String app = props.getProperty("app", "").trim();
      if(app.length() > 0) {
         builder.setApplication(app);
      }

      String host = props.getProperty("host", "").trim();
      if(host.length() > 0) {
         builder.setHost(host);
      }

      String instance = props.getProperty("instance", "").trim();
      if(instance.length() > 0) {
         builder.setInstance(instance);
      }

      String name = props.getProperty("name", "").trim();
      if(name.length() > 0) {
         builder.setName(name);
      }

      this.key = builder.build();

      ImmutableMap.Builder<String, String> propertiesBuilder = ImmutableMap.builder();

      for(String propertyName : props.stringPropertyNames()) {
         switch(propertyName) {
            case "index":
            case "app":
            case "host":
            case "instance":
            case "name":
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

   /**
    * The index name.
    */
   public final String index;

   /**
    * The monitored key.
    */
   public final MetricKey key;

   /**
    * Additional properties associated with the key.
    */
   public final ImmutableMap<String, String> properties;

}