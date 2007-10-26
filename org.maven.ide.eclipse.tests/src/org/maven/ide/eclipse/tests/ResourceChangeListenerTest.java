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

import java.io.FileInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.embedder.ResolverConfiguration;


public class ResourceChangeListenerTest extends AsbtractMavenProjectTestCase {
  
  private IProject project;

  protected void setUp() throws Exception {
    super.setUp();
    workspace.getRoot().getProject("resourcechange").delete(true, null);
    project = createProject("resourcechange", "projects/resourcechange/pom.xml");
    Maven2Plugin.getDefault().getBuildpathManager().configureProject(project, new ResolverConfiguration(), new NullProgressMonitor());
    waitForJobsToComplete();
  }

  protected void tearDown() throws Exception {
    workspace.getRoot().getProject("resourcechange").delete(true, null);
    super.tearDown();
  }

  public void testMarkerOnlyChange() throws Exception {
    // modify
    IFile pom = project.getFile("pom.xml");
    pom.createMarker(Maven2Plugin.MARKER_ID);
    waitForJobsToComplete();

    // ideally, I need to test that the container did not refresh
  }

  public void testPomChanges() throws Exception {
    // modify
    IFile pom = project.getFile("pom.xml");

    InputStream contents = new FileInputStream("projects/resourcechange/pom001.xml");
    pom.setContents(contents, IResource.NONE, null);
    contents.close();

    // assert
    waitForJobsToComplete();
    IClasspathEntry[] cp = getMavenContainerEntries(project);
    assertEquals(1, cp.length);
    assertEquals("junit-4.1.jar", cp[0].getPath().lastSegment());
  }

  public void testPomMove001() throws Exception {
    // setup some more
    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        try {
          InputStream contents = new FileInputStream("projects/resourcechange/pom001.xml");
          IFile pom001 = project.getFile("pom001.xml");
          pom001.create(contents, true, monitor);
        } catch(Exception e) {
          throw new CoreException(new Status(Status.ERROR, Maven2Plugin.PLUGIN_ID, e.getMessage(), e));
        }
      }
    }, null);
    waitForJobsToComplete();

    // modify
    project.getFile("pom.xml").delete(true, null);
    IFile pom001 = project.getFile("pom001.xml");
    pom001.move(project.getFile("pom.xml").getFullPath(), true, null);
    waitForJobsToComplete();

    // assert
    waitForJobsToComplete();
    IClasspathEntry[] cp = getMavenContainerEntries(project);
    assertEquals(1, cp.length);
    assertEquals("junit-4.1.jar", cp[0].getPath().lastSegment());
  }

  public void testPomMove002() throws Exception {
    // setup some more
    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        try {
          InputStream contents = new FileInputStream("projects/resourcechange/pom001.xml");
          IFile pom001 = project.getFile("pom001.xml");
          pom001.create(contents, true, monitor);
        } catch(Exception e) {
          throw new CoreException(new Status(Status.ERROR, Maven2Plugin.PLUGIN_ID, e.getMessage(), e));
        }
      }
    }, null);
    waitForJobsToComplete();

    // modify
    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        project.getFile("pom.xml").delete(true, null);
        IFile pom001 = project.getFile("pom001.xml");
        pom001.move(project.getFile("pom.xml").getFullPath(), true, null);
      }
    }, null);
    waitForJobsToComplete();

    // assert
    waitForJobsToComplete();
    IClasspathEntry[] cp = getMavenContainerEntries(project);
    assertEquals(1, cp.length);
    assertEquals("junit-4.1.jar", cp[0].getPath().lastSegment());
  }

  public void testPomDelete() throws Exception {
    // just in case, make sure we imported the right project
    IClasspathEntry[] cp = getMavenContainerEntries(project);
    assertEquals(1, cp.length);
    assertEquals("junit-3.8.1.jar", cp[0].getPath().lastSegment());

    // change
    project.getFile("pom.xml").delete(true, null);

    // assert
    waitForJobsToComplete();
    assertEquals(0, getMavenContainerEntries(project).length);
  }

  public void testPomRename() throws Exception {
    // just in case, make sure we imported the right project
    IClasspathEntry[] cp = getMavenContainerEntries(project);
    assertEquals(1, cp.length);
    assertEquals("junit-3.8.1.jar", cp[0].getPath().lastSegment());

    // change
    project.getFile("pom.xml").move(project.getFullPath().append("backup"), true, null);

    // assert
    waitForJobsToComplete();
    assertEquals(0, getMavenContainerEntries(project).length);
  }
}
