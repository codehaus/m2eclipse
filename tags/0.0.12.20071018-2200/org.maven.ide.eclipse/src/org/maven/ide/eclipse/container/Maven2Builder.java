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

package org.maven.ide.eclipse.container;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.maven.model.Model;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.embedder.BuildPathManager;
import org.maven.ide.eclipse.embedder.ResolverConfiguration;
import org.maven.ide.eclipse.launch.console.Maven2Console;


public class Maven2Builder extends IncrementalProjectBuilder {
  private final Maven2Plugin plugin;

  private final Maven2Console console;

  private final BuildPathManager buildpathManager;

  public Maven2Builder() {
    plugin = Maven2Plugin.getDefault();
    console = plugin.getConsole();
    buildpathManager = plugin.getBuildpathManager();
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
      IFile pomFile = project.getFile(Maven2Plugin.POM_FILE_NAME);
      if(pomFile == null) {
        console.logError("Project " + project.getName() + " is missing pom.xml");
        return null;
      }

      // if( kind == AUTO_BUILD || kind == INCREMENTAL_BUILD ) {
      IResourceDelta delta = getDelta(project);
      if(delta != null) {
        IJavaProject javaProject = JavaCore.create(project);
        ResolverConfiguration resolverConfiguration = BuildPathManager.getResolverConfiguration(javaProject);
        HashSet poms = new HashSet();
        if(resolverConfiguration.shouldIncludeModules()) {
          addModulePoms(poms, pomFile, monitor);
        } else {
          poms.add(pomFile.getLocation().toString());
        }

        Verifier verifier = new Verifier(poms);
        delta.accept(verifier, IContainer.EXCLUDE_DERIVED);
        if(!verifier.updated) {
          return null;
        }
      }

      buildpathManager.updateClasspathContainer(project, monitor);
    }
    return null;
  }

  private void addModulePoms(HashSet poms, IFile pomFile, IProgressMonitor monitor) {
    poms.add(pomFile.getLocation().toString());

    Model model = plugin.getMavenModelManager().getMavenModel(pomFile);
    if(model != null) {
      IContainer parent = pomFile.getParent();
      for(Iterator it = model.getModules().iterator(); it.hasNext();) {
        if(monitor.isCanceled()) {
          throw new OperationCanceledException();
        }
        String module = (String) it.next();
        IResource memberPom = parent.findMember(module + "/" + Maven2Plugin.POM_FILE_NAME); //$NON-NLS-1$
        if(memberPom != null && memberPom.getType() == IResource.FILE && memberPom.isAccessible()) {
          addModulePoms(poms, (IFile) memberPom, monitor);
        }
      }
    }
  }

  static final class Verifier implements IResourceDeltaVisitor {
    boolean updated;

    private final HashSet poms;

    public Verifier(HashSet poms) {
      this.poms = poms;
    }

    public boolean visit(IResourceDelta delta) {
      IResource resource = delta.getResource();
      if(resource.getType() == IResource.FILE && Maven2Plugin.POM_FILE_NAME.equals(resource.getName())) {
        if(poms.contains(resource.getLocation().toString())) {
          updated = true;
          return false;
        }
      }
      return !updated;
    }

  }

}
