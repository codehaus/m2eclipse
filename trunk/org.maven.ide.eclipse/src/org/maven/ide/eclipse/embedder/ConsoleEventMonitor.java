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

package org.maven.ide.eclipse.embedder;

import java.util.Date;

import org.apache.maven.monitor.event.EventMonitor;


class ConsoleEventMonitor implements EventMonitor {
  private static final String INFO_PREFIX = "[INFO] ";
  private static final String ERROR_PREFIX = "[ERROR] ";

  private final boolean debug;

  private long start = System.currentTimeMillis();
  
  private int errorCode = 0;
  private String errorText = null;
  private Throwable errorCause = null;

  public ConsoleEventMonitor(boolean debug) {
    this.debug = debug;
  }

  public int getErrorCode() {
    return this.errorCode;
  }

  public String getErrorText() {
    return this.errorText;
  }
  
  public Throwable getErrorCause() {
    return this.errorCause;
  }

  public void startEvent(String eventName, String target, long timestamp) {
    if("mojo-execute".equals(eventName)) {
      printInfo(target);
    } else if("project-execute".equals(eventName)) {
      this.start = System.currentTimeMillis();
    }
  }

  public void endEvent(String eventName, String target, long timestamp) {
    if("project-execute".equals(eventName)) {
      printSeparator();
      printInfo("BUILD SUCCESSFUL" + (target==null ? "" : " " + target));
      printTrailer();
    }
  }

  public void errorEvent(String eventName, String target, long timestamp, Throwable cause) {
    if("project-execute".equals(eventName)) {
      errorCode = 1;
      errorCause = cause;
      errorText = eventName;
  
      printSeparator();
      printError("BUILD FAILURE" + (target==null ? "" : " " + target));
      printSeparator();
      
      if(cause != null) {
        printInfo((cause.getMessage()==null ? cause.toString() : cause.getMessage()));
        if(debug) {
          cause.printStackTrace(System.out);
        }
      }
      
      printTrailer();
    } else {
      cause.printStackTrace();
    }
  }

  private void printError(String msg) {
    System.out.println(ERROR_PREFIX + msg);
  }

  private void printInfo(String msg) {
    System.out.println(INFO_PREFIX + msg);
  }

  private void printSeparator() {
    printInfo("----------------------------------------------------------------------------");
  }

  private void printTrailer() {
    printSeparator();
    printInfo(getTotalTime());
    printInfo(getFinishedAt());
    printInfo(getMemory());
    printSeparator();
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

}
