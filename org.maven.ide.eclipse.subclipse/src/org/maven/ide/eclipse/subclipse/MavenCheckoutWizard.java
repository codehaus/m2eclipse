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

package org.maven.ide.eclipse.subclipse;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;

import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;

import org.maven.ide.eclipse.embedder.ResolverConfiguration;


/**
 * Maven checkout wizard
 * 
 * TODO new and import wizards 
 * TODO chare ResolverConfiguration between locationPage and projectsPage
 * 
 * @author Eugene Kuleshov
 */
public class MavenCheckoutWizard extends Wizard /* implements INewWizard, IImportWizard */{

  private final ISVNRemoteFolder[] folders;

  private MavenCheckoutProjectsPage projectsPage;

  private MavenCheckoutLocationPage locationPage;

  public MavenCheckoutWizard() {
    this(null);
    setNeedsProgressMonitor(true);
  }

  public MavenCheckoutWizard(ISVNRemoteFolder[] folders) {
    this.folders = folders;
    setNeedsProgressMonitor(true);
    setWindowTitle("Checkout as Maven project from SVN");
  }

  public void init(IWorkbench workbench, IStructuredSelection selection) {
  }

  public void addPages() {
    locationPage = new MavenCheckoutLocationPage();
    addPage(locationPage);

    projectsPage = new MavenCheckoutProjectsPage(folders, locationPage);
    addPage(projectsPage);
  }

  public boolean canFinish() {
    if(locationPage.isCheckoutAllProjects()) {
      return true;
    }
    return super.canFinish();
  }

  public boolean performFinish() {
    if(!canFinish()) {
      return false;
    }

    List projects;
    ResolverConfiguration resolverConfiguration;
    if(locationPage.isCheckoutAllProjects()) {
      projects = null;
      resolverConfiguration = locationPage.getResolverConfiguration();
    } else {
      projects = projectsPage.getProjects();
      resolverConfiguration = projectsPage.getResolverConfiguration();
    }

    final MavenCheckoutOperation operation = new MavenCheckoutOperation();
    operation.setConfiguration(resolverConfiguration);
    operation.setMavenProjectInfos(projects);
    operation.setFolders(folders);
    operation.setSVNRevision(locationPage.getRevision());
    operation.setLocation(locationPage.getLocation());
    operation.setWorkspaceLocation(locationPage.isDefaultWorkspaceLocation());

    Job job = new WorkspaceJob("Checking out Maven projects") {
      public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        try {
          operation.run(monitor);
        } catch(InterruptedException ex) {
          // interrupted
        } catch(InvocationTargetException ex) {
          Throwable e = ex.getTargetException() == null ? ex : ex.getTargetException();
          if(e instanceof CoreException) {
            throw (CoreException) e;
          }
          throw new CoreException(new Status(IStatus.ERROR, MavenSubclipsePlugin.PLUGIN_ID, 0, e.toString(), e));
        }
        return Status.OK_STATUS;
      }
    };
    job.schedule();

    return true;
  }

}
