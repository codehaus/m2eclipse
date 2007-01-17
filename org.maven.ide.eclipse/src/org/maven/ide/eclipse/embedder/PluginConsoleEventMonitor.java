
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

import org.apache.maven.monitor.event.EventMonitor;

import org.maven.ide.eclipse.launch.console.Maven2Console;


public class PluginConsoleEventMonitor implements EventMonitor {

  private Maven2Console console;

  public PluginConsoleEventMonitor(Maven2Console console) {
    this.console = console;
  }

  public void startEvent( String eventName, String target, long timestamp ) {
    if( "mojo-execute".equals( eventName ) ) {
      console.logMessage( target );
    }
  }

  public void endEvent( String eventName, String target, long timestamp ) {
    if( "project-execute".equals( eventName ) ) {
      console.logMessage( "BUILD SUCCESSFUL" );
    }
  }

  public void errorEvent( String eventName, String target, long timestamp, Throwable cause ) {
    console.logMessage("ERROR " + eventName + " : " + target + (cause == null ? "" : " : " + cause.getMessage()));
  }

}

