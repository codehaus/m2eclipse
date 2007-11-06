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

import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.embedder.BuildPathManager;
import org.maven.ide.eclipse.embedder.ResolverConfiguration;


/**
 * @author Eugene Kuleshov
 */
public class BuildPathManagerTest extends AsbtractMavenProjectTestCase {

  private static final boolean WORKSPACE = true;

  private static final boolean NO_WORKSPACE = false;

  private static final boolean INCLUDE_MODULES = true;

  private static final boolean NO_MODULES = false;

  public void testEnableMavenNature() throws Exception {
    deleteProject("MNGECLIPSE-248parent");
    deleteProject("MNGECLIPSE-248child");

    final IProject project1 = createProject("MNGECLIPSE-248parent", "projects/MNGECLIPSE-248parent/pom.xml");
    final IProject project2 = createProject("MNGECLIPSE-248child", "projects/MNGECLIPSE-248child/pom.xml");

    NullProgressMonitor monitor = new NullProgressMonitor();
    BuildPathManager buildpathManager = Maven2Plugin.getDefault().getBuildpathManager();

    ResolverConfiguration configuration = new ResolverConfiguration();
    buildpathManager.enableMavenNature(project1, configuration, monitor);
//    buildpathManager.updateSourceFolders(project1, monitor);

    buildpathManager.enableMavenNature(project2, configuration, monitor);
//    buildpathManager.updateSourceFolders(project2, monitor);

//    waitForJob("Initializing " + project1.getProject().getName());
//    waitForJob("Initializing " + project2.getProject().getName());
    waitForJobsToComplete();

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
    deleteProject("MNGECLIPSE-248parent");
    deleteProject("MNGECLIPSE-248child");

    final IProject project1 = createProject("MNGECLIPSE-248parent", "projects/MNGECLIPSE-248parent/pom.xml");
    final IProject project2 = createProject("MNGECLIPSE-248child", "projects/MNGECLIPSE-248child/pom.xml");

    NullProgressMonitor monitor = new NullProgressMonitor();
    BuildPathManager buildpathManager = Maven2Plugin.getDefault().getBuildpathManager();

    ResolverConfiguration configuration = new ResolverConfiguration(NO_MODULES, NO_WORKSPACE, "");
    buildpathManager.enableMavenNature(project1, configuration, monitor);
    buildpathManager.enableMavenNature(project2, configuration, monitor);
//    buildpathManager.updateSourceFolders(project1, monitor);
//    buildpathManager.updateSourceFolders(project2, monitor);

//    waitForJob("Initializing " + project1.getProject().getName());
//    waitForJob("Initializing " + project2.getProject().getName());
    waitForJobsToComplete();

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
    deleteProject("MNGECLIPSE-20");
    deleteProject("MNGECLIPSE-20-app");
    deleteProject("MNGECLIPSE-20-ear");
    deleteProject("MNGECLIPSE-20-ejb");
    deleteProject("MNGECLIPSE-20-type");
    deleteProject("MNGECLIPSE-20-web");

    ResolverConfiguration configuration = new ResolverConfiguration();
    IProject project1 = importProject("projects/MNGECLIPSE-20/pom.xml", configuration);
    IProject project2 = importProject("projects/MNGECLIPSE-20/type/pom.xml", configuration);
    IProject project3 = importProject("projects/MNGECLIPSE-20/app/pom.xml", configuration);
    IProject project4 = importProject("projects/MNGECLIPSE-20/web/pom.xml", configuration);
    IProject project5 = importProject("projects/MNGECLIPSE-20/ejb/pom.xml", configuration);
    IProject project6 = importProject("projects/MNGECLIPSE-20/ear/pom.xml", configuration);

    waitForJobsToComplete();
    
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
      assertEquals(toString(markers), 0, markers.length);
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
      assertEquals(toString(markers), 0, markers.length);
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
      assertEquals(toString(markers), 0, markers.length);
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
      assertEquals(toString(markers), 0, markers.length);
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
      assertEquals(toString(markers), 0, markers.length);
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
      assertEquals(toString(markers), 0, markers.length);
    }
  }

  private String toString(IMarker[] markers) {
    String sep = "";
    StringBuffer sb = new StringBuffer();
    for(int i = 0; i < markers.length; i++ ) {
      IMarker marker = markers[i];
      try {
        sb.append(sep).append(marker.getType()+":"+marker.getAttribute(IMarker.MESSAGE));
      } catch(CoreException ex) {
        // ignore
      }
      sep = ", ";
    }
    return sb.toString();
  }

  public void testProjectImportNoWorkspaceResolution() throws Exception {
    deleteProject("MNGECLIPSE-20");
    deleteProject("MNGECLIPSE-20-app");
    deleteProject("MNGECLIPSE-20-ear");
    deleteProject("MNGECLIPSE-20-ejb");
    deleteProject("MNGECLIPSE-20-type");
    deleteProject("MNGECLIPSE-20-web");

    ResolverConfiguration configuration = new ResolverConfiguration(NO_MODULES, NO_WORKSPACE, "");
    IProject project1 = importProject("projects/MNGECLIPSE-20/pom.xml", configuration);
    IProject project2 = importProject("projects/MNGECLIPSE-20/type/pom.xml", configuration);
    IProject project3 = importProject("projects/MNGECLIPSE-20/app/pom.xml", configuration);
    IProject project4 = importProject("projects/MNGECLIPSE-20/web/pom.xml", configuration);
    IProject project5 = importProject("projects/MNGECLIPSE-20/ejb/pom.xml", configuration);
    IProject project6 = importProject("projects/MNGECLIPSE-20/ear/pom.xml", configuration);

    waitForJobsToComplete();
    
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
      assertEquals(toString(markers), 0, markers.length);
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
      assertEquals(toString(markers), 0, markers.length);
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
      assertEquals(toString(markers), 2, markers.length);
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
      assertEquals(toString(markers), 2, markers.length);
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
      assertEquals(toString(markers), 2, markers.length);
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
      assertEquals(toString(markers), 2, markers.length);
    }
  }

  public void testProjectImportWithModules() throws Exception {
    deleteProject("MNGECLIPSE-20");
    deleteProject("MNGECLIPSE-20-app");
    deleteProject("MNGECLIPSE-20-ear");
    deleteProject("MNGECLIPSE-20-ejb");
    deleteProject("MNGECLIPSE-20-type");
    deleteProject("MNGECLIPSE-20-web");

    ResolverConfiguration configuration = new ResolverConfiguration(INCLUDE_MODULES, WORKSPACE, "");
    IProject project = importProject("projects/MNGECLIPSE-20/pom.xml", configuration);

    waitForJobsToComplete();
    
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
    assertEquals(toString(markers), 0, markers.length);
  }

  public void testProjectImportWithModulesNoWorkspaceResolution() throws Exception {
    deleteProject("MNGECLIPSE-20");
    deleteProject("MNGECLIPSE-20-app");
    deleteProject("MNGECLIPSE-20-ear");
    deleteProject("MNGECLIPSE-20-ejb");
    deleteProject("MNGECLIPSE-20-type");
    deleteProject("MNGECLIPSE-20-web");

    ResolverConfiguration configuration = new ResolverConfiguration(INCLUDE_MODULES, NO_WORKSPACE, "");
    IProject project = importProject("projects/MNGECLIPSE-20/pom.xml", configuration);

    waitForJobsToComplete();
    
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
    assertEquals(toString(markers), 0, markers.length);
  }

  public void testUpdateClasspathContainerWithModulesNoWorkspace() throws Exception {
    deleteProject("MNGECLIPSE-20");
    deleteProject("MNGECLIPSE-20-app");
    deleteProject("MNGECLIPSE-20-ear");
    deleteProject("MNGECLIPSE-20-ejb");
    deleteProject("MNGECLIPSE-20-type");
    deleteProject("MNGECLIPSE-20-web");

    ResolverConfiguration configuration = new ResolverConfiguration(INCLUDE_MODULES, NO_WORKSPACE, "");
    IProject project = importProject("projects/MNGECLIPSE-20/pom.xml", configuration);

    waitForJobsToComplete();
    
    BuildPathManager buildpathManager = Maven2Plugin.getDefault().getBuildpathManager();
    buildpathManager.updateClasspathContainer(project, new NullProgressMonitor());

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
        .getClasspathEntries();
    assertEquals("" + Arrays.asList(classpathEntries), 2, classpathEntries.length);
    assertEquals("junit-3.8.1.jar", classpathEntries[0].getPath().lastSegment());
    assertEquals("log4j-1.2.4.jar", classpathEntries[1].getPath().lastSegment());

    IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(toString(markers), 0, markers.length);
  }

  public void testProjectImportWithProfile1() throws Exception {
    deleteProject("MNGECLIPSE-353");
    
    ResolverConfiguration configuration = new ResolverConfiguration(NO_MODULES, WORKSPACE, "jaxb1");
    IProject project = importProject("projects/MNGECLIPSE-353/pom.xml", configuration);

    waitForJobsToComplete();
    
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
        .getClasspathEntries();
    assertEquals("" + Arrays.asList(classpathEntries), 2, classpathEntries.length);
    assertEquals("jaxb-api-1.5.jar", classpathEntries[0].getPath().lastSegment());
    assertEquals("junit-3.8.1.jar", classpathEntries[1].getPath().lastSegment());

    IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(toString(markers), 0, markers.length);
  }

  public void testProjectImportWithProfile2() throws Exception {
    deleteProject("MNGECLIPSE-353");

    ResolverConfiguration configuration = new ResolverConfiguration(NO_MODULES, WORKSPACE, "jaxb20");
    IProject project = importProject("projects/MNGECLIPSE-353/pom.xml", configuration);

    waitForJobsToComplete();
    
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
        .getClasspathEntries();
    assertEquals("" + Arrays.asList(classpathEntries), 4, classpathEntries.length);
    assertEquals("activation-1.1.jar", classpathEntries[0].getPath().lastSegment());
    assertEquals("jaxb-api-2.0.jar", classpathEntries[1].getPath().lastSegment());
    assertEquals("jsr173_api-1.0.jar", classpathEntries[2].getPath().lastSegment());
    assertEquals("junit-3.8.1.jar", classpathEntries[3].getPath().lastSegment());
    
    IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(toString(markers), 0, markers.length);
  }
  
  public void _testResourceFiltering() throws Exception {
    deleteProject("MNGECLIPSE-343");
    ResolverConfiguration configuration = new ResolverConfiguration();
    IProject project = importProject("projects/MNGECLIPSE-343/pom.xml", configuration);
    project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

    IJavaProject javaProject = JavaCore.create(project);
    IPath outputLocation = javaProject.getOutputLocation();
    Properties properties = new Properties();
    InputStream contents = workspace.getRoot().getFile(outputLocation.append("application.properties")).getContents();
    try {
      properties.load(contents);
    } finally {
      contents.close();
    }
    assertEquals("MNGECLIPSE-343 Test 001", properties.getProperty("application.name"));
    assertEquals("0.0.1-SNAPSHOT", properties.getProperty("application.version"));
  }

  public void testEmbedderException() throws Exception {
    deleteProject("MNGECLIPSE-157parent");

    importProject("projects/MNGECLIPSE-157parent/pom.xml", new ResolverConfiguration());
    IProject project = importProject("projects/MNGECLIPSE-157child/pom.xml", new ResolverConfiguration());
    waitForJobsToComplete();

    IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(toString(markers), 1, markers.length);
    assertEquals(toString(markers), "pom.xml", markers[0].getResource().getFullPath().lastSegment());
  }

}
