
package org.maven.ide.eclipse.actions;

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

import java.util.Collections;
import java.util.Set;

import org.apache.maven.project.MavenProject;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
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
import org.maven.ide.eclipse.embedder.ClassPathResolver;
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;
import org.maven.ide.eclipse.index.Indexer;
import org.maven.ide.eclipse.index.Indexer.FileInfo;


public class AddDependencyAction implements IObjectActionDelegate {
  private IStructuredSelection selection;
  private IWorkbenchPart targetPart;

  public void run(IAction action) {
    Object o = selection.iterator().next();
    final IFile file;
    if(o instanceof IProject) {
      file = ((IProject) o).getFile(Maven2Plugin.POM_FILE_NAME);
    } else if(o instanceof IFile) {
      file = (IFile) o;
    } else {
      return;
    }

    Maven2Plugin plugin = Maven2Plugin.getDefault();
    MavenEmbedderManager embedderManager = plugin.getMavenEmbedderManager();
    
    MavenProject mavenProject;
    try {
      mavenProject = (MavenProject) embedderManager.executeInEmbedder("Read Project", new ClassPathResolver.ReadProjectTask(
          file, plugin.getConsole(), plugin.getMavenRepositoryIndexManager(), plugin.getPreferenceStore()));
    } catch(CoreException ex) {
      // TODO move into ReadProjectTask
      Maven2Plugin.log(ex);
      Maven2Plugin.getDefault().getConsole().logError(ex.getMessage());
      return;
    } catch(Exception ex) {
      // TODO move into ReadProjectTask
      String msg = "Unable to read model";
      Maven2Plugin.log(msg, ex);
      Maven2Plugin.getDefault().getConsole().logError(msg + "; " + ex.toString());
      return;
    }

    Set artifacts = mavenProject == null ? Collections.EMPTY_SET : mavenProject.getArtifacts();

    Maven2RepositorySearchDialog dialog = new Maven2RepositorySearchDialog(getShell(), artifacts, Indexer.JAR_NAME);
    if(dialog.open() == Window.OK) {
      Indexer.FileInfo fileInfo = (FileInfo) dialog.getFirstResult();
      if(fileInfo != null) {
        try {
          embedderManager.addDependency(file, fileInfo.getDependency());
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
