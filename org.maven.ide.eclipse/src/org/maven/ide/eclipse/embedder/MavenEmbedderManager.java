
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.launch.console.Maven2Console;
import org.maven.ide.eclipse.preferences.Maven2PreferenceConstants;

/**
 * Maven Embedder manager
 *
 * @author Eugene Kuleshov
 */
public class MavenEmbedderManager {

  private final Maven2Console console;
  private final IPreferenceStore preferenceStore;

  private MavenEmbedder projectEmbedder;

  
  public MavenEmbedderManager(Maven2Console console, IPreferenceStore preferenceStore) {
    this.console = console;
    this.preferenceStore = preferenceStore;
  }

  public MavenEmbedder getProjectEmbedder() {
    if(this.projectEmbedder==null) {
      try {
        boolean debug = preferenceStore.getBoolean(Maven2PreferenceConstants.P_DEBUG_OUTPUT);
        this.projectEmbedder = EmbedderFactory.createMavenEmbedder(EmbedderFactory.createProjectCustomizer(),
            new PluginConsoleMavenEmbeddedLogger(console, debug));
      } catch(MavenEmbedderException ex) {
        console.logError("Can't create project embedder; " + ex.toString());
      }
    }
    return this.projectEmbedder;
  }

  public void invalidateMavenSettings() {
    shutdown();
  }

  public Object executeInEmbedder(String name, MavenEmbedderCallback template) throws CoreException {
    EmbedderJob job = new EmbedderJob(name, template, getProjectEmbedder());
    job.schedule();
    try {
      job.join();

      IStatus status = job.getResult();
      if(status == null) {
        console.logError("Job " + name + " terminated; " + job);
      } else {
        if(status.isOK()) {
          return job.getCallbackResult();
        }

        console.logError("Job " + name + " failed; " + status.getException().toString());
        throw new CoreException(status);
      }

    } catch(InterruptedException ex) {
      console.logError("Job " + name + " interrupted " + ex.toString());
    }
    return null;
  }  
  
  public void addDependency(IFile pomFile, Dependency dependency) {
    addDependencies(pomFile, Collections.singletonList(dependency));
  }

  public void addDependencies(IFile pomFile, List dependencies) {
    File pom = pomFile.getLocation().toFile();
    try {
      MavenEmbedder mavenEmbedder = getProjectEmbedder();
      Model model = mavenEmbedder.readModel(pom);
      model.getDependencies().addAll(dependencies);

      StringWriter w = new StringWriter();
      mavenEmbedder.writeModel(w, model, true);

      pomFile.setContents(new ByteArrayInputStream(w.toString().getBytes("ASCII")), true, true, null);
      pomFile.refreshLocal(IResource.DEPTH_ONE, null); // TODO ???
    } catch(Exception ex) {
      console.logError("Unable to update POM: " + pom + "; " + ex.getMessage());
    }
  }

  public void shutdown() {
    // XXX need to wait when embedder jobs will be completed 
    if(projectEmbedder!=null) {
      try {
        projectEmbedder.stop();
      } catch(MavenEmbedderException ex) {
        console.logError("Error on stopping project embedder "+ex.getMessage());
      }
      projectEmbedder = null;
    }
  }
  

  private static final class EmbedderJob extends Job {
    private final MavenEmbedderCallback template;
    private final MavenEmbedder embedder;

    private Object callbackResult;

    EmbedderJob( String name, MavenEmbedderCallback template, MavenEmbedder embedder ) {
      super( name );
      this.template = template;
      this.embedder = embedder;
    }

    protected IStatus run( IProgressMonitor monitor ) {
      try {
        callbackResult = this.template.run(this.embedder, monitor);
        return Status.OK_STATUS;
      } catch( Throwable t ) {
        return new Status(IStatus.ERROR, Maven2Plugin.PLUGIN_ID, IStatus.ERROR, t.getMessage(), t);
      }
    }
    
    public Object getCallbackResult() {
      return this.callbackResult;
    }
    
  }

}
