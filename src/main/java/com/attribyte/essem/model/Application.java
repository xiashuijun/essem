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

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * An immutable application.
 */
public class Application {

   /**
    * The maximum number of characters for metric name prefix
    * discrimination.
    */
   private static final int MAX_PREFIX_RESOLUTION = 16;

   /**
    * Creates an application with no assigned metrics.
    * @param name The application name.
    * @param hosts The collection of hosts for the application.
    */
   public Application(final String name, final String index, final Collection<Host> hosts) {
      this(name, index, hosts, null);
   }

   /**
    * Creates an application.
    * @param name The name.
    * @param index The associated index, if any.
    * @param hosts The collection of hosts.
    * @param metrics The collection of metrics.
    */
   public Application(final String name,
                      final String index,
                      final Collection<Host> hosts,
                      final Collection<Metric> metrics) {
      this.name = name;
      this.index = index;
      this.hosts = hosts != null ? ImmutableSet.copyOf(hosts) : ImmutableSet.<Host>of();
      this.metrics = metrics != null ? ImmutableSet.copyOf(metrics) : ImmutableSet.<Metric>of();
      this.metricTypeMap = buildTypeMap(metrics);

      ImmutableMap.Builder<String, Metric> mapBuilder = ImmutableMap.builder();
      Map<String, List<Metric>> prefixMapBuilder = Maps.newHashMapWithExpectedSize(1024);

      for(Metric metric : this.metrics) {
         mapBuilder.put(metric.name, metric);
         addPrefix(prefixMapBuilder, metric);
      }

      for(List<Metric> sorted : prefixMapBuilder.values()) {
         Collections.sort(sorted, Metric.alphaComparator);
      }

      this.metricNameMap = mapBuilder.build();
      this.metricNamePrefixMap = ImmutableMap.copyOf(prefixMapBuilder);
   }

   private ImmutableMap<Metric.Type, ImmutableSet<Metric>> buildTypeMap(final Collection<Metric> metrics) {
      if(metrics != null && metrics.size() > 0) {
         Map<Metric.Type, ImmutableSet.Builder<Metric>> builderMap = Maps.newHashMapWithExpectedSize(metrics.size());

         for(Metric metric : metrics) {
            ImmutableSet.Builder<Metric> setBuilder = builderMap.get(metric.type);
            if(setBuilder == null) {
               setBuilder = ImmutableSet.builder();
               builderMap.put(metric.type, setBuilder);
            }
            setBuilder.add(metric);
         }

         ImmutableMap.Builder<Metric.Type, ImmutableSet<Metric>> builder = ImmutableMap.builder();
         for(Map.Entry<Metric.Type, ImmutableSet.Builder<Metric>> entry : builderMap.entrySet()) {
            builder.put(entry.getKey(), entry.getValue().build());
         }

         return builder.build();

      } else {
         return ImmutableMap.of();
      }
   }

   /**
    * Split names for prefix matching.
    */
   private static Splitter nameSplitter = Splitter.on(CharMatcher.anyOf("-_. \t")).omitEmptyStrings().trimResults();

   private void addPrefix(final Map<String, List<Metric>> prefixMap,
                          final Metric metric) {
      for(int i = 0; i < MAX_PREFIX_RESOLUTION; i++) {
         for(String matchName : nameSplitter.split(metric.name)) {
            if(matchName.length() > i) {
               String prefix = matchName.substring(0, i + 1).toLowerCase();
               List<Metric> currList = prefixMap.get(prefix);
               if(currList == null) {
                  currList = Lists.newArrayListWithExpectedSize(4);
                  prefixMap.put(prefix, currList);
               }
               currList.add(metric);
            }
         }
      }
   }

   /**
    * Adds metrics to this application.
    * @param metrics The application with new metrics added.
    */
   public Application withMetrics(final Collection<Metric> metrics) {
      return new Application(name, index, hosts, metrics);
   }

