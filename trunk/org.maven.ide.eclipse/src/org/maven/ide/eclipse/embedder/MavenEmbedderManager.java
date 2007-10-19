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

import org.apache.maven.embedder.ContainerCustomizer;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;

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

  private MavenEmbedder workspaceEmbedder;

  
  public MavenEmbedderManager(Maven2Console console, IPreferenceStore preferenceStore) {
    this.console = console;
    this.preferenceStore = preferenceStore;
  }

  
  public synchronized MavenEmbedder createEmbedder(ContainerCustomizer customizer) {
    try {
      String globalSettings = preferenceStore.getString(Maven2PreferenceConstants.P_GLOBAL_SETTINGS_FILE);
      boolean debug = preferenceStore.getBoolean(Maven2PreferenceConstants.P_DEBUG_OUTPUT);

      return EmbedderFactory.createMavenEmbedder(customizer,
          new PluginConsoleMavenEmbeddedLogger(console, debug), globalSettings);
      
    } catch(MavenEmbedderException ex) {
      String msg = "Can't create project embedder; " + ex.getMessage();
      console.logError(msg);
      Maven2Plugin.log(msg, ex);
    }
    return null;
  }
  
  public MavenEmbedder getWorkspaceEmbedder() {
    if(this.workspaceEmbedder==null) {
      this.workspaceEmbedder = createEmbedder(EmbedderFactory.createExecutionCustomizer()); 
    }
    return this.workspaceEmbedder;
  }

  public void invalidateMavenSettings() {
    shutdown();
  }

  public Object executeInEmbedder(String name, MavenEmbedderCallback template) throws CoreException {
    EmbedderJob job = new EmbedderJob(name, template, getWorkspaceEmbedder());
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
  
  public void shutdown() {
    // XXX need to wait when embedder jobs will be completed 
    if(workspaceEmbedder!=null) {
      try {
        workspaceEmbedder.stop();
      } catch(MavenEmbedderException ex) {
        console.logError("Error on stopping project embedder "+ex.getMessage());
      }
      workspaceEmbedder = null;
    }
  }
  

  public File getLocalRepositoryDir() {
    String localRepository = getWorkspaceEmbedder().getLocalRepository().getBasedir();
    
    File localRepositoryDir = new File(localRepository);
    
  //    if(!localRepositoryDir.exists()) {
  //      console.logError("Created local repository folder "+localRepository);
  //      localRepositoryDir.mkdirs();
  //    }
    
    if(!localRepositoryDir.exists()) {
      localRepositoryDir.mkdirs();
    }
    if(!localRepositoryDir.isDirectory()) {
      console.logError("Local repository "+localRepository+" is not a directory");
    }
    
    return localRepositoryDir;
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
