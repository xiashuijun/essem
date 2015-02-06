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

public class DownsampleFunction {

   /**
    * Creates the functions.
    * @param displayName The display name.
    * @param functionName The function name.
    * @param active Is the function the active function.
    */
   public DownsampleFunction(final String displayName, final String functionName, final boolean active) {
      this.displayName = displayName;
      this.functionName = functionName;
      this.active = active;
   }

   /**
    * Activates this function.
    * @return A copy of this function set to be active.
    */
   public DownsampleFunction activate() {
      return new DownsampleFunction(this.displayName, this.functionName, true);
   }

   /**
    * Deactivates this function.
    * @return A copy of this function set to be inactive.
    */
   public DownsampleFunction deactivate() {
      return new DownsampleFunction(this.displayName, this.functionName, false);
   }

   /**
    * The name for display.
    */
   public final String displayName;

   /**
    * The function name.
    */
   public final String functionName;

   /**
    * Is this the active function?
    */
   public final boolean active;
}
