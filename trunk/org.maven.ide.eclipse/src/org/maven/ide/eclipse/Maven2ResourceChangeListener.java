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

import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
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
  final Maven2Console console;

  public Maven2ResourceChangeListener(MavenModelManager mavenModelManager, BuildPathManager buildpathManager, Maven2Console console) {
    this.mavenModelManager = mavenModelManager;
    this.buildpathManager = buildpathManager;
    this.console = console;
  }

  public void resourceChanged(IResourceChangeEvent event) {
    int type = event.getType();
    if (IResourceChangeEvent.PRE_CLOSE == type || IResourceChangeEvent.PRE_DELETE == type) {
      refreshDependents((IProject) event.getResource());
      return;
    }

    // if (IResourceChangeEvent.POST_CHANGE == type)
    IResourceDelta delta = event.getDelta(); // this is workspace delta
    IResourceDelta[] projectDeltas = delta.getAffectedChildren();
    for (int p = 0; p < projectDeltas.length; p++) {
      if ((IResourceDelta.OPEN & projectDeltas[p].getKind()) != 0) continue; 
      IProject project = (IProject) projectDeltas[p].getResource();
      try {
        if (!project.isOpen() || !project.hasNature(Maven2Plugin.NATURE_ID)) continue;
        if (projectDeltas[p].findMember(new Path(Maven2Plugin.POM_FILE_NAME)) != null) {
          refresh(project);
        }
      } catch(CoreException ex) {
        console.logError(ex.getMessage());
      }
    }
  }

  private void refreshDependents(IProject project) {
    try {
      if (!project.isOpen() || !project.hasNature(Maven2Plugin.NATURE_ID)) return;
      IFile pomFile = project.getFile(Maven2Plugin.POM_FILE_NAME);
      if (pomFile != null) {
        final Set projects = mavenModelManager.getDependentProjects(pomFile);
        mavenModelManager.removeMavenModel(pomFile, true, new NullProgressMonitor());
        buildpathManager.scheduleUpdateClasspathContainer(projects);
      }
    } catch(CoreException ex) {
      console.logError(ex.getMessage());
    }
  }

  private void refresh(IProject project) {
    buildpathManager.scheduleUpdateClasspathContainer(project);
  }
}

