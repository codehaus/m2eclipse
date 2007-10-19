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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.util.Arrays;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.model.Model;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.Messages;
import org.maven.ide.eclipse.embedder.BuildPathManager;
import org.maven.ide.eclipse.embedder.ResolverConfiguration;
import org.maven.ide.eclipse.util.Util;


/**
 * Simple project wizard for creating a new Maven2 project.
 *
 * <p>
 * The wizard provides the following functionality to the user:
 * <ul>
 *   <li>Create the project in the workspace or at some external location.</li>
 *   <li>Provide information about the Maven2 artifact to create.</li>
 *   <li>Choose directories of the default Maven2 directory structure to create.</li>
 *   <li>Choose a set of Maven2 dependencies for the project.</li>
 * </ul>
 * </p>
 * <p>
 * Once the wizard has finished, the following resources are created and
 * configured:
 * <ul>
 *   <li>A POM file containing the given artifact information and the chosen dependencies.</li>
 *   <li>The chosen Maven2 directories.</li>
 *   <li>The .classpath file is configured to hold appropriate entries for
 *       the Maven2 directories created as well as the Java and Maven2 classpath
 *       containers.</li>
 * </ul>
 * </p>
 */
public class Maven2ProjectWizard extends Wizard implements INewWizard {
  /** The name of the default wizard page image. */
  private static final String DEFAULT_PAGE_IMAGE_NAME = "icons/new_m2_project_wizard.gif";

  /** The default wizard page image. */
  private static final ImageDescriptor DEFAULT_PAGE_IMAGE = Maven2Plugin.getImageDescriptor(DEFAULT_PAGE_IMAGE_NAME);

  /** The wizard page for gathering general project information. */
  private Maven2ProjectWizardLocationPage locationPage;

  /** The wizard page for gathering Maven2 project information. */
  private Maven2ProjectWizardArtifactPage artifactPage;

  /** The wizard page for choosing the Maven2 dependencies to use. */
  private Maven2DependenciesWizardPage dependenciesPage;

  /**
   * Default constructor.
   *
   * Sets the title and image of the wizard.
   */
  public Maven2ProjectWizard() {
    super();
    setWindowTitle(Messages.getString("wizard.project.title"));
    setDefaultPageImageDescriptor(DEFAULT_PAGE_IMAGE);
    setNeedsProgressMonitor(true);
  }

  public void addPages() {
    ResolverConfiguration resolverConfiguration = new ResolverConfiguration();
    
    locationPage = new Maven2ProjectWizardLocationPage(resolverConfiguration);
    artifactPage = new Maven2ProjectWizardArtifactPage(resolverConfiguration, locationPage);
    dependenciesPage = new Maven2DependenciesWizardPage(resolverConfiguration);

    addPage(locationPage);
    addPage(artifactPage);
    addPage(dependenciesPage);
  }

  /**
   * Creates the actual project resource for the given <code>project</code>
   * at the provided <code>location</code>.
   *
   * <p>
   * Note that the project is merely created but not yet configured, i.e. no
   * project nature is set on the project at this point as this would eventually
   * already trigger the builder on the project.
   * </p>
   * <p>
   * Once the project is created, it is also opened.
   * </p>
   *
   * @param project   The project for which the actual project resource is to
   *                  be created.
   * @param location  The path at which the project resource is to be created.
   *
   * @throws CoreException if some error occurs while creating or opening the project.
   *
   * @see org.eclipse.core.resources.IProject#create(org.eclipse.core.resources.IProjectDescription, org.eclipse.core.runtime.IProgressMonitor)
   * @see org.eclipse.core.resources.IProject#open(org.eclipse.core.runtime.IProgressMonitor)
   */
  private static void createProject(IProject project, IPath location) throws CoreException {
    IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(project.getName());

    description.setLocation(location);

    project.create(description, null);
    project.open(null);
  }

  /**
   * Creates the actual Maven2 project by creating a normal Java project based
   * on the given <code>project</code> and setting the Java as well as the
   * Maven2 natures on the project. The Java and Maven2 classpath containers
   * as well as the whole Java classpath are also set up.
   *
   * @param project           The project handle for the already existing
   *                          but unconfigured project resource.
   * @param classpathEntries  The classpath entries to include in the Java
   *                          project.
   * @param outputPath        The default output location path to set in the
   *                          .classpath file.
   *
   * @throws CoreException if some error occurs while configuring the Maven2 project.
   */
  private static void createMaven2Project(IProject project, IClasspathEntry[] classpathEntries, IPath outputPath)
      throws CoreException {
    IProjectDescription description = project.getDescription();
    description.setNatureIds(new String[] {JavaCore.NATURE_ID, Maven2Plugin.NATURE_ID});
    project.setDescription(description, null);

    IJavaProject javaProject = JavaCore.create(project);

    javaProject.setRawClasspath(addContainersToClasspath(classpathEntries), outputPath, new NullProgressMonitor());
  }

  /**
   * Creates a POM file for the given <code>model</code> inside the provided
   * <code>project</code>.
   *
   * @param project  The project for which to create the POM file.
   * @param model    The POM to write to file.
   * @param dependencies 
   *
   * @throws CoreException if a POM file already exists in the given project
   *                       or if creating the file fails.
   */
  private static void createPOMFile(final IProject project, final Model model) throws CoreException {
    final IFile file = project.getFile(new Path(Maven2Plugin.POM_FILE_NAME));

    if(file.exists()) {
      throwCoreException(Messages.getString("wizard.project.error.pomExists"));
    }

    final File pom = file.getLocation().toFile();

    try {
      StringWriter w = new StringWriter();
      MavenEmbedder mavenEmbedder = Maven2Plugin.getDefault().getMavenEmbedderManager().getWorkspaceEmbedder();
      mavenEmbedder.writeModel(w, model, true);

      file.create(new ByteArrayInputStream(w.toString().getBytes("ASCII")), true, null);
    } catch(Exception ex) {
      Maven2Plugin.log("Unable to create POM " + pom + "; " + ex.getMessage(), ex);
    }
  }

