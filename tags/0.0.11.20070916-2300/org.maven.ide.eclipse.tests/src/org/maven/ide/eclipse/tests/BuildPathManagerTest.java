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
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.maven.model.Model;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
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


/**
 * @author Eugene Kuleshov
 */
public class BuildPathManagerTest extends TestCase {

  private static final boolean WORKSPACE = true;

  private static final boolean NO_WORKSPACE = false;

  private static final boolean INCLUDE_MODULES = true;

  private static final boolean NO_MODULES = false;

  private IWorkspace workspace;

  protected void setUp() throws Exception {
    super.setUp();
    workspace = ResourcesPlugin.getWorkspace();

    deleteProject("MNGECLIPSE-20");
    deleteProject("MNGECLIPSE-20-app");
    deleteProject("MNGECLIPSE-20-ear");
    deleteProject("MNGECLIPSE-20-ejb");
    deleteProject("MNGECLIPSE-20-type");
    deleteProject("MNGECLIPSE-20-web");

    deleteProject("MNGECLIPSE-248parent");
    deleteProject("MNGECLIPSE-248child");

    deleteProject("MNGECLIPSE-353");
  }

  protected void tearDown() throws Exception {
    super.tearDown();

    deleteProject("MNGECLIPSE-20");
    deleteProject("MNGECLIPSE-20-app");
    deleteProject("MNGECLIPSE-20-ear");
    deleteProject("MNGECLIPSE-20-ejb");
    deleteProject("MNGECLIPSE-20-type");
    deleteProject("MNGECLIPSE-20-web");

    deleteProject("MNGECLIPSE-248parent");
    deleteProject("MNGECLIPSE-248child");

    deleteProject("MNGECLIPSE-353");
  }

  public void testEnableMavenNature() throws Exception {
    final IProject project1 = createProject("MNGECLIPSE-248parent", "projects/MNGECLIPSE-248parent/pom.xml");
    final IProject project2 = createProject("MNGECLIPSE-248child", "projects/MNGECLIPSE-248child/pom.xml");

    NullProgressMonitor monitor = new NullProgressMonitor();
    BuildPathManager buildpathManager = Maven2Plugin.getDefault().getBuildpathManager();

    ResolverConfiguration configuration = new ResolverConfiguration();
    buildpathManager.enableMavenNature(project1, configuration, monitor);
//    buildpathManager.updateSourceFolders(project1, monitor);

    buildpathManager.enableMavenNature(project2, configuration, monitor);
//    buildpathManager.updateSourceFolders(project2, monitor);

    waitForJob("Initializing " + project1.getProject().getName());
    waitForJob("Initializing " + project2.getProject().getName());

    IClasspathEntry[] project1entries = getMavenContainerEntries(project1);
    assertEquals(1, project1entries.length);
    assertEquals(IClasspathEntry.CPE_LIBRARY, project1entries[0].getEntryKind());
    assertTrue(project1entries[0].getPath().lastSegment().equals("junit-4.1.jar"));

    IClasspathEntry[] project2entries = getMavenContainerEntries(project2);
    assertEquals(2, project2entries.length);
    assertEquals(IClasspathEntry.CPE_PROJECT, project2entries[0].getEntryKind());
    assertTrue(project2entries[0].getPath().segment(0).equals("MNGECLIPSE-248parent"));
    assertEquals(IClasspathEntry.CPE_LIBRARY, project2entries[1].getEntryKind());
    assertTrue(project2entries[1].getPath().lastSegment().equals("junit-4.1.jar"));
  }

