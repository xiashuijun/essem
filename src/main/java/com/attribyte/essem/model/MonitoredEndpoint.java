package com.attribyte.essem.model;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * An endpoint monitored for reports.
 */
public class MonitoredEndpoint {

   /**
    * Creates the endpoint from properties.
    * @param props The properties (url, frequencySeconds, app, host, instance, index, username, password).
    * @throws IOException on invalid properties.
    * @throws URISyntaxException on invalid URI.
    */
   public MonitoredEndpoint(final Properties props) throws IOException, URISyntaxException {
      String url = props.getProperty("url", "").trim();
      if(url.length() == 0) throw new IOException("A 'url' must be specified");
      this.url = new URI(url);

      this.frequencySeconds = Integer.parseInt(props.getProperty("frequencySeconds", "0"));
      if(this.frequencySeconds < 1) throw new IOException("The 'frequencySeconds' is invalid");

      this.app = props.getProperty("app", "").trim();
      if(this.app.length() == 0) throw new IOException("An 'app' must be specified");


      this.host = props.getProperty("host", "").trim();
      if(this.host.length() == 0) throw new IOException("The 'host' must be specified");

      this.index = props.getProperty("index", "").trim();
      if(this.index.length() == 0) throw new IOException("The 'index' must be specified");

      String _instance = props.getProperty("instance", "").trim();
      if(_instance.length() > 0) {
         this.instance = _instance;
      } else {
         this.instance = null;
      }

      String username = props.getProperty("username", "").trim();
      String password = props.getProperty("password", "").trim();
      if(username.length() > 0 && password.length() > 0) {
         this.authHeader = "Basic " + BaseEncoding.base64().encode((username + ":" + password).getBytes(Charsets.UTF_8));
      } else {
         this.authHeader = null;
      }
   }

   public final URI url;
   public final int frequencySeconds;
   public final String app;
   public final String host;
   public final String index;
   public final String authHeader;
   public final String instance;
}
