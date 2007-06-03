
package org.maven.ide.eclipse.embedder;

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

import java.util.Date;

import org.apache.maven.monitor.event.EventMonitor;


class ConsoleEventMonitor implements EventMonitor {
  private static final String PREFIX = "[INFO] ";

  private final boolean debug;

  private long start;
  
  private int errorCode = 0;
  private String errorText = null;
  private Throwable errorCause = null;

  
  public ConsoleEventMonitor(boolean debug) {
    this.debug = debug;
  }

  public void startEvent(String eventName, String target, long timestamp) {
    if("mojo-execute".equals(eventName)) {
      System.out.println(PREFIX + target);
    } else if("project-execute".equals(eventName)) {
      this.start = System.currentTimeMillis();
    }
  }

  public void endEvent(String eventName, String target, long timestamp) {
    if("project-execute".equals(eventName)) {
      System.out.println(PREFIX + "----------------------------------------------------------------------------");
      System.out.println(PREFIX + "BUILD SUCCESSFUL");
      System.out.println(PREFIX + "----------------------------------------------------------------------------");
      System.out.println(PREFIX + getTotalTime());
      System.out.println(PREFIX + getFinishedAt());
      System.out.println(PREFIX + getMemory());
      System.out.println(PREFIX + "----------------------------------------------------------------------------");
    }
  }

  private String getTotalTime() {
    return "Total time: " + ((System.currentTimeMillis() - start) / 1000) + " second";
  }

  private String getFinishedAt() {
    return "Finished at: " + new Date();
  }

  private String getMemory() {
    long freeMem = Runtime.getRuntime().freeMemory();
    long totalMem = Runtime.getRuntime().totalMemory();
    return "Memory " + (freeMem / (1024 * 1024)) + "M/" + (totalMem / (1024 * 1024)) + "M";
  }

  public void errorEvent(String eventName, String target, long timestamp, Throwable cause) {
    errorCode = 1;
    errorCause = cause;
    errorText = eventName;

    System.out.println("[ERROR] " + eventName + " : " + target);
    if(cause != null) {
      if(debug && "project-execute".equals(eventName)) {
        cause.printStackTrace(System.out);
      } else {
        System.out.println("Diagnosis: " + cause.getMessage());
      }
    }
    System.out.println("FATAL ERROR: Error executing Maven for a project");
  }

  public int getErrorCode() {
    return this.errorCode;
  }

  public Throwable getErrorCause() {
    return this.errorCause;
  }

  public String getErrorText() {
    return this.errorText;
  }

}
