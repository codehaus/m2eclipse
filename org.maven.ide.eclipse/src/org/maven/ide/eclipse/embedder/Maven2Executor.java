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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.MavenExecutionRequest;

import org.maven.ide.eclipse.launch.Maven2LaunchConstants;
import org.maven.ide.eclipse.launch.Maven2LaunchDelegate;
import org.maven.ide.eclipse.preferences.Maven2PreferenceConstants;


/**
 * Maven2Executor
 *
 * @see Maven2LaunchDelegate
 */
public class Maven2Executor implements Maven2LaunchConstants {

  public static void main(String[] args) {
    // System.err.println("Starting in "+System.getProperty("user.dir"));
    if (args.length < 1) {
      System.err.println("POM file name is missing");
    }
    // final long start = System.currentTimeMillis();

    List goals = new ArrayList();
    for (int i = 1; i < args.length; i++) {
      goals.add(args[i]);
    }

    boolean offline = Boolean.getBoolean(Maven2PreferenceConstants.P_OFFLINE);
    boolean debug = Boolean.getBoolean(Maven2PreferenceConstants.P_DEBUG_OUTPUT);
    String globalSettings = System.getProperty(Maven2PreferenceConstants.P_GLOBAL_SETTINGS_FILE);

    MavenEmbedder embedder = null;
    final ConsoleEventMonitor consoleEventMonitor = new ConsoleEventMonitor(debug);
    try {
      embedder = EmbedderFactory.createMavenEmbedder(EmbedderFactory.createExecutionCustomizer(), 
          new ConsoleMavenEmbeddedLogger(debug), globalSettings);

      String pomFileName = args[0];
      File pomFile = new File( pomFileName );

      MavenExecutionRequest request = EmbedderFactory.createMavenExecutionRequest(embedder, offline, debug);

      request.setBaseDirectory(pomFile.getParentFile());
      request.setPomFile(pomFile.getAbsolutePath());
      request.setGoals(goals);
      request.setProperties(System.getProperties());

      String profiles = System.getProperty(Maven2LaunchConstants.ATTR_PROFILES);
      if(profiles != null) {
        request.addActiveProfiles(Arrays.asList(profiles.split(", ")));
      }
      
      request.addEventMonitor(consoleEventMonitor);
      request.setTransferListener(new ConsoleTransferMonitor());
      
      embedder.execute(request);

    } catch(Throwable e) {
      e.printStackTrace(System.out);
      System.exit(1);
    } finally {
      try {
        if(embedder != null) {
          embedder.stop();
        }
      } catch(MavenEmbedderException e) {
        e.printStackTrace(System.out);
      }
    }
    System.exit(consoleEventMonitor.getErrorCode());
  }

}

