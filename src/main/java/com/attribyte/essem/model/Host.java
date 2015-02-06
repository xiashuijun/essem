/*
 * Copyright 2014, 2015 Attribyte, LLC
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import edu.emory.mathcs.backport.java.util.Collections;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * A host.
 */
public class Host {

   /**
    * Compares hosts by name.
    */
   public static final Comparator<Host> alphaComparator = new Comparator<Host>() {
      @Override
      public int compare(final Host o1, final Host o2) {
         return o1.name.compareTo(o2.name);
      }
   };

   /**
    * Creates a host.
    * @param name The host name.
    * @param instances A set of instances associated with the host.
    */
   public Host(final String name, final Collection<String> instances) {
      this.name = name;
      this.instances = instances != null ? ImmutableSet.copyOf(instances) : ImmutableSet.<String>of();
   }

   /**
    * Are there multiple instances for this host?
    * @return If more than one instance, <code>true</code> otherwise <code>false</code>.
    */
   public boolean isMultiInstance() {
      return this.instances.size() > 1;
   }

   /**
    * Gets a list containing instances (if any) sorted by name.
    * @param sort The sort order.
    * @return The sorted list.
    */
   public List<String> getInstances(Sort sort) {
      List<String> sortedInstances = Lists.newArrayList(this.instances);
      Collections.sort(sortedInstances);
      if(sort == Sort.DESC) {
         Collections.reverse(sortedInstances);
      }
      return sortedInstances;
   }

   @Override
   public int hashCode() {
      return name.hashCode();
   }

   @Override
   public boolean equals(Object o) {
      return o instanceof Host && ((Host)o).name.equals(name);
   }

   /**
    * The host name.
    */
   public final String name;

   /**
    * The immutable set of instances associated with this host.
    */
   public final ImmutableSet<String> instances;
}