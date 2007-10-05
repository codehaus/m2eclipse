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

package org.maven.ide.eclipse.wizards;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.embedder.BuildPathManager;
import org.maven.ide.eclipse.embedder.ResolverConfiguration;


/**
 * Maven Import Wizard
 * 
 * @author Eugene Kuleshov
 */
public class Maven2ImportWizard extends Wizard implements IImportWizard {

  Maven2ImportWizardPage page;

  public Maven2ImportWizard() {
    setNeedsProgressMonitor(true);
  }

  public void init(IWorkbench workbench, IStructuredSelection selection) {
  }

  public void addPages() {
    page = new Maven2ImportWizardPage();
    addPage(page);
  }

  public boolean performFinish() {
    if(!page.isPageComplete()) {
      return false;
    }

    final ResolverConfiguration configuration = page.getResolverConfiguration();
    final List mavenProjects = page.getProjects();
    
    Job job = new WorkspaceJob("Importing projects") {
      public IStatus runInWorkspace(IProgressMonitor monitor) {
        BuildPathManager buildpathManager = Maven2Plugin.getDefault().getBuildpathManager();
        for(Iterator it = mavenProjects.iterator(); it.hasNext();) {
          MavenProjectInfo projectInfo = (MavenProjectInfo) it.next();
          monitor.subTask(projectInfo.getLabel());
          try {
            buildpathManager.importProject(projectInfo.getPomFile(), projectInfo.getModel(), configuration, monitor);
          } catch(CoreException ex) {
            Maven2Plugin.getDefault().getConsole().logError(
                "Can't create project for " + projectInfo.getModel().getId() + "; " + ex.toString());
          }
        }
        return Status.OK_STATUS;
      }
    };
    job.schedule();

    return true;
  }

}
