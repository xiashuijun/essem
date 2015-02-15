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

package com.attribyte.essem;

import com.attribyte.essem.model.StoredKey;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;

/**
 * Parse keys from ES response that results from <code>StoredKeyQuery</code>.
 */
public class StoredKeyParser {

   /**
    * Parses a stored key response.
    * @param esObject The ES response object.
    * @return The list of keys.
    */
   public static List<StoredKey> parseKeys(ObjectNode esObject) throws IOException {

      List<StoredKey> keyList = Lists.newArrayListWithExpectedSize(16);
      JsonNode hitsObj = esObject.get("hits");
      if(hitsObj != null) {
         JsonNode hitsArr = hitsObj.get("hits");
         if(hitsArr != null) {
            for(JsonNode hitObj : hitsArr) {
               JsonNode fieldsObj = hitObj.get("fields");
               if(fieldsObj != null) {
                  keyList.add(StoredKey.fromJSON(fieldsObj));
               }
            }
         }
      }
      return keyList;
   }
}