package com.attribyte.essem.model;

import com.attribyte.essem.util.Util;
import com.google.common.collect.ImmutableList;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class Dashboard {

   public Dashboard(HttpServletRequest request) {
      id = Util.getParameter(request, "id", "");
      String[] tagArr = request.getParameterValues("tag");
      tags = id.isEmpty() ? ImmutableList.copyOf(tagArr) : ImmutableList.<String>of();

      withTitles = Util.getParameter(request, "withTitles", true);
      tz = Util.getParameter(request, "tz", "");
      width = Util.getParameter(request, "width", 0);
      height = Util.getParameter(request, "height", 0);

      autoUpdateSeconds = Util.getParameter(request, "updateSeconds", 0);
      smallBlockGridColumns = Util.getParameter(request, "sbg", 2);
      largeBlockGridColumns = Util.getParameter(request, "lbg", 4);
      displayGrid = Util.getParameter(request, "grid", false);
   }

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