  public void testEnableMavenNatureWithNoWorkspace() throws Exception {
    final IProject project1 = createProject("MNGECLIPSE-248parent", "projects/MNGECLIPSE-248parent/pom.xml");
    final IProject project2 = createProject("MNGECLIPSE-248child", "projects/MNGECLIPSE-248child/pom.xml");

    NullProgressMonitor monitor = new NullProgressMonitor();
    BuildPathManager buildpathManager = Maven2Plugin.getDefault().getBuildpathManager();

    ResolverConfiguration configuration = new ResolverConfiguration(NO_MODULES, NO_WORKSPACE, "");
    buildpathManager.enableMavenNature(project1, configuration, monitor);
    buildpathManager.enableMavenNature(project2, configuration, monitor);
//    buildpathManager.updateSourceFolders(project1, monitor);
//    buildpathManager.updateSourceFolders(project2, monitor);

    waitForJob("Initializing " + project1.getProject().getName());
    waitForJob("Initializing " + project2.getProject().getName());

    IClasspathEntry[] project1entries = getMavenContainerEntries(project1);
    assertEquals(1, project1entries.length);
    assertEquals(IClasspathEntry.CPE_LIBRARY, project1entries[0].getEntryKind());
    assertTrue(project1entries[0].getPath().lastSegment().equals("junit-4.1.jar"));

    IClasspathEntry[] project2entries = getMavenContainerEntries(project2);
    assertEquals(2, project2entries.length);
    assertEquals(IClasspathEntry.CPE_LIBRARY, project2entries[0].getEntryKind());
    assertTrue(project2entries[0].getPath().lastSegment().equals("junit-4.1.jar"));
    assertEquals(IClasspathEntry.CPE_LIBRARY, project2entries[1].getEntryKind());
    assertTrue(project2entries[1].getPath().lastSegment().equals("MNGECLIPSE-248parent-1.0.0.jar"));
  }

