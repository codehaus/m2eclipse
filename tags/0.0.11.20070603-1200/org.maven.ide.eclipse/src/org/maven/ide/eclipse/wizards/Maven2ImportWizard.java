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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.wizards.Maven2ImportWizardPage.MavenProject;


/**
 * Maven2ImportWizard
 * 
 * @author Eugene Kuleshov
 */
public class Maven2ImportWizard extends Wizard implements IImportWizard {

  private Maven2ImportWizardPage page;

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

    Job job = new ProjectImportJob("Importing projects", page.getPomFiles());
    job.schedule();

    return true;
  }

  private static final class ProjectImportJob extends Job {
    private List files;

    ProjectImportJob(String name, List files) {
      super(name);
      this.files = files;
    }

    protected IStatus run(IProgressMonitor monitor) {
      for(Iterator it = files.iterator(); it.hasNext();) {
        MavenProject mavenProject = (MavenProject) it.next();
        try {
          createMavenProject(mavenProject, new NullProgressMonitor());
        } catch(CoreException ex) {
          Maven2Plugin.getDefault().getConsole().logError(
              "Unable to create project " + mavenProject.model.getId() + "; " + ex.toString());
        }
      }

      return Status.OK_STATUS;
    }

    private void createMavenProject(final MavenProject mavenProject, IProgressMonitor monitor) throws CoreException {
      final String projectName = mavenProject.model.getArtifactId();
      final IWorkspace workspace = ResourcesPlugin.getWorkspace();

      workspace.run(new IWorkspaceRunnable() {
        public void run(IProgressMonitor monitor) throws CoreException {
          IWorkspaceRoot root = workspace.getRoot();

          IProject project = root.getProject(projectName);
          if(project.exists()) {
            return;
          }

          IProjectDescription description = workspace.newProjectDescription(projectName);
          description.setLocation(new Path(mavenProject.pomFile.getParentFile().getAbsolutePath()));

          project.create(description, monitor);

          if(!project.isOpen()) {
            project.open(monitor);
          }

          // TODO set natures, source folders, etc

          Maven2Plugin plugin = Maven2Plugin.getDefault();

          plugin.getBuildpathManager().enableMavenNature(project);
          plugin.getBuildpathManager().updateSourceFolders(project, monitor);
          plugin.getBuildpathManager().updateClasspathContainer(project, true, monitor);
        }
      }, monitor);
    }
  }

}
