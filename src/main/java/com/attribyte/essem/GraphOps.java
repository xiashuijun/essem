package com.attribyte.essem;

import com.attribyte.essem.model.graph.Graph;
import com.attribyte.essem.model.graph.Point;

/**
 * Operations applied to graphs.
 */
public class GraphOps {

   /**
    * Determine if a graph is "boring".
    * <p>
    * A boring graph is one where all values are essentially equal.
    * </p>
    * @param graph The graph.
    * @param resolution The resolution for equality.
    * @return Is the graph boring?
    */
   public static final boolean isBoring(final Graph graph, final double resolution) {
      if(graph.points.size() < 2) {
         return true;
      } else {
         final Point firstPoint = graph.points.get(0);
         if(!firstPoint.isIntegralNumber()) {
            final double test = firstPoint.asDouble();
            for(Point point : graph.points) {
               if(Math.abs(point.asDouble() - test) > resolution) {
                  return false;
               }
            }
            return true;
         } else { //Ignore resolution...
            final long test = firstPoint.asLong();
            for(Point point : graph.points) {
               if(point.asLong() != test) {
                  return false;
               }
            }
            return true;
         }
      }
   }
}