  public void testProjectImportDefault() throws Exception {
    ResolverConfiguration configuration = new ResolverConfiguration();
    IProject project1 = importProject("projects/MNGECLIPSE-20/pom.xml", configuration);
    IProject project2 = importProject("projects/MNGECLIPSE-20/type/pom.xml", configuration);
    IProject project3 = importProject("projects/MNGECLIPSE-20/app/pom.xml", configuration);
    IProject project4 = importProject("projects/MNGECLIPSE-20/web/pom.xml", configuration);
    IProject project5 = importProject("projects/MNGECLIPSE-20/ejb/pom.xml", configuration);
    IProject project6 = importProject("projects/MNGECLIPSE-20/ear/pom.xml", configuration);

    {
      IJavaProject javaProject = JavaCore.create(project1);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(0, classpathEntries.length);

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(2, rawClasspath.length);
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[0].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[1].getPath().toString());

      IMarker[] markers = project1.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(0, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(project2);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(0, classpathEntries.length);

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(3, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-type/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[1].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[2].getPath().toString());

      IMarker[] markers = project2.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(0, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(project3);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(3, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-type", classpathEntries[0].getPath().lastSegment());
      assertEquals("junit-3.8.1.jar", classpathEntries[1].getPath().lastSegment());
      assertEquals("log4j-1.2.4.jar", classpathEntries[2].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(3, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-app/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[1].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[2].getPath().toString());

      IMarker[] markers = project3.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(0, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(project4);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(3, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-app", classpathEntries[0].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-type", classpathEntries[1].getPath().lastSegment());
      assertEquals("log4j-1.2.4.jar", classpathEntries[2].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(3, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-web/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[1].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[2].getPath().toString());

      IMarker[] markers = project4.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(0, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(project5);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(3, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-app", classpathEntries[0].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-type", classpathEntries[1].getPath().lastSegment());
      assertEquals("log4j-1.2.4.jar", classpathEntries[2].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(4, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-ejb/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("/MNGECLIPSE-20-ejb/src/main/resources", rawClasspath[1].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[2].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[3].getPath().toString());

      IMarker[] markers = project5.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals("" + Arrays.asList(markers), 0, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(project6);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(4, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-app", classpathEntries[0].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-ejb", classpathEntries[1].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-type", classpathEntries[2].getPath().lastSegment());
      assertEquals("log4j-1.2.4.jar", classpathEntries[3].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(2, rawClasspath.length);
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[0].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[1].getPath().toString());

      IMarker[] markers = project6.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals("" + Arrays.asList(markers), 0, markers.length);
    }
  }

  public void testProjectImportNoWorkspaceResolution() throws Exception {
    ResolverConfiguration configuration = new ResolverConfiguration(NO_MODULES, NO_WORKSPACE, "");
    IProject project1 = importProject("projects/MNGECLIPSE-20/pom.xml", configuration);
    IProject project2 = importProject("projects/MNGECLIPSE-20/type/pom.xml", configuration);
    IProject project3 = importProject("projects/MNGECLIPSE-20/app/pom.xml", configuration);
    IProject project4 = importProject("projects/MNGECLIPSE-20/web/pom.xml", configuration);
    IProject project5 = importProject("projects/MNGECLIPSE-20/ejb/pom.xml", configuration);
    IProject project6 = importProject("projects/MNGECLIPSE-20/ear/pom.xml", configuration);

    {
      IJavaProject javaProject = JavaCore.create(project1);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(0, classpathEntries.length);

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(2, rawClasspath.length);
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[0].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER/noworkspace", rawClasspath[1].getPath().toString());

      IMarker[] markers = project1.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(0, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(project2);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(0, classpathEntries.length);

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(3, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-type/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[1].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER/noworkspace", rawClasspath[2].getPath().toString());

      IMarker[] markers = project2.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(0, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(project3);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(3, classpathEntries.length);
      assertEquals("junit-3.8.1.jar", classpathEntries[0].getPath().lastSegment());
      assertEquals("log4j-1.2.4.jar", classpathEntries[1].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-type-0.0.1-SNAPSHOT.jar", classpathEntries[2].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(3, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-app/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[1].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER/noworkspace", rawClasspath[2].getPath().toString());

      IMarker[] markers = project3.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(2, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(project4);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(3, classpathEntries.length);
      assertEquals("log4j-1.2.4.jar", classpathEntries[0].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-app-0.0.1-SNAPSHOT.jar", classpathEntries[1].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-type-0.0.1-SNAPSHOT.jar", classpathEntries[2].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(3, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-web/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[1].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER/noworkspace", rawClasspath[2].getPath().toString());

      IMarker[] markers = project4.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(2, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(project5);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(3, classpathEntries.length);
      assertEquals("log4j-1.2.4.jar", classpathEntries[0].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-app-0.0.1-SNAPSHOT.jar", classpathEntries[1].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-type-0.0.1-SNAPSHOT.jar", classpathEntries[2].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(4, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-ejb/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("/MNGECLIPSE-20-ejb/src/main/resources", rawClasspath[1].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[2].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER/noworkspace", rawClasspath[3].getPath().toString());

      IMarker[] markers = project5.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(2, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(project6);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(4, classpathEntries.length);
      assertEquals("log4j-1.2.4.jar", classpathEntries[0].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-app-0.0.1-SNAPSHOT.jar", classpathEntries[1].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-ejb-0.0.1-SNAPSHOT.jar", classpathEntries[2].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-type-0.0.1-SNAPSHOT.jar", classpathEntries[3].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(2, rawClasspath.length);
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[0].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER/noworkspace", rawClasspath[1].getPath().toString());

      IMarker[] markers = project6.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals("" + Arrays.asList(markers), 2, markers.length);
    }
  }

  public void testProjectImportWithModules() throws Exception {
    ResolverConfiguration configuration = new ResolverConfiguration(INCLUDE_MODULES, WORKSPACE, "");
    IProject project = importProject("projects/MNGECLIPSE-20/pom.xml", configuration);

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
        .getClasspathEntries();
    assertEquals(2, classpathEntries.length);
    assertEquals("junit-3.8.1.jar", classpathEntries[0].getPath().lastSegment());
    assertEquals("log4j-1.2.4.jar", classpathEntries[1].getPath().lastSegment());

    IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
    assertEquals(7, rawClasspath.length);
    assertEquals("/MNGECLIPSE-20/type/src/main/java", rawClasspath[0].getPath().toString());
    assertEquals("/MNGECLIPSE-20/app/src/main/java", rawClasspath[1].getPath().toString());
    assertEquals("/MNGECLIPSE-20/web/src/main/java", rawClasspath[2].getPath().toString());
    assertEquals("/MNGECLIPSE-20/ejb/src/main/java", rawClasspath[3].getPath().toString());
    assertEquals("/MNGECLIPSE-20/ejb/src/main/resources", rawClasspath[4].getPath().toString());
    assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[5].getPath().toString());
    assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER/modules", rawClasspath[6].getPath().toString());

    IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(0, markers.length);
  }

  public void testProjectImportWithModulesNoWorkspaceResolution() throws Exception {
    ResolverConfiguration configuration = new ResolverConfiguration(INCLUDE_MODULES, NO_WORKSPACE, "");
    IProject project = importProject("projects/MNGECLIPSE-20/pom.xml", configuration);

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
        .getClasspathEntries();
    assertEquals("" + Arrays.asList(classpathEntries), 2, classpathEntries.length);
    assertEquals("junit-3.8.1.jar", classpathEntries[0].getPath().lastSegment());
    assertEquals("log4j-1.2.4.jar", classpathEntries[1].getPath().lastSegment());

    IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
    assertEquals(7, rawClasspath.length);
    assertEquals("/MNGECLIPSE-20/type/src/main/java", rawClasspath[0].getPath().toString());
    assertEquals("/MNGECLIPSE-20/app/src/main/java", rawClasspath[1].getPath().toString());
    assertEquals("/MNGECLIPSE-20/web/src/main/java", rawClasspath[2].getPath().toString());
    assertEquals("/MNGECLIPSE-20/ejb/src/main/java", rawClasspath[3].getPath().toString());
    assertEquals("/MNGECLIPSE-20/ejb/src/main/resources", rawClasspath[4].getPath().toString());
    assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[5].getPath().toString());
    assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER/modules/noworkspace", rawClasspath[6].getPath()
        .toString());

    IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(0, markers.length);
  }

  public void testProjectImportWithProfile1() throws Exception {
    ResolverConfiguration configuration = new ResolverConfiguration(NO_MODULES, WORKSPACE, "jaxb1");
    IProject project = importProject("projects/MNGECLIPSE-353/pom.xml", configuration);

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
        .getClasspathEntries();
    assertEquals("" + Arrays.asList(classpathEntries), 2, classpathEntries.length);
    assertEquals("junit-3.8.1.jar", classpathEntries[0].getPath().lastSegment());

    IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(0, markers.length);
  }

  public void testProjectImportWithProfile2() throws Exception {
    ResolverConfiguration configuration = new ResolverConfiguration(NO_MODULES, WORKSPACE, "jaxb20");
    IProject project = importProject("projects/MNGECLIPSE-353/pom.xml", configuration);

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
        .getClasspathEntries();
    assertEquals("" + Arrays.asList(classpathEntries), 2, classpathEntries.length);
    assertEquals("junit-3.8.1.jar", classpathEntries[0].getPath().lastSegment());

    IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(0, markers.length);
  }

  public void testUpdateClasspathContainerWithModulesNoWorkspace() throws Exception {
    ResolverConfiguration configuration = new ResolverConfiguration(INCLUDE_MODULES, NO_WORKSPACE, "");
    IProject project = importProject("projects/MNGECLIPSE-20/pom.xml", configuration);

    BuildPathManager buildpathManager = Maven2Plugin.getDefault().getBuildpathManager();
    buildpathManager.updateClasspathContainer(project, new NullProgressMonitor());

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
        .getClasspathEntries();
    assertEquals("" + Arrays.asList(classpathEntries), 2, classpathEntries.length);
    assertEquals("junit-3.8.1.jar", classpathEntries[0].getPath().lastSegment());
    assertEquals("log4j-1.2.4.jar", classpathEntries[1].getPath().lastSegment());

    IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(0, markers.length);
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

  private IProject importProject(String pomName, ResolverConfiguration configuration) throws CoreException {
    Maven2Plugin plugin = Maven2Plugin.getDefault();
    BuildPathManager buildpathManager = plugin.getBuildpathManager();
    File pomFile = new File(pomName);
    Model model = plugin.getMavenModelManager().readMavenModel(pomFile);
    IProject project = buildpathManager.createProject(pomFile, model, configuration, new NullProgressMonitor());
    assertNotNull("Failed to import project " + pomFile, project);
    return project;
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

  private IClasspathEntry[] getMavenContainerEntries(IProject project) throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    return container.getClasspathEntries();
  }

  private void deleteProject(String projectName) throws CoreException {
    final IProject project = workspace.getRoot().getProject(projectName);

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        if(project.exists()) {
          project.delete(false, true, monitor);
        }
      }
    }, new NullProgressMonitor());

    new File("projects/" + projectName + "/.classpath").delete();
    new File("projects/" + projectName + "/.project").delete();
  }

  private IProject createProject(String projectName, final String pomResource) throws CoreException {
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

//  private void assertFolder(IProject project, String name) {
//    IFolder folder = project.getFolder(name);
//    long t = System.currentTimeMillis();
//    while(!folder.exists() && (System.currentTimeMillis() - t) < 10000L) {
//      try {
//        Thread.sleep(1000L);
//      } catch(InterruptedException ex) {
//        // ignore
//      }
//    }
//    assertTrue("Expected to see folder " + folder, folder.exists());
//  }

}
