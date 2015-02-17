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

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import org.apache.commons.codec.binary.Base64;
import org.attribyte.api.http.RequestBuilder;
import org.attribyte.util.EncodingUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Implements "Basic" auth.
 */
public class BasicAuth implements IndexAuthorization {

   /**
    * The authorization header.
    */
   public static final String AUTHORIZATION_HEADER = "Authorization";

   /**
    * The WWW-Authenticate response header.
    */
   public static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";

   /**
    * A hash function for the authorization value.
    */
   private final HashFunction hashFunction = Hashing.md5();

   /**
    * Create from properties of the form username:password=index0,index1,...
    * @param props The properties.
    */
   public BasicAuth(final Properties props) {

      Map<HashCode, Set<String>> authMap = Maps.newHashMapWithExpectedSize(16);
      Set<String> indexSet = Sets.newLinkedHashSetWithExpectedSize(16);
      Splitter indexSplitter = Splitter.on(CharMatcher.anyOf(", \t")).trimResults().omitEmptyStrings();

      Enumeration names = props.propertyNames();
      while(names.hasMoreElements()) {

         String key = ((String)names.nextElement()).trim();
         String up = key.replace('|', ':');
         String expectedValue = "Basic " + EncodingUtil.encodeBase64(up.getBytes(Charsets.UTF_8));

         HashCode expectedValueCode = hashFunction.hashString(expectedValue, Charsets.UTF_8);
         Set<String> allowSet = authMap.get(expectedValueCode);
         if(allowSet == null) {
            allowSet = Sets.newLinkedHashSetWithExpectedSize(16);
            authMap.put(expectedValueCode, allowSet);
         }

         String indexStr = props.getProperty(key).trim();
         for(String index : indexSplitter.split(indexStr)) {
            if(!index.equals("*")) {
               indexSet.add(index);
            }
            allowSet.add(index);
         }
      }

      this.authorizedIndexes = ImmutableSet.copyOf(indexSet);

      ImmutableMap.Builder<HashCode, ImmutableSet<String>> builder = ImmutableMap.builder();
      for(Map.Entry<HashCode, Set<String>> entry : authMap.entrySet()) {
         builder.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
      }
      this.authMap = builder.build();
   }

   @Override
   public boolean isAuthorized(final String index, final HttpServletRequest request) {
      String auth = request.getHeader(AUTHORIZATION_HEADER);
      if(auth == null) {
         return false;
      } else {
         HashCode authHash = hashFunction.hashString(auth, Charsets.UTF_8);
         Set<String> allowedIndexes = authMap.get(authHash);
         return allowedIndexes != null && (allowedIndexes.contains("*") || allowedIndexes.contains(index));
      }
   }

   @Override
   public Auth getAuth(final String index, final HttpServletRequest request) {
      String auth = request.getHeader(AUTHORIZATION_HEADER);
      if(auth == null) {
         return Auth.UNAUTHORIZED;
      } else {
         HashCode authHash = hashFunction.hashString(auth, Charsets.UTF_8);
         Set<String> allowedIndexes = authMap.get(authHash);
         boolean authorized = allowedIndexes != null && (allowedIndexes.contains("*") || allowedIndexes.contains(index));
         if(authorized) {
            if(!auth.toLowerCase().startsWith("basic ")) {
               return Auth.UNAUTHORIZED; //Should never happen...
            }

            auth = auth.substring(6).trim();
            String upass = new String(BaseEncoding.base64().decode(auth), Charsets.US_ASCII).trim();

            int pos = upass.indexOf(':');
            if(pos < 1) {
               return Auth.UNAUTHORIZED; //Should never happen...
            } else {
               return new Auth(upass.substring(0, pos), true);
            }
         } else {
            return Auth.UNAUTHORIZED;
         }
      }
   }

   @Override
   public void sendUnauthorized(final String index, final HttpServletResponse response) throws IOException {
      response.setHeader(WWW_AUTHENTICATE_HEADER, "Basic realm=" + "\"" + index + "\"");
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
   }

   @Override
   public Collection<String> authorizedIndexes() {
      return authorizedIndexes;
   }

   /**
    * Gets the appropriate header value to be sent with basic auth.
    * @param username The username.
    * @param password The password.
    * @return The auth header value.
    */
   public static final String basicAuthValue(final String username, final String password) {
      String up = username + ":" + password;
      return "Basic " + EncodingUtil.encodeBase64(up.getBytes(Charsets.UTF_8));
   }

   /**
    * Adds the auth value to a request builder.
    * @param requestBuilder The builder.
    * @param authValue The auth value to add.
    * @return The input builder.
    */
   public static final RequestBuilder addAuth(final RequestBuilder requestBuilder, final String authValue) {
      if(Strings.nullToEmpty(authValue).length() > 0) {
         requestBuilder.addHeader(AUTHORIZATION_HEADER, authValue);
      }
      return requestBuilder;
   }


   private final ImmutableMap<HashCode, ImmutableSet<String>> authMap;
   private final ImmutableSet<String> authorizedIndexes;
}