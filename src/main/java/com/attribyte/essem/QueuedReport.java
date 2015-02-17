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

import org.attribyte.essem.ReportProtos;

/**
 * A report in the queue.
 */
public class QueuedReport {

   /**
    * Creates a report.
    * @param index The index.
    * @param report The failed report.
    */
   public QueuedReport(final String index, final ReportProtos.EssemReport report) {
      this.index = index;
      this.report = report;
      this.cause = null;
      this.failedCount = 0;
   }

   /**
    * Creates a report.
    * @param report The failed report.
    * @param cause The cause.
    * @param failedCount The number of times this report has failed.
    */
   public QueuedReport(QueuedReport report, final Throwable cause, final int failedCount) {
      this.index = report.index;
      this.report = report.report;
      this.cause = cause;
      this.failedCount = failedCount;
   }

   /**
    * Creates a report.
    * @param report The failed report.
    * @param cause The cause.
    */
   public QueuedReport(QueuedReport report, final Throwable cause) {
      this(report, cause, 1);
   }

   /**
    * Creates a copy of this report with the failure count incremented by one.
    * @param cause The new cause.
    * @return The new report.
    */
   public QueuedReport incrementCount(final Throwable cause) {
      return new QueuedReport(this, cause, this.failedCount + 1);
   }

   final String index;

   /**
    * The report that failed.
    */
   final ReportProtos.EssemReport report;

   /**
    * The failure cause.
    */
   final Throwable cause;

   /**
    * The number of times this report has failed.
    */
   final int failedCount;
}