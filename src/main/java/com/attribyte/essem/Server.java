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

import com.attribyte.essem.model.DisplayTZ;
import com.attribyte.essem.model.MonitoredApplication;
import com.attribyte.essem.model.MonitoredEndpoint;
import com.attribyte.essem.util.Util;
import org.attribyte.essem.reporter.EssemReporter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.ByteString;
import org.apache.log4j.PropertyConfigurator;
import org.attribyte.api.Logger;
import org.attribyte.api.http.AsyncClient;
import org.attribyte.api.http.ClientOptions;
import org.attribyte.api.http.Request;
import org.attribyte.api.http.RequestOptions;
import org.attribyte.api.http.Response;
import org.attribyte.api.http.impl.jetty.JettyClient;
import org.attribyte.util.InitUtil;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.LifeCycle;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The standalone server.
 */
public class Server {

   /**
    * The IP to which the server is bound. By default,
    * it binds to all interfaces.
    */
   public static final String LISTEN_IP_KEY = "listenIP";

   /**
    * The port to which the server is bound. 8080 is the default.
    */
   public static final String LISTEN_PORT_KEY = "listenPort";

   /**
    * The path where HTTP request logs are stored. If unspecified,
    * no request logs are stored.
    */
   public static final String REQUEST_LOG_PATH_KEY = "requestLogPath";

   /**
    * The prefix applied to request log filenames.
    */
   public static final String REQUEST_LOG_PREFIX_KEY = "requestLogPrefix";

   /**
    * The time zone for request log timestamps.
    */
   public static final String REQUEST_LOG_TIMEZONE_KEY = "requestLogTimeZone";

   /**
    * The number of days request logs are retained.
    */
   public static final String REQUEST_LOG_RETAIN_DAYS_KEY = "requestLogRetainDays";

   /**
    * Are request logs written in "extended" format?.
    */
   public static final String REQUEST_LOG_EXTENDED_KEY = "requestLogExtendedFormat";

   /**
    * The name of the index to which internal metrics are reported ('essem').
    */
   public static final String ESSEM_INDEX_NAME = "essem";

   /**
    * The system property that points to the installation directory.
    */
   public static final String ESSEM_INSTALL_DIR_SYSPROP = "essem.install.dir";

   /**
    * The internal scheduler.
    */
   private static final ScheduledExecutorService scheduler =
           MoreExecutors.getExitingScheduledExecutorService(
                   new ScheduledThreadPoolExecutor(1,
                           new ThreadFactoryBuilder().setNameFormat("essem-reporter-%d").build()
                   )
           );

