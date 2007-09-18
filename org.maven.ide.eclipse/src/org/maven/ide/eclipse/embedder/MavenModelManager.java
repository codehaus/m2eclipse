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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.index.MavenRepositoryIndexManager;
import org.maven.ide.eclipse.launch.console.Maven2Console;


/**
 * This class maintain the mapping between Eclipse projects and Maven models, and be able to lookup projects and models
 * or artifacts.
 * 
 * @author Scott Cytacki
 * @author Eugene Kuleshov
 */
public class MavenModelManager {
  private final MavenEmbedderManager embedderManager;

  private final MavenRepositoryIndexManager indexManager;

  private final Maven2Console console;

  /**
   * Map of the project pomFile location to the Model
   */
  private final Map models = new HashMap();

  /**
   * Map of the artifact keys to the pomFile in the Worspace for those artifacts.
   * 
   * @see #getArtifactKey(Artifact)
   */
  private final Map artifacts = new HashMap();

  private final Map projectsToArtifacts = new HashMap();

  private final Map artifactsToProjects = new HashMap();

  private boolean isInitialized = false;

  public MavenModelManager(MavenEmbedderManager embedderManager, MavenRepositoryIndexManager indexManager,
      Maven2Console console) {
    this.embedderManager = embedderManager;
    this.indexManager = indexManager;
    this.console = console;
  }

  public IFile getArtifactFile(Artifact a) {
    IFile file = (IFile) artifacts.get(getArtifactKey(a));
    return file != null && file.isAccessible() ? file : null;
  }

  public Model getMavenModel(IFile pomFile) {
    return (Model) models.get(getPomFileKey(pomFile));
  }

  public synchronized void initModels(IProgressMonitor monitor) {
    if(isInitialized) {
      return;
    }

    isInitialized = true;

    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();

    Map mavenModels = new HashMap();
    for(int i = 0; i < projects.length; i++ ) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      IProject project = projects[i];
      try {
        if(project.isOpen() && project.hasNature(Maven2Plugin.NATURE_ID)) {
          IFile pomFile = project.getFile(Maven2Plugin.POM_FILE_NAME);
          if(pomFile == null) {
            console.logError("Project " + project.getName() + " is missing pom.xml");
          } else {

            IJavaProject javaProject = JavaCore.create(project);
            ResolverConfiguration resolverConfiguration = BuildPathManager.getResolverConfiguration(javaProject);
            initMavenModel(pomFile, pomFile, mavenModels, monitor, resolverConfiguration);
          }
        }
      } catch(CoreException ex) {
        console.logError("Unable to read project " + project.getName() + "; " + ex.getMessage());
      }
    }

