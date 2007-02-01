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

package org.maven.ide.eclipse.embedder;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidArtifactRTException;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.container.Maven2ClasspathContainer;
import org.maven.ide.eclipse.container.Maven2ClasspathContainerInitializer;
import org.maven.ide.eclipse.launch.console.Maven2Console;
import org.maven.ide.eclipse.preferences.Maven2PreferenceConstants;
import org.maven.ide.eclipse.util.Util;


public class ClassPathResolver {
  private final MavenEmbedderManager embedderManager;
  private final Maven2Console console;
  private final MavenModelManager mavenModelManager;
  private final IPreferenceStore preferenceStore;

  
  public ClassPathResolver(MavenEmbedderManager embedderManager, Maven2Console console,
      MavenModelManager mavenModelManager, IPreferenceStore preferenceStore) {
    this.embedderManager = embedderManager;
    this.console = console;
    this.mavenModelManager = mavenModelManager;
    this.preferenceStore = preferenceStore;
  }
  
  public void updateClasspathContainer(IProject project, boolean recursive, IProgressMonitor monitor) throws CoreException {
    IFile pomFile = project.getFile(Maven2Plugin.POM_FILE_NAME);

    Set entries = new HashSet();
    Map moduleArtifacts = new HashMap();

    Set dependentProjects = mavenModelManager.getDependentProjects(pomFile);
    
    Util.deleteMarkers(project);
    try {
      mavenModelManager.updateMavenModel(pomFile, true, monitor);
    } catch(CoreException ex) {
      Util.addMarker(pomFile, ex.getMessage(), 1, IMarker.SEVERITY_ERROR);
    }
    
    resolveClasspathEntries(entries, moduleArtifacts, pomFile, pomFile, true, monitor);

    dependentProjects.addAll(mavenModelManager.getDependentProjects(pomFile));

    Maven2ClasspathContainer container = new Maven2ClasspathContainer(entries);

    JavaCore.setClasspathContainer(container.getPath(), new IJavaProject[] {JavaCore.create(project)},
        new IClasspathContainer[] {container}, monitor);
    
    for(Iterator it = dependentProjects.iterator(); it.hasNext();) {
      IProject p = (IProject) it.next();
      updateClasspathContainer(p, recursive, monitor);
    }
  }
  