   /**
    * Starts the server.
    * @param args The startup args.
    * @throws Exception on startup error.
    */
   public static void main(String[] args) throws Exception {

      if(args.length < 1) {
         System.err.println("Start-up error: Expecting <config file> [config file] ...");
         System.exit(1);
      }

      final Properties props = new Properties();
      final Properties logProps = new Properties();
      loadProperties(args, props, logProps); //Exits with message on any error...
      PropertyConfigurator.configure(logProps);

      final Logger logger = new Logger() {
         private final org.apache.log4j.Logger logger =
                 org.apache.log4j.Logger.getLogger(ESSEM_INDEX_NAME);

         public void debug(String msg) {
            this.logger.debug(msg);
         }

         public void info(String msg) {
            this.logger.info(msg);
         }

         public void warn(String msg) {
            this.logger.warn(msg);
         }

         public void warn(String msg, Throwable t) {
            this.logger.warn(msg, t);
         }

         public void error(String msg) {
            this.logger.error(msg);
         }

         public void error(String msg, Throwable t) {
            this.logger.error(msg, t);
         }
      };

      try {

         String listenIP = props.getProperty(LISTEN_IP_KEY, null);

         int httpPort = Integer.parseInt(props.getProperty(LISTEN_PORT_KEY, "8080"));

         logInfo(logger, "Starting server on port " + httpPort);

         org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server();

         server.addLifeCycleListener(new Shutdown());

         HttpConfiguration httpConfig = new HttpConfiguration();
         httpConfig.setOutputBufferSize(1024);
         httpConfig.setRequestHeaderSize(1024);
         httpConfig.setResponseHeaderSize(1024);
         httpConfig.setSendServerVersion(false);
         httpConfig.setSendDateHeader(false);
         ServerConnector httpConnector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
         if(listenIP != null) {
            httpConnector.setHost(listenIP);
         }
         httpConnector.setPort(httpPort);
         httpConnector.setIdleTimeout(30000L);
         server.addConnector(httpConnector);

         HandlerCollection serverHandlers = new HandlerCollection();
         server.setHandler(serverHandlers);

         ServletContextHandler rootContext = new ServletContextHandler(ServletContextHandler.NO_SESSIONS | ServletContextHandler.NO_SECURITY);
         rootContext.setContextPath("/");
         rootContext.addAliasCheck(new ContextHandler.ApproveAliases());
         //rootContext.setErrorHandler(new JettyErrorHandler(logger));
         serverHandlers.addHandler(rootContext);

         File requestLogPathFile = getSystemFile(REQUEST_LOG_PATH_KEY, props, false);

         if(requestLogPathFile != null) {
            String requestLogPath = requestLogPathFile.getAbsolutePath();
            if(!requestLogPath.endsWith("/")) {
               requestLogPath = requestLogPath + "/";
            }

            String requestLogPrefix = props.getProperty(REQUEST_LOG_PREFIX_KEY, ESSEM_INDEX_NAME);
            int requestLogRetainDays = Integer.parseInt(props.getProperty(REQUEST_LOG_RETAIN_DAYS_KEY, "180"));
            boolean requestLogExtendedFormat = props.getProperty(REQUEST_LOG_EXTENDED_KEY, "true").equalsIgnoreCase("true");

            String requestLogTimeZone = props.getProperty(REQUEST_LOG_TIMEZONE_KEY, TimeZone.getDefault().getID());

            NCSARequestLog requestLog = new NCSARequestLog(requestLogPath + requestLogPrefix + "-yyyy_mm_dd.request.log");
            requestLog.setRetainDays(requestLogRetainDays);
            requestLog.setAppend(true);
            requestLog.setExtended(requestLogExtendedFormat);
            requestLog.setLogTimeZone(requestLogTimeZone);
            requestLog.setLogCookies(false);
            requestLog.setPreferProxiedForAddress(true);

            RequestLogHandler requestLogHandler = new RequestLogHandler();
            requestLogHandler.setRequestLog(requestLog);
            serverHandlers.addHandler(requestLogHandler);
         } else {
            logInfo(logger, "Request logging is disabled!");
         }

         final MetricRegistry internalRegistry = new MetricRegistry();

         ClientOptions httpClientOptions = new ClientOptions("httpClient.", props);
         AsyncClient httpClient = new JettyClient(httpClientOptions);

         int maxResponseBytes = Integer.parseInt(props.getProperty("httpClient.maxResponseBytes", "1000000000"));
         int timeoutSeconds = Integer.parseInt(props.getProperty("httpClient.timeoutSeconds", "45"));

         RequestOptions requestOptions = new RequestOptions(false, maxResponseBytes, timeoutSeconds); //Don't follow redirects.

         if(!props.containsKey("esEndpoint")) {
            throw new Exception("The 'esEndpoint' must be specified!");
         }

         final ESEndpoint esEndpoint;

         try {

            String esUsername = props.getProperty("esUsername", "").trim();
            String esPassword = props.getProperty("esPassword", "").trim();
            final String esAuth = esUsername.length() > 0 && esPassword.length() > 0 ?
                    BasicAuth.basicAuthValue(esUsername, esPassword) : null;
            esEndpoint = new ESEndpoint(props.getProperty("esEndpoint"), esAuth);
         } catch(URISyntaxException use) {
            throw new Exception("The 'esEndpoint' is not a valid URI", use);
         }

         testES(esEndpoint, httpClient); //Throws exception on failure...

         File esSchemaFile = getSystemFile("esSchemaFile", props, true); //Must exist
         ByteString esSchema = ByteString.copyFrom(Files.toByteArray(esSchemaFile));
         Reporter reporter = new ESReporter(esEndpoint, esSchema, httpClient, logger);
         reporter.createStore(ESSEM_INDEX_NAME);

         File esUserSchemaFile = getSystemFile("esUserSchemaFile", props, true); //Must exist
         ByteString esUserSchema = ByteString.copyFrom(Files.toByteArray(esUserSchemaFile));
         ESUserStore userStore = new ESUserStore(esEndpoint, esUserSchema, httpClient, logger);

         RetryStrategy retryStrategy = new RetryStrategy.ExponentialBackoff();
         retryStrategy.init(new InitUtil("retry.", props, false).getProperties());

         final ReportQueue reportQueue = ReportQueue.fromProperties(reporter, retryStrategy, new InitUtil("reporter.", props, false).getProperties());

         Properties authProps = new InitUtil("auth.", props, false).getProperties();
         final IndexAuthorization authorization = buildAuth(authProps, reporter);
         for(String index : authorization.authorizedIndexes()) {
            logInfo(logger, "Creating user store for " + index);
            userStore.createStore(index);
         }

         Properties reportAuthProps = new InitUtil("reportAuth.", props, false).getProperties();
         final IndexAuthorization reportAuthorization = buildAuth(reportAuthProps, reporter);

         //Enable monitoring for endpoints...if any

         final List<MonitoredEndpoint> monitoredEndpoints = initMonitoredEndpoints(props, logger);
         if(monitoredEndpoints.size() > 0) {
            for(MonitoredEndpoint endpoint : monitoredEndpoints) {
               reporter.createStore(endpoint.index);
            }
            logInfo(logger, "Initialized " + monitoredEndpoints.size() + " sampled endpoints");
         }

         int endpointSamplerConcurrency = Integer.parseInt(props.getProperty("endpointSamplerConcurrency", "4"));

         final JSONEndpointSampler endpointSampler = monitoredEndpoints.size() > 0 ?
                 new JSONEndpointSampler(endpointSamplerConcurrency, monitoredEndpoints, httpClient, requestOptions, reportQueue, logger) : null;

         int indexKeeperConcurrency = Integer.parseInt(props.getProperty("indexKeeperConcurrency", "2"));
         int indexKeeperFrequency = Integer.parseInt(props.getProperty("indexKeeperFrequencyMinutes", "30"));

         final IndexKeeper indexKeeper;
         final List<MonitoredApplication> monitoredApps = initMonitoredApplications(props, logger);
         if(monitoredApps.size() > 0) {
            logger.info("Initializing index keeper...");
            indexKeeper = new IndexKeeper(monitoredApps, indexKeeperFrequency, indexKeeperConcurrency, httpClient, esEndpoint, logger);
            logger.info("Initialized index keeper with " + monitoredApps.size() + " monitored apps");
         } else {
            indexKeeper = null;
         }

         ReportServlet reportServlet = new ReportServlet(reportQueue, reportAuthorization);

         rootContext.addServlet(new ServletHolder(reportServlet), "/report/*");

         APIServlet apiServlet = new APIServlet(esEndpoint, httpClient, requestOptions, authorization, new DefaultResponseGenerator());
         rootContext.addServlet(new ServletHolder(apiServlet), "/api/*");
         internalRegistry.register("api", apiServlet);

         APIServlet mgraphServlet = new APIServlet(esEndpoint, httpClient, requestOptions, authorization, new MGraphResponseGenerator());
         rootContext.addServlet(new ServletHolder(mgraphServlet), "/mgraph/*");
         internalRegistry.register("mgraph", mgraphServlet);

         APIServlet passthroughServlet = new APIServlet(esEndpoint, httpClient, requestOptions, authorization, new PassthroughResponseGenerator());
         rootContext.addServlet(new ServletHolder(passthroughServlet), "/pass/*");
         internalRegistry.register("passthrough", passthroughServlet);

         if(props.getProperty("console.enabled", "false").equalsIgnoreCase("true")) {
            File assetDirFile = getSystemFile("console.assetDirectory", props, true);
            if(!assetDirFile.isDirectory()) throw new Exception("The 'console.assetDirectory' must be a directory");

            File templateDirFile = getSystemFile("console.templateDirectory", props, true);
            if(!templateDirFile.isDirectory()) throw new Exception("The 'console.templateDirectory' must be a directory");

            File dashboardTemplateDirFile = getSystemFile("console.dashboardTemplateDirectory", props, true);
            if(!dashboardTemplateDirFile.isDirectory()) throw new Exception("The 'console.dashboardTemplateDirectory' must be a directory");

            List<String> allowedAssetPaths = Lists.newArrayList(
                    Splitter.on(',').omitEmptyStrings().trimResults().split(props.getProperty("console.assetPaths", ""))
            );

            List<String> allowedIndexes = Lists.newArrayList(
                    Splitter.on(',').omitEmptyStrings().trimResults().split(props.getProperty("console.indexes", ""))
            );

            boolean consoleDebugMode = props.getProperty("console.debug", "true").equalsIgnoreCase("true");

            final List<DisplayTZ> consoleZones;
            File consoleZonesFile = getSystemFile("console.timezones", props, false);

            if(consoleZonesFile != null) {
               consoleZones = DisplayTZ.parse(consoleZonesFile);
            } else {
               consoleZones = Collections.emptyList();
            }

            ConsoleServlet consoleServlet = new ConsoleServlet(esEndpoint, userStore, rootContext, authorization,
                    templateDirFile.getAbsolutePath(), dashboardTemplateDirFile.getAbsolutePath(),
                    assetDirFile.getAbsolutePath(), allowedAssetPaths, allowedIndexes,
                    consoleZones, httpClient, requestOptions, logger, consoleDebugMode);
            rootContext.addServlet(new ServletHolder(consoleServlet), "/console/*");
            internalRegistry.register("console-application-cache", consoleServlet.applicationCache);


            logInfo(logger, "Console is enabled");
         } else {
            logInfo(logger, "Console is disabled");
         }

         //Schedule internal metrics reports...

         internalRegistry.register("report-servlet", reportServlet);
         scheduler.scheduleAtFixedRate(new Runnable() {
            final EssemReporter reporter = EssemReporter.newBuilder(null, internalRegistry)
                    .forApplication("essem-server")
                    .forHost(java.net.InetAddress.getLocalHost().getHostName())
                    .build();

            @Override
            public void run() {
               try {
                  QueuedReport report = new QueuedReport(ESSEM_INDEX_NAME, reporter.buildReport(internalRegistry));
                  reportQueue.enqueueReport(report);
               } catch(InterruptedException ie) {
                  Thread.currentThread().interrupt();
               } catch(Throwable t) {
                  t.printStackTrace();
               }
            }
         }, 1, 1, TimeUnit.MINUTES);

         server.setStopAtShutdown(true);
         server.start();
         server.join();

      } catch(Throwable t) {
         System.err.println("Startup problem: " + t.getMessage());
         logger.error("Startup problem", t);
         System.exit(1);
      }
   }

