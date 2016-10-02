package com.attribyte.essem.es;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.List;

public class Sort extends QueryComponent {

   /**
    * Sort direction.
    */
   public enum Direction {

      /**
       * Ascending.
       */
      ASC,

      /**
       * Descending.
       */
      DESC
   }

   /**
    * Creates a sort with a single component.
    * @param component The component.
    */
   public Sort(final Sort.Component component) {
      this(ImmutableList.of(component));
   }

   /**
    * Creates a sort with one or more components.
    * @param components The list of components.
    */
   public Sort(final List<Sort.Component> components) {
      this.components = components != null ? ImmutableList.copyOf(components) : ImmutableList.<Sort.Component>of();
   }

   /**
    * A sort component.
    */
   public static interface Component {
      public void generate(final JsonGenerator generator) throws IOException;
   }

   /**
    * Specify sort for a field expected to have a single value.
    */
   public static class SingleFieldSort implements Sort.Component {

      public SingleFieldSort(final String field, final Direction direction) {
         this.field = field;
         this.direction = direction;
      }

      public void generate(final JsonGenerator generator) throws IOException {
         generator.writeStartObject();
         {
            generator.writeObjectFieldStart(field);
            {
               switch(direction) {
                  case DESC:
                     generator.writeStringField("order", "desc");
                     break;
                  default:
                     generator.writeStringField("order", "asc");
                     break;
               }
            }
            generator.writeEndObject();
         }
         generator.writeEndObject();
      }

      public final String field;
      public final Direction direction;
   }

   @Override
   public void generate(final JsonGenerator generator) throws IOException {
      if(components.size() > 0) {
         generator.writeArrayFieldStart("sort");
         {
            for(Sort.Component component : components) {
               component.generate(generator);
            }
         }
         generator.writeEndArray();
      }
   }

   public final ImmutableList<Sort.Component> components;
}