  private void resolveClasspathEntries(Set libraryEntries, Map moduleArtifacts, IFile rootPomFile, IFile pomFile, boolean recursive,
      IProgressMonitor monitor) {
    if(monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    console.logMessage("Reading " + pomFile.getFullPath());
    monitor.subTask("Reading " + pomFile.getFullPath());

    IProject currentProject = pomFile.getProject();
    try {
      boolean offline = preferenceStore.getBoolean(Maven2PreferenceConstants.P_OFFLINE);
      boolean downloadSources = !offline & preferenceStore.getBoolean(Maven2PreferenceConstants.P_DOWNLOAD_SOURCES);
      boolean downloadJavadoc = !offline & preferenceStore.getBoolean(Maven2PreferenceConstants.P_DOWNLOAD_JAVADOC);
      boolean debug = preferenceStore.getBoolean(Maven2PreferenceConstants.P_DEBUG_OUTPUT);

      MavenProject mavenProject = mavenModelManager.readMavenProject(pomFile, monitor, offline, debug);
      if(mavenProject == null) {
        return;
      }

      MavenEmbedder embedder = embedderManager.getProjectEmbedder();
      
      // deleteMarkers(pomFile);
      // TODO use version?
      moduleArtifacts.put(mavenProject.getGroupId() + ":" + mavenProject.getArtifactId(), mavenProject.getArtifact());

      Set artifacts = mavenProject.getArtifacts();
      for(Iterator it = artifacts.iterator(); it.hasNext();) {
        if(monitor.isCanceled()) {
          throw new OperationCanceledException();
        }

        Artifact a = (Artifact) it.next();

        monitor.subTask("Processing " + a.getId());

        // The artifact filename cannot be used here to determine
        // the type because eclipse project artifacts don't have jar or zip file names.
        // TODO use version?
        String artifactKey = a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getVersion() + ":" + a.getType();
        ArtifactHandler artifactHandler = embedder.getArtifactHandler(a);
        if(!moduleArtifacts.containsKey(artifactKey) 
            && artifactHandler.isAddedToClasspath()
            && ("jar".equals(artifactHandler.getExtension()) || "zip".equals(artifactHandler.getExtension()))) {

          moduleArtifacts.put(artifactKey, a);
          mavenModelManager.addProjectArtifact(pomFile, a);
          // this is needed to projects with have modules (either inner or external)
          mavenModelManager.addProjectArtifact(rootPomFile, a);
          
          IFile artifactPomFile = mavenModelManager.getArtifactFile(a);
          if(artifactPomFile != null) {
            IProject artifactProject = artifactPomFile.getProject();
            if(artifactProject.getFullPath().equals(currentProject.getFullPath())) {
              // This is another artifact in our current project so we should not
              // add our own project to ourself
              continue;
            }

            libraryEntries.add(JavaCore.newProjectEntry(artifactProject.getFullPath(), false));
            continue;
          }

          Path srcPath = materializeArtifactPath(embedder, mavenProject, a, "java-source", "sources", downloadSources, monitor);

          String artifactLocation = a.getFile().getAbsolutePath();
          
          IClasspathAttribute[] attributes = new IClasspathAttribute[0];
          if(srcPath == null) { // no need to search for javadoc if we have source code
            Path javadocPath = materializeArtifactPath(embedder, mavenProject, a, "java-doc", "javadoc", downloadJavadoc, monitor);
            String javaDocUrl = null;
            if(javadocPath != null) {
              javaDocUrl = Maven2ClasspathContainerInitializer.getJavaDocUrl(javadocPath.toString());
            } else {
              javaDocUrl = getJavaDocUrl(artifactLocation, monitor);
            }
            if(javaDocUrl != null) {
              attributes = new IClasspathAttribute[] {JavaCore.newClasspathAttribute(
                  IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, javaDocUrl)};
            }
          }

          libraryEntries.add(JavaCore.newLibraryEntry(new Path(artifactLocation), srcPath, null, new IAccessRule[0],
              attributes, false /*not exported*/));
        }
      }

      if(recursive) {
        IContainer parent = pomFile.getParent();

        List modules = mavenProject.getModules();
        for(Iterator it = modules.iterator(); it.hasNext() && !monitor.isCanceled();) {
          if(monitor.isCanceled()) {
            throw new OperationCanceledException();
          }

          String module = (String) it.next();
          IResource memberPom = parent.findMember(module + "/" + Maven2Plugin.POM_FILE_NAME); //$NON-NLS-1$
          if(memberPom != null && memberPom.getType() == IResource.FILE) {
            resolveClasspathEntries(libraryEntries, moduleArtifacts, rootPomFile, (IFile) memberPom, true, monitor);
          }
        }
      }

    } catch(OperationCanceledException ex) {
      throw ex;

    } catch(InvalidArtifactRTException ex) {
      // TODO move into ReadProjectTask
      Util.deleteMarkers(pomFile);
      Util.addMarker(pomFile, ex.getBaseMessage(), 1, IMarker.SEVERITY_ERROR);
      console.logError("Unable to read model; " + ex.toString());

    } catch(Throwable ex) {
      // TODO move into ReadProjectTask
      Util.deleteMarkers(pomFile);
      Util.addMarker(pomFile, "Unable to read model; " + ex.toString(), 1, IMarker.SEVERITY_ERROR);
      
      String msg = "Unable to read model from " + pomFile.getFullPath();
      Maven2Plugin.log(msg, ex);
      console.logError(msg + "; " + ex.toString());

    } finally {
      monitor.done();

    }
  }

  // type = "java-source"
  private Path materializeArtifactPath(MavenEmbedder embedder, MavenProject mavenProject, Artifact a, String type,
      String suffix, boolean download, IProgressMonitor monitor) throws Exception {
    String artifactLocation = a.getFile().getAbsolutePath();
    // artifactLocation ends on '.jar' or '.zip'
    File file = new File(artifactLocation.substring(0, artifactLocation.length() - 4) + "-" + suffix + ".jar");
    if(file.exists()) {
      // XXX ugly hack to do not download any artifacts
      return new Path(file.getAbsolutePath());
    } else if(download) {
      monitor.beginTask("Resolve " + type + " " + a.getId(), IProgressMonitor.UNKNOWN);
      try {
        Artifact f = embedder.createArtifactWithClassifier(a.getGroupId(), a.getArtifactId(), a.getVersion(),
            type, suffix);
        if(f != null) {
          embedder.resolve(f, mavenProject.getRemoteArtifactRepositories(), embedder.getLocalRepository());
          return new Path(f.getFile().getAbsolutePath());
        }
      } catch(AbstractArtifactResolutionException ex) {
        String name = ex.getGroupId() + ":" + ex.getArtifactId() + "-" + ex.getVersion() + "." + ex.getType();
        console.logError(ex.getOriginalMessage() + " " + name);
      } finally {
        monitor.done();
      }
    }
    return null;
  }
  
  private String getJavaDocUrl(String artifactLocation, IProgressMonitor monitor) throws CoreException {
    // guess the javadoc url from the project url in the artifact's pom.xml
    File file = new File(artifactLocation.substring(0, artifactLocation.length()-4) + ".pom");
    if(file.exists()) {
      Model model = mavenModelManager.readMavenModel(file);
      String url = model.getUrl();
      if(url!=null) {
        url = url.trim();
        if(url.length()>0) {
          if(!url.endsWith("/")) url += "/";
          return url + "apidocs/";  // assuming project is using maven-generated site
        }
      }              
    }
    return null;
  }

}