   /**
    * Creates index authorization from properties.
    * @param authProps The properties.
    * @param reporter The reporter (for index creation).
    * @return The index authorization or <code>null</code>.
    * @throws IOException on initialization error.
    */
   private static IndexAuthorization buildAuth(final Properties authProps,
                                               final Reporter reporter) throws IOException {
      if(authProps.size() == 0) {
         return null;
      }

      IndexAuthorization authorization = new BasicAuth(authProps);
      for(String index : authorization.authorizedIndexes()) {
         reporter.createStore(index);
      }

      return authorization;
   }

   /**
    * Initialize any configured sampled endpoints.
    * @param props The config properties.
    * @param logger The logger for warnings/errors.
    * @throws Exception on initialization error.
    */
   private static List<MonitoredEndpoint> initMonitoredEndpoints(final Properties props, final Logger logger) throws Exception {

      File sampledEndpointDir = getSystemFile("sampledEndpointDir", props, false);

      List<MonitoredEndpoint> monitoredEndpoints = Lists.newArrayListWithExpectedSize(16);

      if(sampledEndpointDir != null) {
         File[] propFiles = sampledEndpointDir.listFiles();
         if(propFiles != null) {
            for(File propFile : propFiles) {
               if(propFile.isDirectory()) continue;
               Properties endpointProps = new Properties();
               try {
                  byte[] propBytes = Files.toByteArray(propFile);
                  endpointProps.load(new ByteArrayInputStream(propBytes));
                  if(endpointProps.getProperty("url") != null) {
                     monitoredEndpoints.add(new MonitoredEndpoint(endpointProps));
                  }
               } catch(IOException ioe) {
                  logger.warn("Problem loading sampled endpoint '" + propFile.getAbsolutePath() + "'", ioe);
               }
            }
         }
      }

      return monitoredEndpoints;
   }


