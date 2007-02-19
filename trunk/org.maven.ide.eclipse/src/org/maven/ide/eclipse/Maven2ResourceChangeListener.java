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

package org.maven.ide.eclipse;

import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.maven.ide.eclipse.embedder.BuildPathManager;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.launch.console.Maven2Console;


/**
 * An <code>IResourceChangeListener</code> for monitoring project dependencies
 *
 * @author Eugene Kuleshov
 */
final class Maven2ResourceChangeListener implements IResourceChangeListener {
  private final MavenModelManager mavenModelManager;
  private final BuildPathManager buildpathManager;
  private final Maven2Console console;

  public Maven2ResourceChangeListener(MavenModelManager mavenModelManager, BuildPathManager buildpathManager, Maven2Console console) {
    this.mavenModelManager = mavenModelManager;
    this.buildpathManager = buildpathManager;
    this.console = console;
  }

  public void resourceChanged(IResourceChangeEvent event) {
    try {
      int type = event.getType();
      Verifier verifier = new Verifier(type, new NullProgressMonitor(), mavenModelManager, buildpathManager);
      switch(type) {
        // case POST_BUILD:
        case IResourceChangeEvent.PRE_BUILD:
          event.getDelta().accept(verifier);
          break;
  
        case IResourceChangeEvent.POST_CHANGE:
          event.getDelta().accept(verifier);
          break;
        
        case IResourceChangeEvent.PRE_CLOSE:
          event.getResource().accept(verifier, IResource.DEPTH_INFINITE, true);
          break;
        
        case IResourceChangeEvent.PRE_DELETE:
          event.getResource().accept(verifier, IResource.DEPTH_INFINITE, true);
          break;
      }
    } catch(CoreException ex) {
      console.logError(ex.getMessage());
      Maven2Plugin.log(ex);
    }
  }

  
  final class Verifier implements IResourceDeltaVisitor, IResourceVisitor {
    private final int type;
    private final IProgressMonitor monitor;
    private final MavenModelManager mavenModelManager;
    private final BuildPathManager buildpathManager;

    boolean updated;

    public Verifier(int type, IProgressMonitor monitor, MavenModelManager mavenModelManager, BuildPathManager buildPathManager) {
      this.type = type;
      this.monitor = monitor;
      this.mavenModelManager = mavenModelManager;
      this.buildpathManager = buildPathManager;
    }

    public boolean visit(IResourceDelta delta) {
      IResource resource = delta.getResource();
      if(resource.getType() != IResource.PROJECT) {
        return true;
      }
      // console.logMessage(getType() + " : " + getFlags(delta) + ": " + delta.getFullPath());
      return visitProject((IProject) resource);
    }

    public boolean visit(IResource resource) throws CoreException {
      if(resource.getType() != IResource.PROJECT) {
        return true;
      }
      // console.logMessage(getType() + " : " + resource.getFullPath());
      return visitProject((IProject) resource);
    }
    
    private boolean visitProject(IProject project) {
      IFile pomFile = project.getFile(Maven2Plugin.POM_FILE_NAME);
      if(pomFile==null) {
        // console.logError("Project "+getProject().getName()+" is missing pom.xml");
        return true;
      }

      if(type==IResourceChangeEvent.PRE_CLOSE || type==IResourceChangeEvent.PRE_DELETE) {
        // Util.deleteMarkers(pomFile);

        final Set projects = mavenModelManager.getDependentProjects(pomFile);
        
        mavenModelManager.removeMavenModel(pomFile, true, monitor);
        
        new Job("Updating dependent projects") {
          protected IStatus run(IProgressMonitor monitor) {
            for(Iterator it = projects.iterator(); it.hasNext();) {
              IProject project = (IProject) it.next();
              try {
                buildpathManager.updateClasspathContainer(project, true, monitor);
              } catch(CoreException ex) {
                console.logError(ex.toString());
                Maven2Plugin.log(ex);
              }
            }
            
            return Status.OK_STATUS;
          }
        }.schedule();
      }
      return false;
    }

    private String getType() {
      switch(type) {
        case IResourceChangeEvent.PRE_CLOSE:
          return "PRE_CLOSE";
        case IResourceChangeEvent.PRE_DELETE:
          return "PRE_DELETE";
        case IResourceChangeEvent.PRE_BUILD:
          return "PRE_BUILD";
        case IResourceChangeEvent.POST_BUILD:
          return "POST_BUILD";
        case IResourceChangeEvent.POST_CHANGE:
          return "POST_CHANGE";
      }
      return "" + type;
    }

    private String getFlags(IResourceDelta delta) {
      int kind = delta.getKind();
      String s = "";
      if((kind & IResourceDelta.CHANGED) > 0) {
        s += "CHANGED ";
        
        int flags = delta.getFlags();
        if((flags & IResourceDelta.CONTENT) > 0) s += "CONTENT ";
        if((flags & IResourceDelta.DESCRIPTION) > 0) s += "DESCRIPTION ";
        if((flags & IResourceDelta.ENCODING) > 0) s += "ENCODING ";
        if((flags & IResourceDelta.OPEN) > 0) s += "OPEN ";
        if((flags & IResourceDelta.MOVED_TO) > 0) s += "MOVED_TO ";
        if((flags & IResourceDelta.MOVED_FROM) > 0) s += "MOVED_FROM ";
        if((flags & IResourceDelta.TYPE) > 0) s += "TYPE ";
        if((flags & IResourceDelta.SYNC) > 0) s += "SYNC ";
        if((flags & IResourceDelta.MARKERS) > 0) s += "MARKERS ";
        if((flags & IResourceDelta.REPLACED) > 0) s += "REPLACED ";
        
      }
      if((kind & IResourceDelta.ADDED) > 0) s += "ADDED ";
      if((kind & IResourceDelta.REMOVED) > 0) s += "REMOVED ";
      if((kind & IResourceDelta.ADDED_PHANTOM) > 0) s += "ADDED_PHANTOM ";
      if((kind & IResourceDelta.REMOVED_PHANTOM) > 0) s += "REMOVED_PHANTOM ";
      
      return s;
    }

  }
  
}

