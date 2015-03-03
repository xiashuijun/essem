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

import com.attribyte.essem.util.Util;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.attribyte.util.URIEncoder;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class Dashboard {

   public static class Builder {

      /**
       * Creates an empty builder.
       */
      public Builder() {
      }

      /**
       * Creates a builder from a dashboard.
       * @param other The other dashboard.
       */
      public Builder(Dashboard other) {
         this.id = other.id;
         this.tags = Lists.newArrayList(other.tags);
         this.withTitles = other.withTitles;
         this.tz = other.displayTZ;
         this.width = other.width;
         this.height = other.height;
         this.autoUpdateSeconds = other.autoUpdateSeconds;
         this.displayGrid = other.displayGrid;
         this.smallBlockGridColumns = other.smallBlockGridColumns;
         this.largeBlockGridColumns = other.largeBlockGridColumns;
      }

      public Builder setId(final String id) {
         this.id = id;
         return this;
      }

      public Builder setTags(final List<String> tags) {
         this.tags = tags;
         return this;
      }

      public Builder setWithTitles(final boolean withTitles) {
         this.withTitles = withTitles;
         return this;
      }

      public Builder setTz(final DisplayTZ tz) {
         this.tz = tz;
         return this;
      }

      public Builder setWidth(final int width) {
         this.width = width;
         return this;
      }

      public Builder setHeight(final int height) {
         this.height = height;
         return this;
      }

      public Builder setAutoUpdateSeconds(final int autoUpdateSeconds) {
         this.autoUpdateSeconds = autoUpdateSeconds;
         return this;
      }

      public Builder setDisplayGrid(final boolean displayGrid) {
         this.displayGrid = displayGrid;
         return this;
      }

      public Builder setSmallBlockGridColumns(final int smallBlockGridColumns) {
         this.smallBlockGridColumns = smallBlockGridColumns;
         return this;
      }

      public Builder setLargeBlockGridColumns(final int largeBlockGridColumns) {
         this.largeBlockGridColumns = largeBlockGridColumns;
         return this;
      }

      private String id;
      private List<String> tags = Lists.newArrayListWithExpectedSize(4);
      private boolean withTitles;
      private DisplayTZ tz;
      private int width;
      private int height;
      private int autoUpdateSeconds;
      private boolean displayGrid;
      private int smallBlockGridColumns;
      private int largeBlockGridColumns;

      /**
       * Builds an immutable dashboard.
       * @return The dashboard.
       */
      public Dashboard build() {
         return new Dashboard(id, tags != null ? ImmutableList.copyOf(tags) : ImmutableList.<String>of(),
         withTitles, tz, width, height, autoUpdateSeconds, displayGrid,
                 smallBlockGridColumns, largeBlockGridColumns);
      }

   }

   public Dashboard(final String id, final ImmutableList<String> tags, final boolean withTitles,
                    final DisplayTZ tz,
                    final int width, final int height, final int autoUpdateSeconds, final boolean displayGrid,
                    final int smallBlockGridColumns, final int largeBlockGridColumns) {
      this.id = id;
      this.tags = tags;
      this.withTitles = withTitles;
      this.tz = tz.id;
      this.displayTZ = tz;
      this.width = width;
      this.height = height;
      this.autoUpdateSeconds = autoUpdateSeconds;
      this.displayGrid = displayGrid;
      this.smallBlockGridColumns = smallBlockGridColumns;
      this.largeBlockGridColumns = largeBlockGridColumns;
      this.queryString = buildQueryString();
   }

   public Dashboard(final HttpServletRequest request, final Map<String, DisplayTZ> zoneMap) {
      this.id = Util.getParameter(request, "id", "");
      this.tags = this.id.isEmpty() ? Util.getParameterValues(request, "tag") : ImmutableList.<String>of();

      this.withTitles = Util.getParameter(request, "withTitles", true);
      this.tz = Util.getParameter(request, "tz", TimeZone.getDefault().getID());

      DisplayTZ dtz = zoneMap.get(this.tz);
      this.displayTZ = dtz != null ? dtz : new DisplayTZ(this.tz, this.tz);
      this.width = Util.getParameter(request, "width", 0);
      this.height = Util.getParameter(request, "height", 0);

      this.autoUpdateSeconds = Util.getParameter(request, "updateSeconds", 0);
      this.smallBlockGridColumns = Util.getParameter(request, "sbg", 2);
      this.largeBlockGridColumns = Util.getParameter(request, "lbg", 4);
      this.displayGrid = Util.getParameter(request, "grid", false);
      this.queryString = buildQueryString();
   }

   private String buildQueryString() {
      StringBuilder buf = new StringBuilder();
      buf.append("?");

      if(!Strings.isNullOrEmpty(id)) {
         buf.append("id=").append(id);
      }

      for(String tag : tags) {
         buf.append("&tag=").append(URIEncoder.encodeQueryString(tag));
      }

      buf.append("&withTitles=").append(withTitles);
      buf.append("&width=").append(width);
      buf.append("&height=").append(height);
      buf.append("&updateSeconds=").append(autoUpdateSeconds);
      buf.append("&grid=").append(displayGrid);
      buf.append("&sbg=").append(smallBlockGridColumns);
      buf.append("&lbg=").append(largeBlockGridColumns);
      buf.append("&tz=").append(tz);
      return buf.toString();
   }

   private static Joiner tagJoiner = Joiner.on(',').skipNulls();

   /**
    * Gets tags as a comma-separated string.
    * @return The tag string.
    */
   public String getTagString() {
      if(tags.size() > 0) {
         return tagJoiner.join(tags);
      } else {
         return "Dashboard";
      }
   }

   /**
    * A query string that generates this dashboard.
    */
   public final String queryString;

   /**
    * A stored graph id, if any.
    */
   public final String id;

   /**
    * A list of tags, if any.
    */
   public final ImmutableList<String> tags;

   /**
    * Should titles be displayed.
    */
   public final boolean withTitles;

   /**
    * The time zone.
    */
   public final String tz;

   /**
    * The time zone for display
    */
   public final DisplayTZ displayTZ;

   /**
    * The width.
    */
   public final int width;

   /**
    * Is a custom width set?
    */
   public boolean hasCustomWidth() {
      return width > 0;
   }

   /**
    * The height.
    */
   public final int height;

   /**
    * Is a custom height set?
    */
   public boolean hasCustomHeight() {
      return height > 0;
   }

   /**
    * The auto-update/refresh time in seconds.
    */
   public final int autoUpdateSeconds;

   /**
    * Is auto-update enabled?
    */
   public boolean isAutoUpdate() {
      return autoUpdateSeconds > 0;
   }

   /**
    * Should the dashboard graphs be displayed as a grid?
    */
   public final boolean displayGrid;

   /**
    * How many columns for a small width?
    */
   public final int smallBlockGridColumns;

   /**
    * How many columns for a large width?
    */
   public final int largeBlockGridColumns;

}