   /**
    * Initialize any applications configured for monitoring.
    * @param props The config properties.
    * @param logger The logger for warnings/errors.
    * @return The list of monitored applications.
    * @throws Exception on initialization error.
    */
   private static List<MonitoredApplication> initMonitoredApplications(final Properties props, final Logger logger) throws Exception {

      List<MonitoredApplication> monitoredApps = Lists.newArrayListWithExpectedSize(16);
      File monitoredAppDir = getSystemFile("monitoredAppDir", props, false);

      if(monitoredAppDir != null) {
         File[] propFiles = monitoredAppDir.listFiles();

         if(propFiles != null) {
            for(File propFile : propFiles) {
               if(propFile.isDirectory()) continue;
               Properties endpointProps = new Properties();
               try {
                  byte[] propBytes = Files.toByteArray(propFile);
                  endpointProps.load(new ByteArrayInputStream(propBytes));
                  if(endpointProps.getProperty("index") != null) {
                     monitoredApps.add(new MonitoredApplication(endpointProps));
                  }
               } catch(IOException ioe) {
                  logger.warn("Problem loading monitored app '" + propFile.getAbsolutePath() + "'", ioe);
               }
            }
         }
      }

      return monitoredApps;
   }

   /**
    * Startup information + graceful shutdown.
    */
   public static class Shutdown implements LifeCycle.Listener {

