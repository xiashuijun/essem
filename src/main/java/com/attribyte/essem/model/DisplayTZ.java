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

package com.attribyte.essem.model;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * A timezone for display.
 */
public class DisplayTZ {

   /**
    * Split with '='.
    */
   private static Splitter eqSplitter = Splitter.on('=').omitEmptyStrings().trimResults();

   /**
    * Parse a file of the format [id] = [name].
    * @param file The file to parse.
    * @return The list of display zones.
    * @throws IOException on parse error.
    */
   public static final List<DisplayTZ> parse(final File file) throws IOException {
      List<String> lines = CharStreams.readLines(new FileReader(file));
      List<DisplayTZ> tzList = Lists.newArrayListWithExpectedSize(lines.size());
      for(String line : lines) {
         line = line.trim();
         if(line.length() > 0 && !line.startsWith("#")) {
            Iterator<String> iter = eqSplitter.split(line).iterator();
            if(iter.hasNext()) {
               String id = iter.next();
               if(iter.hasNext()) {
                  String display = iter.next();
                  tzList.add(new DisplayTZ(id, display));
               }
            }
         }
      }
      return tzList;
   }

   /**
    * Creates the zone.
    * @param id The zone id.
    * @param display The name for display.
    */
   public DisplayTZ(final String id, final String display) {
      this.id = id;
      this.display = display;
   }

   /**
    * The zone id.
    */
   public final String id;

   /**
    * The name for display.
    */
   public final String display;
}
