
package org.maven.ide.eclipse.builder;

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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.container.Maven2ClasspathContainer;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.launch.console.Maven2Console;
import org.maven.ide.eclipse.util.Util;


public class Maven2Builder extends IncrementalProjectBuilder {
  private Maven2Plugin plugin;
  private Maven2Console  console;
  private MavenModelManager mavenModelManager;

  
  public Maven2Builder() {
    plugin = Maven2Plugin.getDefault();
    console = plugin.getConsole();
    mavenModelManager = plugin.getMavenModelManager();
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
   *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
   */
  protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
    IProject project = getProject();
    if(project.hasNature(Maven2Plugin.NATURE_ID)) {

      // if( kind == AUTO_BUILD || kind == INCREMENTAL_BUILD ) {
      Verifier verifier = new Verifier(monitor, mavenModelManager);
      IResourceDelta delta = getDelta(project);
      if(delta == null) {
        IFile pomFile = getProject().getFile(Maven2Plugin.POM_FILE_NAME);
        if(pomFile==null) {
          console.logError("Project "+getProject().getName()+" is missing pom.xml");
        }
        mavenModelManager.updateMavenModel(pomFile, true, monitor);
      } else {
        delta.accept(verifier, IContainer.EXCLUDE_DERIVED);
        if(!verifier.updated) {
          return null;
        }
      }

      updateClasspath(monitor, project);
    }
    return null;
  }

  private void updateClasspath(IProgressMonitor monitor, IProject project) throws JavaModelException {
    Set entries = new HashSet();
    Set moduleArtifacts = new HashSet();
    IFile pomFile = project.getFile(Maven2Plugin.POM_FILE_NAME);
    plugin.getClasspathResolver().resolveClasspathEntries(entries, moduleArtifacts, pomFile, true, monitor);

    Maven2ClasspathContainer container = new Maven2ClasspathContainer(entries);

    JavaCore.setClasspathContainer(container.getPath(), new IJavaProject[] {JavaCore.create(project)},
        new IClasspathContainer[] {container}, monitor);
  }

  
  static final class Verifier implements IResourceDeltaVisitor {
    private final IProgressMonitor monitor;
    private final MavenModelManager mavenModelManager;

    boolean updated;

    public Verifier(IProgressMonitor monitor, MavenModelManager mavenModelManager) {
      this.monitor = monitor;
      this.mavenModelManager = mavenModelManager;
    }

    public boolean visit(IResourceDelta delta) {
      IResource resource = delta.getResource();
      if(resource.getType() == IResource.FILE && Maven2Plugin.POM_FILE_NAME.equals(resource.getName())) {
        updated = true;

        Util.deleteMarkers(resource);
        try {
          mavenModelManager.updateMavenModel((IFile) resource, true, monitor);
        } catch(CoreException ex) {
          Util.addMarker(resource, ex.getMessage(), 1, IMarker.SEVERITY_ERROR);
        }
      }
      return true;
    }

  }

}