      public void lifeCycleFailure(LifeCycle event, Throwable cause) {
         System.out.println("Essem Failure " + cause.toString());
      }

      public void lifeCycleStarted(LifeCycle event) {
         System.out.println("Essem Started...");
      }

      public void lifeCycleStarting(LifeCycle event) {
         System.out.println("Essem Server Starting...");
      }

      public void lifeCycleStopped(LifeCycle event) {
         System.out.println("Essem Server Stopped...");
      }

      public void lifeCycleStopping(LifeCycle event) {
         System.out.println("Essem Server shutdown!");
      }
   }

   /**
    * Log an informational message to both a logger and the console during start-up.
    * @param logger The logger.
    * @param message The message to log.
    */
   private static void logInfo(final Logger logger, final String message) {
      System.out.println(message);
      logger.info(message);
   }

   /**
    * Load properties from an array of property file names.
    * <p>
    * Properties are added to <code>logProps</code> when
    * name starts with 'log.'. Exits with error code and message
    * on any load problems.
    * </p>
    * @param filenames The file names.
    * @param props The properties to fill.
    * @param logProps Logger-specific properties.
    */
   private static void loadProperties(final String[] filenames, final Properties props,
                                      final Properties logProps) {

      for(String filename : filenames) {

         File f = new File(filename);

         if(!f.exists()) {
            System.err.println("Start-up error: The configuration file, '" + f.getAbsolutePath() + " does not exist");
            System.exit(1);
         }

         if(!f.canRead()) {
            System.err.println("Start-up error: The configuration file, '" + f.getAbsolutePath() + " is not readable");
            System.exit(1);
         }

         FileInputStream fis = null;
         Properties currProps = new Properties();

         try {
            fis = new FileInputStream(f);
            currProps.load(fis);
            if(f.getName().startsWith("log.")) {
               logProps.putAll(resolveLogFiles(currProps));
            } else {
               props.putAll(currProps);
            }
         } catch(IOException ioe) {
            System.err.println("Start-up error: Problem reading configuration file, '" + f.getAbsolutePath() + "'");
            Util.closeQuietly(fis);
            fis = null;
            System.exit(1);
         } finally {
            Util.closeQuietly(fis);
         }
      }
   }