   /**
    * Gets an immutable list of all metrics that match a prefix (up to a limit).
    * @param prefix The prefix.
    * @param type The type. May be <code>null</code> or <code>INVALID</code>.
    * @return The list of matching metrics.
    */
   public List<Metric> matchPrefix(String prefix, Metric.Type type) {

      if(prefix.length() > MAX_PREFIX_RESOLUTION) {
         prefix = prefix.substring(0, MAX_PREFIX_RESOLUTION);
      }

      EnumSet<Metric.Type> types = (type == null || type == Metric.Type.UNKNOWN) ? null : EnumSet.of(type);

      List<Metric> matches = metricNamePrefixMap.get(prefix.toLowerCase());
      if(types != null) {
         matches = ImmutableList.copyOf(Metric.keep(matches, types));
      }

      return matches != null ? matches : ImmutableList.<Metric>of();
   }

   @Override
   public int hashCode() {
      return Objects.hash(index, name);
   }

   @Override
   public boolean equals(Object o) {
      return o instanceof Application &&
              Objects.equals(((Application)o).name, name) &&
              Objects.equals(((Application)o).index, index);
   }

   /**
    * Gets hosts sorted by name.
    * @return The list of hosts.
    */
   public List<Host> getHostList() {
      return getHosts(Sort.ASC);
   }

   /**
    * Gets a list of hosts hosts sorted by name.
    * @param sort The sort order.
    * @return The sorted list of hosts.
    */
   public List<Host> getHosts(final Sort sort) {
      List<Host> sorted = Lists.newArrayList(hosts);
      switch(sort) {
         case ASC:
            Collections.sort(sorted, Host.alphaComparator);
            break;
         default:
            Collections.sort(sorted, Collections.reverseOrder(Host.alphaComparator));
            break;
      }
      return sorted;
   }

   /**
    * Are there multiple hosts for this application?
    * @return If more than one host, <code>true</code> otherwise <code>false</code>.
    */
   public boolean isMultiHost() {
      return hosts.size() > 1;
   }

   /**
    * Counts the number of hosts.
    * @return The number of hosts.
    */
   public int getHostCount() {
      return hosts.size();
   }

   /**
    * Counts the number of metrics.
    * @return The number of metrics.
    */
   public int getMetricsCount() {
      return metrics.size();
   }

   /**
    * Gets a sorted list of all metrics.
    * @param sort The sort order.
    * @return The sorted list.
    */
   public List<Metric> getMetrics(final Sort sort) {
      return sort(this.metrics, sort);
   }

   /**
    * Gets a sorted list of a specific metric type.
    * @param type The type.
    * @param sort The sort order.
    * @return The immutable set of metrics.
    */
   public List<Metric> getMetrics(final Metric.Type type, final Sort sort) {
      return sort(metricTypeMap.get(type), sort);
   }

   /**
    * Gets a metric by name.
    * @param name The metric name.
    * @return The metric or <code>null</code> if not found.
    */
   public Metric getMetric(final String name) {
      return metricNameMap.get(name);
   }

   /**
    * Sorts a list of metrics.
    * @param metrics The metrics.
    * @param sort The sort order.
    * @return The sorted list.
    */
   private List<Metric> sort(final Collection<Metric> metrics, final Sort sort) {
      if(metrics == null) return Collections.emptyList();
      List<Metric> sorted = Lists.newArrayList(metrics);
      switch(sort) {
         case ASC:
            Collections.sort(sorted, Metric.alphaComparator);
            break;
         default:
            Collections.sort(sorted, Collections.reverseOrder(Metric.alphaComparator));
            break;
      }

      return sorted;
   }

   /**
    * The application name.
    */
   public final String name;

   /**
    * The index associated with the application.
    */
   public final String index;

   /**
    * The immutable set of hosts for this application.
    */
   public final ImmutableSet<Host> hosts;

   /**
    * The immutable set of metrics for this application.
    */
   public final ImmutableSet<Metric> metrics;

   /**
    * An immutable map of metric vs name for this application.
    */
   private final ImmutableMap<String, Metric> metricNameMap;

   /**
    * A map of metrics vs type.
    */
   private final ImmutableMap<Metric.Type, ImmutableSet<Metric>> metricTypeMap;

   /**
    * A map of metrics vs (up to a limit) a prefix of their name.
    */
   private final ImmutableMap<String, List<Metric>> metricNamePrefixMap;

}
