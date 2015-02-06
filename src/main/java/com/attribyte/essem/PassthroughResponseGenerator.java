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

import com.attribyte.essem.query.GraphQuery;
import com.attribyte.essem.query.NameQuery;
import com.attribyte.essem.query.StatsQuery;
import org.attribyte.api.http.Response;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumSet;

/**
 * Passes the response from the back-end to the client with no modification.
 */
public class PassthroughResponseGenerator implements ResponseGenerator {

   @Override
   public boolean generateGraph(final GraphQuery graphQuery,
                                final Response esResponse,
                                final EnumSet<Option> options,
                                final HttpServletResponse response) throws IOException {
      response.setContentType("application/json");
      response.setStatus(esResponse.getStatusCode());
      response.getOutputStream().write(esResponse.getBody().toByteArray());
      return true;
   }

   @Override
   public boolean generateNames(final NameQuery nameQuery,
                                final Response esResponse,
                                final EnumSet<Option> options,
                                final HttpServletResponse response) throws IOException {
      response.setContentType("application/json");
      response.setStatus(esResponse.getStatusCode());
      response.getOutputStream().write(esResponse.getBody().toByteArray());
      return true;
   }

   @Override
   public boolean generateStats(final StatsQuery statsQuery,
                                final Response esResponse,
                                final EnumSet<Option> options,
                                final HttpServletResponse response) throws IOException {
      response.setContentType("application/json");
      response.setStatus(esResponse.getStatusCode());
      response.getOutputStream().write(esResponse.getBody().toByteArray());
      return true;
   }
}