   /**
    * Examines log configuration keys for those that represent files to add
    * system install path if not absolute.
    * @param logProps The properties.
    * @return The properties with modified values.
    */
   private static Properties resolveLogFiles(final Properties logProps) {

      Properties filteredProps = new Properties();

      for(String key : logProps.stringPropertyNames()) {
         if(key.endsWith(".File")) {
            String filename = logProps.getProperty(key);
            if(filename.startsWith("/")) {
               filteredProps.put(key, filename);
            } else {
               filteredProps.put(key, systemInstallDir() + filename);
            }
         } else {
            filteredProps.put(key, logProps.getProperty(key));
         }
      }

      return filteredProps;
   }

   /**
    * Gets the system install directory (always ends with '/').
    * @return The directory.
    */
   private static String systemInstallDir() {
      String systemInstallDir = System.getProperty(ESSEM_INSTALL_DIR_SYSPROP, "").trim();
      if(systemInstallDir.length() > 0 && !systemInstallDir.endsWith("/")) {
         systemInstallDir = systemInstallDir + "/";
      }
      return systemInstallDir;
   }

   /**
    * Loads a file defined by a property and expected to be in the system install directory.
    * <p>
    * The system install directory will be added as a prefix if the property value
    * does not start with '/'.
    * </p>
    * @param propName The property name.
    * @param props The properties.
    * @param mustExist Specify if the file must exist.
    * @return The file.
    * @throws Exception if file is not specified, does not exist or is not readable.
    */
   private static File getSystemFile(final String propName, final Properties props, final boolean mustExist) throws Exception {

      String filename = props.getProperty(propName, "").trim();
      if(filename.length() > 0) {

         if(!filename.startsWith("/")) {
            filename = systemInstallDir() + filename;
         }

         File systemFile = new File(filename);

         if(!systemFile.exists()) {
            throw new Exception("'" + systemFile.getAbsolutePath() + "' does not exist");
         }

         if(!systemFile.canRead()) {
            throw new Exception("'" + systemFile.getAbsolutePath() + "' is not readable");
         }

         return systemFile;

      } else if(mustExist) {
         throw new Exception("The '" + propName + "' must be specified");
      } else {
         return null;
      }
   }

   /**
    * Test the ES connection.
    * @param esEndpoint The endpoint URI.
    * @param client The HTTP client.
    * @throws java.lang.Exception if connection could not be established.
    */
   private static void testES(final ESEndpoint esEndpoint, final AsyncClient client) throws Exception {

      try {
         final Request esIndexRequest =
                 esEndpoint.getRequestBuilder(esEndpoint.buildIndexStatsURI(null)).create();
         final Response esIndexResponse = client.send(esIndexRequest);
         switch(esIndexResponse.getStatusCode()) {
            case 200:
               return;
            default:
               throw new Exception("ES communication error (" + esIndexResponse.getStatusCode() + ")");
         }
      } catch(IOException ioe) {
         throw new Exception("ES endpoint appears to be down", ioe);
      }
   }
}