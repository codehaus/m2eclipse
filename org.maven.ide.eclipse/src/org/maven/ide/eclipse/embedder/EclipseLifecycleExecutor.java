
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.BuildFailureException;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.extension.ExtensionManager;
import org.apache.maven.lifecycle.DefaultLifecycleExecutor;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.TaskValidationResult;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.logging.AbstractLogEnabled;


public class EclipseLifecycleExecutor extends AbstractLogEnabled implements LifecycleExecutor {

  private PluginManager pluginManager;
  private ExtensionManager extensionManager;
  private List lifecycles;
  private ArtifactHandlerManager artifactHandlerManager;
  private List defaultReports;
  private Map phaseToLifecycleMap;

  private static ThreadLocal reactorManager = new ThreadLocal();

  
  public static List /*java.util.File*/getAffectedProjects() {
    Object obj = reactorManager.get();
    if(obj instanceof ReactorManager) {
      ReactorManager rm = (ReactorManager) obj;
      List lst = rm.getSortedProjects();
      Iterator it = lst.iterator();
      List toRet = new ArrayList();
      while(it.hasNext()) {
        MavenProject elem = (MavenProject) it.next();
        toRet.add(elem.getFile());
      }
      return toRet;
    }
    return Collections.EMPTY_LIST;
  }

  /**
   * Execute a task. Each task may be a phase in the lifecycle or the
   * execution of a mojo.
   */
  public void execute(MavenSession session, ReactorManager rm, EventDispatcher dispatcher)
      throws BuildFailureException, LifecycleExecutionException {
    reactorManager.set(rm);
    createExecutor().execute(session, rm, dispatcher);
  }

  private LifecycleExecutor createExecutor() {
    DefaultLifecycleExecutor exec = new DefaultLifecycleExecutor();
    setVar(exec, pluginManager, "pluginManager");
    setVar(exec, extensionManager, "extensionManager");
    System.out.println("setting lifecycle=" + lifecycles);
    setVar(exec, lifecycles, "lifecycles");
    setVar(exec, artifactHandlerManager, "artifactHandlerManager");
    setVar(exec, defaultReports, "defaultReports");
    // setVar(exec, phaseToLifecycleMap, "phaseToLifecycleMap");
    exec.enableLogging(getLogger());
    return exec;
  }

  private void setVar(Object exec, Object value, String name) {
    try {
      Field fld = exec.getClass().getDeclaredField(name);
      fld.setAccessible(true);
      fld.set(exec, value);
    } catch(Exception ex) {
      // XXX log exception to console
    }
  }

  public TaskValidationResult isTaskValid(String s, MavenSession mavensession, MavenProject mavenproject) {
    return createExecutor().isTaskValid(s, mavensession, mavenproject);
  }

//  public Map getLifecycleMappings(MavenSession mavenSession, String string, String string0, MavenProjectInfo mavenProject)
//      throws LifecycleExecutionException, BuildFailureException, PluginNotFoundException {
//    return createExecutor().getLifecycleMappings(mavenSession, string, string0, mavenProject);
//  }

}
