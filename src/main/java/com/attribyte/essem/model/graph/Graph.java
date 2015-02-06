package com.attribyte.essem.model.graph;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * A graph as an immutable list of points.
 */
public class Graph {

   /**
    * Creates the graph.
    * @param points The list of points.
    */
   public Graph(final List<Point> points) {
      this.points = points != null ? ImmutableList.copyOf(points) : ImmutableList.<Point>of();
   }

   /**
    * The immutable list of points.
    */
   public final ImmutableList<Point> points;

}
