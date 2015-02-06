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

import com.codahale.metrics.MetricSet;
import com.google.common.base.Function;

import java.io.IOException;
import java.util.Collection;

/**
 * Report metrics. Instances <em>must be thread-safe.</em>
 */
public interface Reporter extends MetricSet {

   /**
    * Creates a new metrics store.
    * @param storeName The store name.
    * @return Was the index created.
    * @throws IOException on index create error.
    */
   public boolean createStore(final String storeName) throws IOException;

   /**
    * Accepts a collection of reports for processing.
    * @param reports The collection of reports.
    * @param failedFunction A function called when a report fails.
    */
   public void report(final Collection<QueuedReport> reports,
                      final Function<QueuedReport, Boolean> failedFunction);

   /**
    * Accepts a previously failed report for retry.
    * @param failedReport The failed report.
    * @param failed A function called if this report fails again.
    */
   public void retry(final QueuedReport failedReport,
                     final Function<QueuedReport, Boolean> failed);
}
