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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

/**
 * Allows authorization to configured for indexes.
 */
public interface IndexAuthorization {

   /**
    * Info about authorization.
    */
   public static class Auth {

      /**
       * The unauthorized user.
       */
      public static final Auth UNAUTHORIZED = new Auth(null, false);

      /**
       * A 'system' user.
       */
      public static final Auth SYSTEM = new Auth("__SYSTEM__", true, true);

      private Auth(final String uid, final boolean isAuthorized, final boolean isSystem) {
         this.uid = uid;
         this.isAuthorized = isAuthorized;
         this.isSystem = isSystem;
      }

      /**
       * Creates auth.
       * @param uid The user id.
       */
      public Auth(final String uid, final boolean isAuthorized) {
         this.uid = uid;
         this.isAuthorized = isAuthorized;
         this.isSystem = false;
      }

      /**
       * The user id.
       */
      public final String uid;

      /**
       * Is the user authorized?
       */
      public final boolean isAuthorized;

      /**
       * Is this the global system user?
       */
      public final boolean isSystem;
   }

   /**
    * Determine if access to the index is authorized.
    * @param index The index.
    * @param request The request.
    * @return Is access authorized?
    */
   public boolean isAuthorized(final String index, final HttpServletRequest request);

   /**
    * Determine if access to the index is authorized.
    * @param index The index.
    * @param request The request.
    * @return The authorization info.
    */
   public Auth getAuth(final String index, final HttpServletRequest request);

   /**
    * Sends an appropriate "unauthorized" response.
    * @param index The index.
    * @param response The response.
    * @throws IOException on output error.
    */
   public void sendUnauthorized(final String index, final HttpServletResponse response) throws IOException;


   /**
    * The collection of indexes with authorization enabled.
    * @return The collection of index names.
    */
   public Collection<String> authorizedIndexes();

}
