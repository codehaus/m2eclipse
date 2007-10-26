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

package org.maven.ide.eclipse.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.maven.model.Model;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.embedder.BuildPathManager;
import org.maven.ide.eclipse.embedder.ResolverConfiguration;

import junit.framework.TestCase;

public abstract class AsbtractMavenProjectTestCase extends TestCase {
  protected IWorkspace workspace;

  protected void setUp() throws Exception {
    super.setUp();
    workspace = ResourcesPlugin.getWorkspace();

    // lets not assume we've got subversion in the target platform 
    Hashtable options = JavaCore.getOptions();
    options.put(JavaCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER, ".svn/");
    JavaCore.setOptions(options);
  }

  protected void tearDown() throws Exception {
    super.tearDown();

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        IProject[] projects = workspace.getRoot().getProjects();
        for(int i = 0; i < projects.length; i++ ) {
          projects[i].delete(false, true, monitor);
        }
      }
    }, new NullProgressMonitor());
  }

  protected void deleteProject(String projectName) throws CoreException {
    final IProject project = workspace.getRoot().getProject(projectName);
  
    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        if(project.exists()) {
          deleteMember(".classpath", project, monitor);
          deleteMember(".project", project, monitor);
          project.delete(false, true, monitor);
        }
      }
  
      private void deleteMember(String name, final IProject project, IProgressMonitor monitor) throws CoreException {
        IResource member = project.findMember(name);
        if(member.exists()) {
          member.delete(true, monitor);
        }
      }
    }, new NullProgressMonitor());
  }

  protected IProject createProject(String projectName, final String pomResource) throws CoreException {
    final IProject project = workspace.getRoot().getProject(projectName);
  
    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        project.create(monitor);
  
        if(!project.isOpen()) {
          project.open(monitor);
        }
  
        IFile pomFile = project.getFile("pom.xml");
        if(!pomFile.exists()) {
          InputStream is = null;
          try {
            is = new FileInputStream(pomResource);
            pomFile.create(is, true, monitor);
          } catch(FileNotFoundException ex) {
            throw new CoreException(new Status(IStatus.ERROR, "", 0, ex.toString(), ex));
          } finally {
            try {
              is.close();
            } catch(IOException ex) {
              // ignore
            }
          }
        }
      }
    }, null);
  
    return project;
  }

  protected IProject importProject(String pomName, ResolverConfiguration configuration) throws CoreException {
    Maven2Plugin plugin = Maven2Plugin.getDefault();
    BuildPathManager buildpathManager = plugin.getBuildpathManager();
    File pomFile = new File(pomName);
    Model model = plugin.getMavenModelManager().readMavenModel(pomFile);
    IProject project = buildpathManager.importProject(pomFile, model, configuration, new NullProgressMonitor());
    assertNotNull("Failed to import project " + pomFile, project);
    return project;
  }

  protected void waitForJobsToComplete() throws InterruptedException {
    IJobManager jobManager = Job.getJobManager();
    boolean running;
    do {
      running = false;
      Job[] jobs = jobManager.find(null);
      for(int i = 0; i < jobs.length; i++ ) {
        Job job = jobs[i];
        if(!job.isSystem()) {
          while(job.getState() != Job.NONE) {
            running = true;
            Thread.sleep(50L);
          }
        }
      }
    } while (running);
  }

  private void waitForJob(String jobName) throws InterruptedException {
      IJobManager jobManager = Job.getJobManager();
      Job[] jobs = jobManager.find(null);
  //    System.err.println(Arrays.asList(jobs));
  
      Job job = findJob(jobs, jobName);
      if(job != null) {
        while(job.getState() != Job.NONE) {
          Thread.sleep(50L);
        }
        assertEquals(Status.OK_STATUS, job.getResult());
      }
    }

  private Job findJob(Job[] jobs, String name) {
    for(int i = 0; i < jobs.length; i++ ) {
      Job job = jobs[i];
      if(name.equals(job.getName()))
        return job;
    }
    return null;
  }

  private List getResources(IProject project) throws CoreException {
    final List resources = new ArrayList();
    project.accept(new IResourceVisitor() {
      public boolean visit(IResource resource) throws CoreException {
        resources.add(resource);
        return true;
      }
    }, IResource.DEPTH_INFINITE, IContainer.EXCLUDE_DERIVED);
    return resources;
  }

  protected IClasspathEntry[] getMavenContainerEntries(IProject project) throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    return container.getClasspathEntries();
  }

}
