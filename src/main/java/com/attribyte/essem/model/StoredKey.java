/*
 * Copyright 2015 Attribyte, LLC
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

package com.attribyte.essem.model;

import com.attribyte.essem.model.graph.MetricKey;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.Date;

/**
 * A stored key.
 * <p>
 *    A stored key is uniquely identified by index, user, metric key and field.
 *    An optional title is allowed.
 * </p>
 */
public class StoredKey {

   /**
    * A comparator that sorts by create time (ascending).
    */
   public static final Comparator<StoredKey> createTimeComparator = new Comparator<StoredKey>() {
      @Override
      public int compare(final StoredKey o1, final StoredKey o2) {
         return o1.createTime.compareTo(o2.createTime);
      }
   };

   /**
    * Hash function used to create the id.
    */
   private static final HashFunction hasher = Hashing.murmur3_128();

   /**
    * The JSON parser factory.
    */
   private static final JsonFactory parserFactory = new JsonFactory().enable(JsonParser.Feature.ALLOW_COMMENTS);

   /**
    * Creates a key.
    * @param index The associated index.
    * @param uid The user id.
    * @param key The key.
    * @param createTime The create time.
    */
   public StoredKey(final String index, final String uid, final MetricKey key,
                    final Date createTime, final String title) {
      this.index = Strings.nullToEmpty(index);
      this.uid = Strings.nullToEmpty(uid);
      this.key = key;
      this.createTime = createTime != null ? createTime : new Date();
      this.id = hasher.newHasher()
              .putString(this.index)
              .putString(this.uid)
              .putString(key.application)
              .putString(key.host)
              .putString(key.instance)
              .putString(key.name)
              .putString(key.field).hash().toString();
      this.title = Strings.nullToEmpty(title);
   }

   /**
    * Creates a key with a new title from a previously stored key.
    * @param proto The proto key.
    * @param title Tne new title.
    */
   public StoredKey(final StoredKey proto, final String title) {
      this(proto.index, proto.uid, proto.key, proto.createTime, title);
   }

   /**
    * Creates a key with create time as 'now'.
    * @param index The associated index.
    * @param uid The user id.
    * @param key The key.
    */
   public StoredKey(final String index, final String uid, final MetricKey key, final String title) {
      this(index, uid, key, new Date(), title);
   }

   /**
    * Gets this stored key as a JSON object.
    * @return The JSON as a string.
    * @throws IOException on JSON generation error.
    */
   public final String getAsJSON() throws IOException {
      StringWriter writer = new StringWriter();
      JsonGenerator generator = parserFactory.createGenerator(writer);
      generateJSON(generator);
      generator.flush();
      generator.close();
      return writer.toString();
   }

   /**
    * Creates a stored key from JSON.
    * @param obj The JSON object.
    * @return The stored key.
    * @throws IOException on parse error.
    */
   public static final StoredKey fromJSON(final JsonNode obj) throws IOException {

      String uid = obj.has("uid") ? obj.get("uid").textValue() : null;
      String index = obj.has("index") ? obj.get("index").textValue() : null;
      String application = obj.has("application") ? obj.get("application").textValue() : null;
      String host = obj.has("host") ? obj.get("host").textValue() : null;
      String instance = obj.has("instance") ? obj.get("instance").textValue() : null;
      String name = obj.has("name") ? obj.get("name").textValue() : null;
      String field = obj.has("field") ? obj.get("field").textValue() : null;
      MetricKey key = new MetricKey(name, application, host, instance, field);

      String createdString = (obj.has("created") ? obj.get("created").textValue() : "").trim();
      Date created = createdString.isEmpty() ? new Date() : new Date(DateTime.fromStandardFormat(createdString));

      String title = obj.has("title") ? obj.get("title").textValue() : null;

      return new StoredKey(index, uid, key, created, title);
   }

   /**
    * Generates JSON for this key.
    * @param generator The generator.
    * @throws java.io.IOException on generation error.
    */
   public void generateJSON(final JsonGenerator generator) throws IOException {
      generator.writeStartObject();
      generator.writeStringField("id", id);
      generator.writeStringField("uid", uid);
      generator.writeStringField("index", index);
      generator.writeStringField("application", key.application);
      generator.writeStringField("host", key.host);
      generator.writeStringField("instance", key.instance);
      generator.writeStringField("name", key.name);
      generator.writeStringField("field", key.field);
      generator.writeStringField("created", DateTime.standardFormat(this.createTime));
      generator.writeStringField("title", title);
      generator.writeEndObject();
   }

   @Override
   public boolean equals(final Object o) {
      if(o instanceof StoredKey) {
         StoredKey other = (StoredKey)o;
         return id.equals(other.id);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return id.hashCode();
   }

   /**
    * The index.
    */
   public final String index;

   /**
    * The user id.
    */
   public final String uid;

   /**
    * The unique key id.
    */
   public final String id;

   /**
    * The metric key.
    */
   public final MetricKey key;

   /**
    * The create time.
    */
   public final Date createTime;

   /**
    * An optional title.
    */
   public final String title;

}