    Map mavenProjects = new HashMap();
    for(int i = 0; i < projects.length; i++ ) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      IProject project = projects[i];
      try {
        if(project.isOpen() && project.hasNature(Maven2Plugin.NATURE_ID)) {
          IFile pomFile = project.getFile(Maven2Plugin.POM_FILE_NAME);
          if(pomFile == null) {
            console.logError("Project " + project.getName() + " is missing pom.xml");
          } else {
            IJavaProject javaProject = JavaCore.create(project);
            ResolverConfiguration resolverConfiguration = BuildPathManager.getResolverConfiguration(javaProject);
            initMavenProject(pomFile, pomFile, mavenProjects, monitor, resolverConfiguration);
          }
        }
      } catch(CoreException ex) {
        console.logError("Unable to read project " + project.getName() + "; " + ex.getMessage());
      }
    }
  }

  private void initMavenModel(IFile pomFile, IFile rootPomFile, Map mavenModels, IProgressMonitor monitor,
      ResolverConfiguration resolverConfiguration) throws CoreException {
    String pomKey = getPomFileKey(pomFile);
    Model mavenModel = (Model) mavenModels.get(pomKey);
    if(mavenModel == null) {
      mavenModel = updateMavenModel(pomFile, false, monitor);
      mavenModels.put(pomKey, mavenModel);
    }

    if(resolverConfiguration.shouldIncludeModules()) {
      IContainer parent = pomFile.getParent();
      for(Iterator it = mavenModel.getModules().iterator(); it.hasNext();) {
        if(monitor.isCanceled()) {
          throw new OperationCanceledException();
        }
        String module = (String) it.next();
        IResource memberPom = parent.findMember(module + "/" + Maven2Plugin.POM_FILE_NAME); //$NON-NLS-1$
        if(memberPom != null && memberPom.getType() == IResource.FILE && memberPom.isAccessible()) {
          initMavenModel((IFile) memberPom, rootPomFile, mavenModels, monitor, resolverConfiguration);
        }
      }
    }
  }

  private void initMavenProject(IFile pomFile, IFile rootPomFile, Map mavenProjects, IProgressMonitor monitor,
      ResolverConfiguration resolverConfiguration) throws CoreException {
    String pomKey = getPomFileKey(pomFile);
    if(mavenProjects.containsKey(pomKey)) {
      return;
    }

    MavenExecutionResult result = readMavenProject(pomFile, monitor, true, false, resolverConfiguration);
    MavenProject mavenProject = result.getProject();
    if(mavenProject == null) {
      return;
    }

    mavenProjects.put(pomKey, mavenProject);

    Set artifacts = mavenProject.getArtifacts();
    for(Iterator it = artifacts.iterator(); it.hasNext();) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }
      Artifact artifact = (Artifact) it.next();
      addProjectArtifact(pomFile, artifact);
      addProjectArtifact(rootPomFile, artifact);
    }

    if(resolverConfiguration.shouldIncludeModules()) {
      IContainer parent = pomFile.getParent();
      for(Iterator it = mavenProject.getModules().iterator(); it.hasNext();) {
        if(monitor.isCanceled()) {
          throw new OperationCanceledException();
        }
        String module = (String) it.next();
        IResource memberPom = parent.findMember(module + "/" + Maven2Plugin.POM_FILE_NAME); //$NON-NLS-1$
        if(memberPom != null && memberPom.getType() == IResource.FILE && memberPom.isAccessible()) {
          initMavenProject((IFile) memberPom, rootPomFile, mavenProjects, monitor, resolverConfiguration);
        }
      }
    }
  }

  // add artefact as dependency in project with pomFile
  public void addProjectArtifact(IFile pomFile, Artifact a) {
    String artifactKey = getArtifactKey(a);
    String pomKey = getPomFileKey(pomFile);

    getSet(projectsToArtifacts, pomKey).add(artifactKey);
    getSet(artifactsToProjects, artifactKey).add(pomKey);
  }

  private Set getSet(Map map, String key) {
    Set s = (Set) map.get(key);
    if(s == null) {
      s = new HashSet();
      map.put(key, s);
    }
    return s;
  }

  /**
   * @return Set of IProject
   */
  public Set getDependentProjects(IFile pomFile) {
    Set projects = new HashSet();

    Model model = getMavenModel(pomFile);
    if(model != null) {
      String artifactKey = getArtifactKey(model);
      Set a = (Set) artifactsToProjects.get(artifactKey);
      if(a != null) {
        for(Iterator it = a.iterator(); it.hasNext();) {
          String pomKey = (String) it.next();
          Model m = (Model) models.get(pomKey);
          if(m != null) {
            IFile f = (IFile) this.artifacts.get(getArtifactKey(m));
            if(f != null) {
              projects.add(f.getProject());
            }
          }
        }
      }
    }

    return projects;
  }

  public Model updateMavenModel(IFile pomFile, boolean includeModules, IProgressMonitor monitor) throws CoreException {
    removeMavenModel(pomFile, includeModules, monitor);
    if(!pomFile.isAccessible()) {
      return null;
    }

    Model mavenModel = readMavenModel(pomFile.getLocation().toFile());
    if(mavenModel == null) {
      console.logMessage("Unable to read model for " + pomFile.getFullPath().toString());
      return null;
    }

    String pomKey = getPomFileKey(pomFile);
    models.put(pomKey, mavenModel);

    String artifactKey = getArtifactKey(mavenModel);
    artifacts.put(artifactKey, pomFile);
    console.logMessage("Updated model " + pomFile.getFullPath().toString() + " : " + artifactKey);

    if(includeModules) {
      IContainer parent = pomFile.getParent();
      for(Iterator it = mavenModel.getModules().iterator(); it.hasNext();) {
        if(monitor.isCanceled()) {
          throw new OperationCanceledException();
        }
        String module = (String) it.next();
        IResource memberPom = parent.findMember(module + "/" + Maven2Plugin.POM_FILE_NAME); //$NON-NLS-1$
        if(memberPom != null && memberPom.getType() == IResource.FILE) {
          updateMavenModel((IFile) memberPom, includeModules, monitor);
        }
      }
    }

    return mavenModel;
  }

  public Model removeMavenModel(IFile pomFile, boolean recursive, IProgressMonitor monitor) {
    String pomKey = getPomFileKey(pomFile);
    Model mavenModel = (Model) models.remove(pomKey);

    projectsToArtifacts.remove(pomKey);
//    if(artifacts!=null) {
//      for(Iterator it = artifacts.iterator(); it.hasNext();) {
//        String artifactKey = (String) it.next();
//        artifactsToProjects.remove(artifactKey);
//      }
//    }

    if(mavenModel != null) {
      String artifactKey = getArtifactKey(mavenModel);

      artifacts.remove(artifactKey);

      console.logMessage("Removed model " + pomFile.getFullPath().toString() + " : " + artifactKey);

      if(recursive) {
        IContainer parent = pomFile.getParent();
        for(Iterator it = mavenModel.getModules().iterator(); it.hasNext();) {
          if(monitor.isCanceled()) {
            throw new OperationCanceledException();
          }
          String module = (String) it.next();
          IResource memberPom = parent.findMember(module + "/" + Maven2Plugin.POM_FILE_NAME); //$NON-NLS-1$
          if(memberPom != null && memberPom.getType() == IResource.FILE) {
            removeMavenModel((IFile) memberPom, recursive, monitor);
          }
        }
      }
    }

    return mavenModel;
  }

  public Model readMavenModel(File pomFile) throws CoreException {
    try {
      MavenEmbedder projectEmbedder = embedderManager.getWorkspaceEmbedder();
      return projectEmbedder.readModel(pomFile);
    } catch(XmlPullParserException ex) {
      String msg = "Parsing error " + pomFile.getAbsolutePath() + "; " + ex.toString();
      console.logError(msg);
      throw new CoreException(new Status(IStatus.ERROR, Maven2Plugin.PLUGIN_ID, IStatus.ERROR, msg, ex));
    } catch(IOException ex) {
      String msg = "Can't read model " + pomFile.getAbsolutePath() + "; " + ex.toString();
      console.logError(msg);
      throw new CoreException(new Status(IStatus.ERROR, Maven2Plugin.PLUGIN_ID, IStatus.ERROR, msg, ex));
    }
  }

  public MavenExecutionResult readMavenProject(IFile pomFile, IProgressMonitor monitor, //
      boolean offline, boolean debug, ResolverConfiguration resolverConfiguration) {
    try {
      monitor.subTask("Reading " + pomFile.getFullPath());

      File file = pomFile.getLocation().toFile();

      MavenEmbedder mavenEmbedder = embedderManager.createEmbedder( //
          EmbedderFactory.createWorkspaceCustomizer(resolverConfiguration.shouldResolveWorkspaceProjects()));
      MavenExecutionRequest request = EmbedderFactory.createMavenExecutionRequest(mavenEmbedder, offline, debug);
      request.setPomFile(file.getAbsolutePath());
      request.setBaseDirectory(file.getParentFile());
      request.setTransferListener(new TransferListenerAdapter(monitor, console, indexManager));
      request.setProfiles(resolverConfiguration.getActiveProfileList());
      request.addActiveProfiles(resolverConfiguration.getActiveProfileList());

      return mavenEmbedder.readProjectWithDependencies(request);

      // XXX need to manage markers somehow see MNGECLIPSE-***
      // Util.deleteMarkers(pomFile);

//      if(!result.hasExceptions()) {
//        return result.getMavenProject();
//      }
//      
//      return result.getMavenProject();

//    } catch(Exception ex) {
//      Util.deleteMarkers(this.file);
//      Util.addMarker(this.file, "Unable to read project; " + ex.toString(), 1, IMarker.SEVERITY_ERROR);
//      
//      String msg = "Unable to read " + file.getLocation() + "; " + ex.toString();
//      console.logError(msg);
//      Maven2Plugin.log(msg, ex);

    } finally {
      monitor.done();
    }
  }

  public void addDependency(IFile pomFile, Dependency dependency) {
    addDependencies(pomFile, Collections.singletonList(dependency));
  }

  public void addDependencies(IFile pomFile, List dependencies) {
    File pom = pomFile.getLocation().toFile();
    try {
      MavenEmbedder mavenEmbedder = embedderManager.getWorkspaceEmbedder();
      Model model = mavenEmbedder.readModel(pom);
      model.getDependencies().addAll(dependencies);

      StringWriter w = new StringWriter();
//      mavenEmbedder.writeModel(w, model, true);
      MavenXpp3Writer writer = new MavenXpp3Writer();
      writer.write(w, model);

      pomFile.setContents(new ByteArrayInputStream(w.toString().getBytes("ASCII")), true, true, null);
      pomFile.refreshLocal(IResource.DEPTH_ONE, null); // TODO ???
    } catch(Exception ex) {
      console.logError("Unable to update POM: " + pom + "; " + ex.getMessage());
    }
  }

  public static String getArtifactKey(Model model) {
    String groupId = model.getGroupId();
    if(groupId == null) {
      // If the groupId is null in the model, then it needs to be inherited
      // from the parent.  And the parent's groupId has to be specified in the
      // in the parent element of the model.
      groupId = model.getParent().getGroupId();
    }

    String version = model.getVersion();
    if(version == null) {
      version = model.getParent().getVersion();
    }

    return groupId + ":" + model.getArtifactId() + ":" + version;
  }

  public static String getArtifactKey(Artifact a) {
    return a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getVersion();
  }

  public static String getPomFileKey(IFile pomFile) {
    return pomFile.getFullPath().toPortableString();
  }

}
