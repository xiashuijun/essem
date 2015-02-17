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

package com.attribyte.essem.model.graph;

import com.google.common.base.Strings;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * A metric key.
 * <p>
 * - Empty and null components are equivalent for equality determination.
 * - Components are lower-cased for equality determination.
 * </p>
 */
public class MetricKey {

   /**
    * Parses the components of a key from request parameters.
    * @param request The request.
    * @param app The application name to use (overrides any parameter).
    * @return The parsed key.
    */
   public static MetricKey parseKey(final HttpServletRequest request, String app) {

      if(app == null) {
         app = request.getParameter("app");
         if(app == null) app = request.getParameter("application");
      }

      return new MetricKey(request.getParameter("name"), app,
              request.getParameter("host"), request.getParameter("instance"),
              request.getParameter("field"));
   }

   /**
    * Parses the components of a key from request parameters.
    * @param request The request.
    * @return The parsed key.
    */
   public static MetricKey parseKey(final HttpServletRequest request) {
      return parseKey(request, null);
   }

   /**
    * Convenience builder for immutable instances of key.
    */
   public static class Builder {


      public String getName() {
         return name;
      }

      public Builder setName(final String name) {
         this.name = name;
         return this;
      }

      public String getApplication() {
         return application;
      }

      public Builder setApplication(final String application) {
         this.application = application;
         return this;
      }

      public String getHost() {
         return host;
      }

      public Builder setHost(final String host) {
         this.host = host;
         return this;
      }

      public String getInstance() {
         return instance;
      }

      public Builder setInstance(final String instance) {
         this.instance = instance;
         return this;
      }

      public String getField() {
         return field;
      }

      public Builder setField(final String field) {
         this.field = field;
         return this;
      }

      /**
       * Builds an immutable key.
       * @return The key.
       */
      public MetricKey build() {
         return new MetricKey(name, application, host, instance, field);
      }

      private String name;
      private String application;
      private String host;
      private String instance;
      private String field;
   }

   /**
    * Creates a key builder.
    * @return A builder.
    */
   public static Builder builder() {
      return new Builder();
   }

   /**
    * Creates a key for an application-specific field.
    * @param name The metric name.
    * @param application The application name.
    * @param field The field.
    * @return The immutable key.
    */
   public static MetricKey createApplicationField(final String name, final String application, final String field) {
      return new MetricKey(name, application, null, null, field);
   }

   /**
    * Creates a key from an existing key with the field changed.
    * @param proto The prototype field.
    * @param field The field to add.
    */
   public MetricKey(final MetricKey proto, final String field) {
      this(proto.name, proto.application, proto.host, proto.instance, field);
   }

   /**
    * Creates a key with no specified field.
    * @param name The metric name.
    * @param application The application name.
    * @param host The host name.
    * @param instance The instance name.
    */
   public MetricKey(final String name, final String application, final String host,
                    final String instance) {
      this(name, application, host, instance, null);
   }

   /**
    * Creates a key with a field.
    * @param name The metric name.
    * @param application The application name.
    * @param host The host name.
    * @param instance The instance name.
    * @param field The field.
    */
   public MetricKey(final String name, final String application, final String host,
                    final String instance, final String field) {
      this.name = Strings.nullToEmpty(name);
      this.application = Strings.nullToEmpty(application);
      this.host = Strings.nullToEmpty(host);
      this.instance = Strings.nullToEmpty(instance);
      this.field = Strings.nullToEmpty(field);
      this.hashCode = Objects.hash(
              this.name.toLowerCase(),
              this.application.toLowerCase(),
              this.host.toLowerCase(),
              this.instance.toLowerCase(),
              this.field.toLowerCase()
      );
      this.hasApplication = this.application.length() > 0;
      this.hasName = this.name.length() > 0;
      this.hasHost = this.host.length() > 0;
      this.hasInstance = this.instance.length() > 0;
      this.hasField = this.instance.length() > 0;
   }

   @Override
   public int hashCode() {
      return hashCode;
   }

   @Override
   public boolean equals(Object o) {
      if(o instanceof MetricKey) {
         MetricKey other = (MetricKey)o;
         return eq(name, other.name) && eq(application, other.application) && eq(host, other.host) &&
                 eq(instance, other.instance) && eq(field, other.field);
      } else {
         return false;
      }
   }

   /**
    * Determine if two components are equal.
    * @param str0 The first string.
    * @param str1 The second string.
    * @return Are they equal according to key equality rules?
    */
   private static boolean eq(final String str0, final String str1) {
      return str0.equalsIgnoreCase(str1);
   }

   /**
    * The metric name.
    */
   public final String name;

   /**
    * Is the name set?
    */
   public final boolean hasName;

   /**
    * The application name.
    */
   public final String application;

   /**
    * Is the application name set?
    */
   public final boolean hasApplication;

   /**
    * The host.
    */
   public final String host;

   /**
    * Is the host set?
    */
   public final boolean hasHost;

   /**
    * The instance name.
    */
   public final String instance;

   /**
    * Is the instance name set?
    */
   public final boolean hasInstance;

   /**
    * The field name.
    */
   public final String field;

   /**
    * Is the field name set?
    */
   public final boolean hasField;

   private final int hashCode;
}
