
package org.maven.ide.eclipse.util;

/*
 * Licensed to the Codehaus Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

public class Tracer {
  private Tracer() {
  }

  public static void trace(ITraceable target, String message, Object param) {
    if(target.isTraceEnabled()) {
      System.out.println(target.getClass().getName() + ": " + message + (param != null ? ": [" + param + "]" : ""));
    }
  }

  public static void trace(ITraceable target, String message, Object param, Throwable e) {
    trace(target, message, param);
    if(target.isTraceEnabled() && e != null) {
      e.printStackTrace(System.out);
    }
  }

  public static void trace(ITraceable target, String message) {
    trace(target, message, null);
  }

}