  /**
   * Adds the Java and Maven2 classpath containers to the given classpath
   * <code>entries</code>.
   *
   * @param entries  A given set of classpath entries.
   * @return         An array containing all of the initially provided classpath
   *                 <code>entries</code> as well as the Java and Maven2
   *                 classpath containers.
   *                 Is never <code>null</code>.
   */
  private static IClasspathEntry[] addContainersToClasspath(IClasspathEntry[] entries) {
    IClasspathEntry[] classpath = new IClasspathEntry[entries.length + 2];
    System.arraycopy(entries, 0, classpath, 0, entries.length);

    classpath[classpath.length - 2] = JavaCore.newContainerEntry(new Path(JavaRuntime.JRE_CONTAINER));
    classpath[classpath.length - 1] = BuildPathManager.getDefaultContainerEntry();

    return classpath;
  }

  /**
   * {@inheritDoc}
   *
   * To perform the actual project creation, an operation is created and run
   * using this wizard as execution context. That way, messages about the
   * progress of the project creation are displayed inside the wizard.
   */
  public boolean performFinish() {
    // First of all, we extract all the information from the wizard pages.
    // Note that this should not be done inside the operation we will run
    // since many of the wizard pages' methods can only be invoked from within
    // the SWT event dispatcher thread. However, the operation spawns a new
    // separate thread to perform the actual work, i.e. accessing SWT elements
    // from withing that thread would lead to an exception.

    final IProject project = locationPage.getProjectHandle();

    final String projectName = locationPage.getProjectName();
    
    // Get the location where to create the project. For some reason, when using
    // the default workspace location for a project, we have to pass null
    // instead of the actual location.
    final IPath location = locationPage.isInWorkspace() ? null : locationPage.getLocationPath();

    final String[] directories = artifactPage.getDirectories();

    final Model model = artifactPage.getModel();
    model.getDependencies().addAll(Arrays.asList(dependenciesPage.getDependencies()));

    final IClasspathEntry[] classpathEntries = artifactPage.getClasspathEntries(project.getFullPath());

    final IPath outputPath = artifactPage.getDefaultOutputLocationPath(project.getFullPath());

    Job job = new Job("Creating project " + projectName) {
      public IStatus run(IProgressMonitor monitor) {
        try {
          doFinish(project, location, directories, model, classpathEntries, outputPath, monitor);
          return Status.OK_STATUS;
        } catch(CoreException e) {
          return e.getStatus();
        } finally {
          monitor.done();
        }
      }
    };
    
    job.addJobChangeListener(new JobChangeAdapter() {
      public void done(IJobChangeEvent event) {
        IStatus result = event.getResult();
        if(!result.isOK()) {
          MessageDialog.openError(getShell(), "Failed to create project " + projectName, result.getMessage());
        }
      }
    });

    job.schedule();
    
    return true;
  }

  /**
   * Performs the actual project creation.
   *
   * <p>
   * The following steps are executed in the given order:
   * <ul>
   *   <li>Create the actual project resource without configuring it yet.</li>
   *   <li>Create the required Maven2 directories.</li>
   *   <li>Create the POM file.</li>
   *   <li>Configure the Maven2 project.</li>
   *   <li>Add the Maven2 dependencies to the project.</li>
   * </ul>
   * </p>
   *
   * @param project           The handle for the project to create.
   * @param location          The location at which to create the project.
   * @param directories       The Maven2 directories to create.
   * @param model             The POM containing the project artifact information.
   * @param classpathEntries  The classpath entries of the project.
   * @param outputPath        The default output location path to set in the
   *                          .classpath file of the project.
   * @param monitor           The monitor for displaying the project creation
   *                          progress.
   *
   * @throws CoreException if any of the above listed actions fails.
   */
  static void doFinish(IProject project, IPath location, String[] directories, Model model,
      IClasspathEntry[] classpathEntries, IPath outputPath, IProgressMonitor monitor) throws CoreException {
    monitor.beginTask(Messages.getString("wizard.project.monitor.create"), 5);

    monitor.subTask(Messages.getString("wizard.project.monitor.createProject"));
    createProject(project, location);
    monitor.worked(1);

    monitor.subTask(Messages.getString("wizard.project.monitor.createDirectories"));
    for(int i = 0; i < directories.length; i++ ) {
      Util.createFolder(project.getFolder(directories[i]));
    }
    monitor.worked(1);

    monitor.subTask(Messages.getString("wizard.project.monitor.createPOM"));
    createPOMFile(project, model);
    monitor.worked(1);

    monitor.subTask(Messages.getString("wizard.project.monitor.configureMaven2"));
    createMaven2Project(project, classpathEntries, outputPath);
    monitor.worked(1);
  }

  /**
   * Helper method which throws a <code>CoreException</code> while setting
   * an appropriate error status.
   *
   * @param message  An error message indicating the reason of the exception.
   *
   * @throws CoreException by definition ;)
   */
  private static void throwCoreException(String message) throws CoreException {
    IStatus status = new Status(IStatus.ERROR, "org.maven.ide.eclipse", IStatus.OK, message, null);
    throw new CoreException(status);
  }

  public void init(IWorkbench workbench, IStructuredSelection selection) {
    // do nothing
  }
}
