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

package org.maven.ide.eclipse.actions;

import java.util.Collections;
import java.util.Set;

import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.embedder.BuildPathManager;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.index.Indexer;
import org.maven.ide.eclipse.index.Indexer.FileInfo;


public class AddDependencyAction implements IObjectActionDelegate {
  private IStructuredSelection selection;
  private IWorkbenchPart targetPart;

  public void run(IAction action) {
    Object o = selection.iterator().next();
    IFile file;
    IProject project;
    if(o instanceof IProject) {
      project = (IProject) o;
      file = project.getFile(Maven2Plugin.POM_FILE_NAME);
    } else if(o instanceof IFile) {
      file = (IFile) o;
      project = file.getProject();
    } else {
      return;
    }

    
    MavenModelManager modelManager = Maven2Plugin.getDefault().getMavenModelManager();
    Set artifacts;
    try {
      IJavaProject javaProject = JavaCore.create(project);
      IClasspathEntry entry = BuildPathManager.getMavenContainerEntry(javaProject);
      boolean resolveWorkspaceProjects = BuildPathManager.isResolvingWorkspaceProjects(entry);

      MavenExecutionResult result = modelManager.readMavenProject(file, new NullProgressMonitor(), true, false, resolveWorkspaceProjects);
      MavenProject mavenProject = result.getMavenProject();
      artifacts = mavenProject == null ? Collections.EMPTY_SET : mavenProject.getArtifacts();
    } catch(Exception ex) {
      // TODO move into ReadProjectTask
      String msg = "Unable to read project";
      Maven2Plugin.log(msg, ex);
      Maven2Plugin.getDefault().getConsole().logError(msg + "; " + ex.toString());
      return;
    }

    Maven2RepositorySearchDialog dialog = new Maven2RepositorySearchDialog(getShell(), artifacts, Indexer.JAR_NAME);
    if(dialog.open() == Window.OK) {
      Indexer.FileInfo fileInfo = (FileInfo) dialog.getFirstResult();
      if(fileInfo != null) {
        try {
          modelManager.addDependency(file, fileInfo.getDependency());
        } catch(Exception ex) {
          Maven2Plugin.log("Can't add dependency to " + file, ex);
        }
      }
    }
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    this.targetPart = targetPart;
  }

  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    }
  }

  protected Shell getShell() {
    Shell shell = null;
    if(targetPart != null) {
      shell = targetPart.getSite().getShell();
    }
    if(shell != null) {
      return shell;
    }

    IWorkbench workbench = Maven2Plugin.getDefault().getWorkbench();
    if(workbench == null) {
      return null;
    }

    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    return window == null ? null : window.getShell();
  }

}
