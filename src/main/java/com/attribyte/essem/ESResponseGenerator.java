package com.attribyte.essem;

import com.attribyte.essem.es.SearchRequest;
import com.attribyte.essem.query.NameQuery;
import com.attribyte.essem.query.StatsQuery;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.attribyte.api.http.Response;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumSet;


/**
 * Generate an API response from an ES query response.
 */
public abstract class ESResponseGenerator implements ResponseGenerator {

   /**
    * The single instance of object mapper.
    */
   protected static final ObjectMapper mapper = Util.mapper;

   /**
    * The single instance of parser factory.
    */
   protected static final JsonFactory parserFactory = Util.parserFactory;

   /**
    * A set containing all components of a key.
    */
   protected static final ImmutableSet<String> keyComponents =
           ImmutableSet.of("application", "host", "instance", "name", "_type");

   /**
    * A set of all valid downsample functions.
    */
   protected static final ImmutableSet<String> downsampleFunctions =
           ImmutableSet.of("avg", "sum", "min", "max"); //stats, extended_stats...

   /**
    * The content type sent with HTTP responses ('application/json').
    */
   public static final String JSON_CONTENT_TYPE_HEADER = Util.JSON_CONTENT_TYPE_HEADER;

   /**
    * The histogram key node ('key').
    */
   public static final String KEY_NODE_KEY = "key";

   /**
    * The histogram bucket samples count key ('doc_count').
    */
   public static final String SAMPLES_KEY = "doc_count";

   /**
    * Translates the bucket name.
    * @param bucketName The bucket name.
    * @return The translated name.
    */
   protected final String translateBucketName(final String bucketName) {
      if(bucketName.equals("_type")) {
         return "metric";
      } else {
         return bucketName;
      }
   }

   @Override
   public boolean generateNames(NameQuery nameQuery,
                                final Response esResponse,
                                final EnumSet<Option> options,
                                final HttpServletResponse response) throws IOException {
      response.setContentType("application/json");
      response.setStatus(esResponse.getStatusCode());
      response.getOutputStream().write(esResponse.getBody().toByteArray());  //TODO
      return esResponse.getStatusCode() == 200;
   }

   @Override
   public boolean generateStats(StatsQuery statsQuery,
                                Response esResponse,
                                EnumSet<Option> options,
                                HttpServletResponse response) throws IOException {
      ObjectNode jsonObject = mapper.readTree(parserFactory.createParser(esResponse.getBody().toByteArray()));
      ObjectNode responseObject = JsonNodeFactory.instance.objectNode();
      ObjectNode meta = responseObject.putObject("meta");

      if(!Strings.isNullOrEmpty(statsQuery.range.expression)) {
         meta.put("range", statsQuery.range.expression);
      }
      if(statsQuery.range.startTimestamp > 0L) {
         meta.put("rangeStartTimestamp", statsQuery.range.startTimestamp);
      }
      if(statsQuery.range.endTimestamp > 0L) {
         meta.put("rangeEndTimestamp", statsQuery.range.endTimestamp);
      }
      if(!Strings.isNullOrEmpty(statsQuery.key.application)) {
         meta.put("application", statsQuery.key.application);
      }
      if(!Strings.isNullOrEmpty(statsQuery.key.host)) {
         meta.put("host", statsQuery.key.host);
      }
      if(!Strings.isNullOrEmpty(statsQuery.key.instance)) {
         meta.put("instance", statsQuery.key.instance);
      }
      if(!Strings.isNullOrEmpty(statsQuery.key.name)) {
         meta.put("name", statsQuery.key.name);
      }
      if(!Strings.isNullOrEmpty(statsQuery.key.field)) {
         meta.put("field", statsQuery.key.field);
      }

      JsonNode aggregations = jsonObject.get("aggregations");
      if(aggregations != null) {
         JsonNode stats = aggregations.get("stats");
         if(stats != null) {
            responseObject.set("stats", stats);
         }
      }

      response.setContentType("application/json");
      response.setStatus(200);
      response.getOutputStream().write(responseObject.toString().getBytes(Charsets.UTF_8));
      response.getOutputStream().flush();
      return true;
   }
}