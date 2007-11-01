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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.DeltaProcessingState;
import org.eclipse.jdt.internal.core.JavaElementDelta;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerContentProvider;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.jface.preference.IPreferenceStore;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidArtifactRTException;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.extension.ExtensionScanningException;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Resource;
import org.apache.maven.project.InvalidProjectModelException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.validation.ModelValidationResult;

import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.Messages;
import org.maven.ide.eclipse.container.Maven2ClasspathContainer;
import org.maven.ide.eclipse.index.MavenRepositoryIndexManager;
import org.maven.ide.eclipse.launch.console.Maven2Console;
import org.maven.ide.eclipse.preferences.Maven2PreferenceConstants;
import org.maven.ide.eclipse.util.Util;


public class BuildPathManager {

  public static final String CLASSPATH_COMPONENT_DEPENDENCY = "org.eclipse.jst.component.dependency";

  public static final String CLASSPATH_COMPONENT_NON_DEPENDENCY = "org.eclipse.jst.component.nondependency";

  public static final String PACKAGING_WAR = "war";

  private final MavenEmbedderManager embedderManager;

  private final Maven2Console console;

  private final MavenModelManager mavenModelManager;

  private final MavenRepositoryIndexManager indexManager;

  private final IPreferenceStore preferenceStore;

  private final RefreshJob refreshJob;

  private String jdtVersion;

  public BuildPathManager(MavenEmbedderManager embedderManager, Maven2Console console,
      MavenModelManager mavenModelManager, MavenRepositoryIndexManager indexManager, IPreferenceStore preferenceStore) {
    this.embedderManager = embedderManager;
    this.console = console;
    this.mavenModelManager = mavenModelManager;
    this.indexManager = indexManager;
    this.preferenceStore = preferenceStore;
    this.refreshJob = new RefreshJob(this, console);
  }

  public static IClasspathEntry getDefaultContainerEntry() {
    return JavaCore.newContainerEntry(new Path(Maven2Plugin.CONTAINER_ID));
  }

  public static ResolverConfiguration getResolverConfiguration(IJavaProject javaProject) {
    return getResolverConfiguration(getMavenContainerEntry(javaProject));
  }

  public static ResolverConfiguration getResolverConfiguration(IClasspathEntry entry) {
    if(entry == null) {
      return new ResolverConfiguration();
    }

    boolean includeModules = entry == null ? false : //
        entry.getPath().toString().indexOf("/" + Maven2Plugin.INCLUDE_MODULES) > -1;

    boolean resolveWorkspaceProjects = entry == null ? true : //
        entry.getPath().toString().indexOf("/" + Maven2Plugin.NO_WORKSPACE_PROJECTS) == -1;

    return new ResolverConfiguration(includeModules, resolveWorkspaceProjects, getActiveProfiles(entry));
  }

  public static IClasspathEntry createContainerEntry(ResolverConfiguration configuration) {
    IPath newPath = new Path(Maven2Plugin.CONTAINER_ID);
    if(configuration.shouldIncludeModules()) {
      newPath = newPath.append(Maven2Plugin.INCLUDE_MODULES);
    }
    if(!configuration.shouldResolveWorkspaceProjects()) {
      newPath = newPath.append(Maven2Plugin.NO_WORKSPACE_PROJECTS);
    }
    if(configuration.getActiveProfiles().length() > 0) {
      newPath = newPath.append(Maven2Plugin.ACTIVE_PROFILES + "[" + configuration.getActiveProfiles().trim() + "]");
    }

    return JavaCore.newContainerEntry(newPath);
  }

  public static IClasspathEntry getMavenContainerEntry(IJavaProject javaProject) {
    IClasspathEntry[] classpath;
    try {
      classpath = javaProject.getRawClasspath();
    } catch(JavaModelException ex) {
      return null;
    }
    for(int i = 0; i < classpath.length; i++ ) {
      IClasspathEntry entry = classpath[i];
      if(isMaven2ClasspathContainer(entry.getPath())) {
        return entry;
      }
    }
    return null;
  }

  private static String getActiveProfiles(IClasspathEntry entry) {
    String path = entry.getPath().toString();
    String prefix = "/" + Maven2Plugin.ACTIVE_PROFILES + "[";
    int n = path.indexOf(prefix);
    if(n == -1) {
      return "";
    }

    return path.substring(n + prefix.length(), path.indexOf("]", n));
  }

  public void updateClasspathContainer(IProject project, IProgressMonitor monitor) throws CoreException {
    Map resolved = new HashMap();
    internalUpdateClasspathContainer(project, resolved, monitor);
    setClasspathContainer(resolved, monitor);
  }

  void setClasspathContainer(Map resolved, IProgressMonitor monitor) throws CoreException {
    monitor.subTask("Updating JDT");
    Iterator piter = resolved.entrySet().iterator();
    while(piter.hasNext()) {
      Map.Entry entry = (Entry) piter.next();
      IJavaProject javaProject = JavaCore.create((IProject) entry.getKey());
      IClasspathContainer container = (IClasspathContainer) entry.getValue();
      if(javaProject != null && container != null) {

        JavaCore.setClasspathContainer(container.getPath(), new IJavaProject[] {javaProject},
            new IClasspathContainer[] {container}, monitor);

        // XXX In Eclipse 3.3, changes to resolved classpath are not announced by JDT Core
        // and PackageExplorer does not properly refresh when we update Maven
        // classpath container.
        // As a temporary workaround, send F_CLASSPATH_CHANGED notifications
        // to all PackageExplorerContentProvider instances listening to
        // java ElementChangedEvent. 
        // Note that even with this hack, build clean is sometimes necessary to
        // reconcile PackageExplorer with actual classpath
        // See https://bugs.eclipse.org/bugs/show_bug.cgi?id=154071
        if(getJDTVersion().startsWith("3.3")) {
          DeltaProcessingState state = JavaModelManager.getJavaModelManager().deltaState;
          synchronized(state) {
            IElementChangedListener[] listeners = state.elementChangedListeners;
            for(int i = 0; i < listeners.length; i++ ) {
              if(listeners[i] instanceof PackageExplorerContentProvider) {
                JavaElementDelta delta = new JavaElementDelta(javaProject);
                delta.changed(IJavaElementDelta.F_CLASSPATH_CHANGED);
                listeners[i].elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));
              }
            }
          }
        }
      }
    }
  }

  private synchronized String getJDTVersion() {
    if(jdtVersion == null) {
      Bundle[] bundles = Maven2Plugin.getDefault().getBundle().getBundleContext().getBundles();
      for(int i = 0; i < bundles.length; i++ ) {
        if(JavaCore.PLUGIN_ID.equals(bundles[i].getSymbolicName())) {
          jdtVersion = (String) bundles[i].getHeaders().get(Constants.BUNDLE_VERSION);
          break;
        }
      }
    }
    return jdtVersion;
  }

  void internalUpdateClasspathContainer(IProject project, Map resolved, IProgressMonitor monitor)
      throws JavaModelException, CoreException {

    if(monitor.isCanceled()) {
      throw new OperationCanceledException();
    }
    monitor.subTask(project.getName());

    IJavaProject javaProject = JavaCore.create(project);
    ResolverConfiguration resolverConfiguration = getResolverConfiguration(javaProject);

    IFile pomFile = project.getFile(Maven2Plugin.POM_FILE_NAME);

    Set entries = new HashSet();
    Map moduleArtifacts = new HashMap();

    deleteMarkers(project);
    try {
      mavenModelManager.updateMavenModel(pomFile, resolverConfiguration.shouldIncludeModules(), monitor);
    } catch(CoreException ex) {
      addMarker(pomFile, ex.getMessage(), 1, IMarker.SEVERITY_ERROR);
    }

    MavenEmbedder embedder = embedderManager.createEmbedder( //
        EmbedderFactory.createWorkspaceCustomizer(resolverConfiguration.shouldResolveWorkspaceProjects()));
    if(embedder != null) {
      try {
        resolveClasspathEntries(entries, moduleArtifacts, pomFile, pomFile, resolverConfiguration, monitor, embedder);
      } finally {
        try {
          embedder.stop();
        } catch(MavenEmbedderException ex) {
          String msg = "Unable to stop project embedder; " + ex.getMessage();
          console.logError(msg);
          Maven2Plugin.log(CLASSPATH_COMPONENT_DEPENDENCY, ex);
        }
      }
    }

    IClasspathEntry containerEntry = getMavenContainerEntry(javaProject);
    if(containerEntry != null) {
      Maven2ClasspathContainer container = new Maven2ClasspathContainer(containerEntry.getPath(), entries);
      resolved.put(project, container);
    } else {
      resolved.put(project, null); // TODO test me
    }

    Set dependentProjects = mavenModelManager.getDependentProjects(pomFile);
    for(Iterator it = dependentProjects.iterator(); it.hasNext();) {
      IProject p = (IProject) it.next();
      if(!resolved.containsKey(p)) {
        internalUpdateClasspathContainer(p, resolved, monitor);
      }
    }
  }

  private void resolveClasspathEntries(Set entries, Map moduleArtifacts, IFile rootPomFile, IFile pomFile,
      ResolverConfiguration resolverConfiguration, IProgressMonitor monitor, MavenEmbedder embedder) {
    if(monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    if(pomFile == null || !pomFile.isAccessible()) {
      return;
    }

    console.logMessage("Reading " + pomFile.getFullPath());
    monitor.subTask("Reading " + pomFile.getFullPath());
    deleteMarkers(pomFile);

    IProject currentProject = pomFile.getProject();
    try {
      boolean offline = preferenceStore.getBoolean(Maven2PreferenceConstants.P_OFFLINE);
      boolean downloadSources = !offline & preferenceStore.getBoolean(Maven2PreferenceConstants.P_DOWNLOAD_SOURCES);
      boolean downloadJavadoc = !offline & preferenceStore.getBoolean(Maven2PreferenceConstants.P_DOWNLOAD_JAVADOC);
      boolean debug = preferenceStore.getBoolean(Maven2PreferenceConstants.P_DEBUG_OUTPUT);

      MavenExecutionResult result = mavenModelManager.readMavenProject(pomFile.getLocation().toFile(), monitor,
          offline, debug, resolverConfiguration, embedder);

      MavenProject mavenProject = getMavenProject(pomFile, result);
      if(mavenProject == null) {
        return;
      }

      // deleteMarkers(pomFile);
      // TODO use version?
      moduleArtifacts.put(mavenProject.getGroupId() + ":" + mavenProject.getArtifactId() + ":"
          + mavenProject.getVersion(), mavenProject.getArtifact());

      // From MNGECLIPSE-105
      // If the current project is a WAR project AND it has
      // a dynamic web project nature, make sure that any workspace
      // projects that it depends on are NOT included in any way
      // in the container (neither as projects nor as artifacts).
      // The idea is that the inclusion is controlled explicitly
      // by a developer via WTP UI.
      boolean skipWorkspaceProjectsForWeb = PACKAGING_WAR.equals(mavenProject.getPackaging())
          && hasDynamicWebProjectNature(currentProject);

      // Set artifacts = mavenProject.getArtifacts();
      // TODO merge all artifacts into a single list; may need to refine this
      List artifacts = new ArrayList();
      artifacts.addAll(mavenProject.getCompileArtifacts());
      artifacts.addAll(mavenProject.getTestArtifacts());
      artifacts.addAll(mavenProject.getRuntimeArtifacts());
      artifacts.addAll(mavenProject.getSystemArtifacts());
      // artifacts.addAll(mavenProject.getAttachedArtifacts());
      // artifacts.addAll(mavenProject.getDependencyArtifacts());

      for(Iterator it = artifacts.iterator(); it.hasNext();) {
        if(monitor.isCanceled()) {
          throw new OperationCanceledException();
        }

        Artifact a = (Artifact) it.next();

        monitor.subTask("Processing " + a.getId());

        // String artifactKey = a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getVersion() + ":" + a.getType();
        String artifactKey = a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getVersion() + ":" + a.getClassifier();
        if(moduleArtifacts.containsKey(artifactKey)) {
          continue;
        }

//        ArtifactHandler artifactHandler = embedder.getArtifactHandler(a);
//        if(!artifactHandler.isAddedToClasspath()
//            || !("jar".equals(artifactHandler.getExtension()) || "zip".equals(artifactHandler.getExtension()))) {
//          continue;
//        }

        moduleArtifacts.put(artifactKey, a);

        ArrayList attributes = new ArrayList();

        mavenModelManager.addProjectArtifact(pomFile, a);
        // this is needed to projects with have modules (either inner or external)
        mavenModelManager.addProjectArtifact(rootPomFile, a);

        String scope = a.getScope();
        // Check the scope & set WTP non-dependency as appropriate
        if(Artifact.SCOPE_PROVIDED.equals(scope) || Artifact.SCOPE_TEST.equals(scope)
            || Artifact.SCOPE_SYSTEM.equals(scope)) {
          attributes.add(JavaCore.newClasspathAttribute(CLASSPATH_COMPONENT_NON_DEPENDENCY, ""));
        }

        IFile artifactPomFile = mavenModelManager.getArtifactFile(a);
        if(artifactPomFile != null) {
          IProject artifactProject = artifactPomFile.getProject();
          if(artifactProject.getFullPath().equals(currentProject.getFullPath())) {
            // This is another artifact in our current project so we should not
            // add our own project to ourself
            continue;
          }

          if(skipWorkspaceProjectsForWeb) {
            // From MNGECLIPSE-105
            // Leave it out so that the user can handle it the WTP way
            continue;
          }
        }

        if(resolverConfiguration.shouldResolveWorkspaceProjects()) {
          mavenModelManager.addProjectArtifact(pomFile, a);
          // this is needed to projects with have modules (either inner or external)
          mavenModelManager.addProjectArtifact(rootPomFile, a);

          if(artifactPomFile != null) {
            IProject artifactProject = artifactPomFile.getProject();
            entries.add(JavaCore.newProjectEntry(artifactProject.getFullPath(), false));
            continue;
          }
        }

        File artifactFile = a.getFile();
        if(artifactFile == null) {
          // Embedder returns unresolved artifacts when dependencies can't be downloaded 
          try {
            embedder.resolve(a, mavenProject.getRemoteArtifactRepositories(), embedder.getLocalRepository());
            artifactFile = a.getFile();
          } catch(AbstractArtifactResolutionException ex) {
            String name = ex.getGroupId() + ":" + ex.getArtifactId() + "-" + ex.getVersion() + "-" + ex.getType();
            console.logError("Unable resolve artifact " + name);
          }
        }

        if(artifactFile != null) {
          String artifactLocation = artifactFile.getAbsolutePath();

          Path srcPath = materializeArtifactPath(embedder, mavenProject, a, "java-source", "sources", downloadSources,
              monitor);

          attributes.add(JavaCore.newClasspathAttribute(Maven2Plugin.GROUP_ID_ATTRIBUTE, a.getGroupId()));
          attributes.add(JavaCore.newClasspathAttribute(Maven2Plugin.ARTIFACT_ID_ATTRIBUTE, a.getArtifactId()));
          attributes.add(JavaCore.newClasspathAttribute(Maven2Plugin.VERSION_ATTRIBUTE, a.getVersion()));

          if(srcPath == null) { // no need to search for javadoc if we have source code
            Path javadocPath = materializeArtifactPath(embedder, mavenProject, a, "javadoc", "javadoc",
                downloadJavadoc, monitor);
            String javaDocUrl = null;
            if(javadocPath != null) {
              javaDocUrl = Maven2ClasspathContainer.getJavaDocUrl(javadocPath.toString());
            } else {
              javaDocUrl = getJavaDocUrl(artifactLocation, monitor);
            }
            if(javaDocUrl != null) {
              attributes.add(JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
                  javaDocUrl));
            }
          }

          entries.add(JavaCore.newLibraryEntry(new Path(artifactLocation), //
              srcPath, null, new IAccessRule[0], //
              (IClasspathAttribute[]) attributes.toArray(new IClasspathAttribute[attributes.size()]), // 
              false /*not exported*/));
        }
      }

      if(resolverConfiguration.shouldIncludeModules()) {
        IContainer basedir = pomFile.getParent();

        List modules = mavenProject.getModules();
        for(Iterator it = modules.iterator(); it.hasNext() && !monitor.isCanceled();) {
          if(monitor.isCanceled()) {
            throw new OperationCanceledException();
          }

          String module = (String) it.next();
          IResource memberPom = basedir.findMember(module + "/" + Maven2Plugin.POM_FILE_NAME); //$NON-NLS-1$
          if(memberPom != null && memberPom.getType() == IResource.FILE) {
            resolveClasspathEntries(entries, moduleArtifacts, // 
                rootPomFile, (IFile) memberPom, resolverConfiguration, monitor, embedder);
          }
        }
      }

    } catch(OperationCanceledException ex) {
      throw ex;

    } catch(InvalidArtifactRTException ex) {
      addMarker(pomFile, ex.getBaseMessage(), 1, IMarker.SEVERITY_ERROR);
      console.logError("Unable to read " + getPomName(pomFile) + "; " + ex.getBaseMessage());

    } catch(IllegalStateException ex) {
      addMarker(pomFile, ex.getMessage(), 1, IMarker.SEVERITY_ERROR);
      console.logError("Unable to read " + getPomName(pomFile) + "; " + ex.getMessage());

    } catch(Throwable ex) {
      addMarker(pomFile, ex.toString(), 1, IMarker.SEVERITY_ERROR);

      String msg = "Unable to read " + getPomName(pomFile) + "; " + ex.toString();
      console.logError(msg);
      Maven2Plugin.log(msg, ex);

    } finally {
      monitor.done();

    }
  }

  private MavenProject getMavenProject(IFile pomFile, MavenExecutionResult result) {
    addErrorMarkers(pomFile, result);

    if(!result.hasExceptions()) {
      return result.getProject();
    }

    for(Iterator it = result.getExceptions().iterator(); it.hasNext();) {
      Exception ex = (Exception) it.next();
      if(ex instanceof ExtensionScanningException) {
        if(ex.getCause() instanceof ProjectBuildingException) {
          handleProjectBuildingException(pomFile, (ProjectBuildingException) ex.getCause());
        } else {
          handleBuildException(pomFile, ex);
        }

      } else if(ex instanceof ProjectBuildingException) {
        handleProjectBuildingException(pomFile, (ProjectBuildingException) ex);

      } else if(ex instanceof AbstractArtifactResolutionException) {
        // String msg = ex.getMessage().replaceAll("----------", "").replaceAll("\r\n\r\n", "\n").replaceAll("\n\n", "\n");
        // addMarker(pomFile, msg, 1, IMarker.SEVERITY_ERROR);
        // console.logError(msg);

        AbstractArtifactResolutionException rex = (AbstractArtifactResolutionException) ex;
        String errorMessage = getArtifactId(rex) + " " + getErrorMessage(ex);
        addMarker(pomFile, errorMessage, 1, IMarker.SEVERITY_ERROR);
        console.logError(errorMessage);

        try {
          // TODO
          File file = pomFile.getLocation().toFile();
          return embedderManager.getWorkspaceEmbedder().readProject(file);

        } catch(ProjectBuildingException ex2) {
          handleProjectBuildingException(pomFile, ex2);
        } catch(Exception ex2) {
          handleBuildException(pomFile, ex2);
        }

      } else {
        handleBuildException(pomFile, ex);
      }
    }
    return null;
  }

  private void addErrorMarkers(IFile pomFile, MavenExecutionResult result) {
    ArtifactResolutionResult resolutionResult = result.getArtifactResolutionResult();
    if(resolutionResult != null) {
      // List missingArtifacts = resolutionResult.getMissingArtifacts();
      addErrorMarkers(pomFile, "Metadata resolution error", resolutionResult.getMetadataResolutionExceptions());
      addErrorMarkers(pomFile, "Artifact error", resolutionResult.getErrorArtifactExceptions());
      addErrorMarkers(pomFile, "Version range violation", resolutionResult.getVersionRangeViolations());
      addErrorMarkers(pomFile, "Curcular dependency error", resolutionResult.getCircularDependencyExceptions());
    }
  }

  private void addErrorMarkers(IFile pomFile, String msg, List exceptions) {
    if(exceptions != null) {
      for(Iterator it = exceptions.iterator(); it.hasNext();) {
        Exception ex = (Exception) it.next();
        if(ex instanceof AbstractArtifactResolutionException) {
          AbstractArtifactResolutionException rex = (AbstractArtifactResolutionException) ex;
          String errorMessage = getArtifactId(rex) + " " + getErrorMessage(ex);
          addMarker(pomFile, errorMessage, 1, IMarker.SEVERITY_ERROR);
          console.logError(errorMessage);

        } else {
          addMarker(pomFile, ex.getMessage(), 1, IMarker.SEVERITY_ERROR);
          console.logError(msg + "; " + ex.toString());
        }
      }
    }
  }

  private String getArtifactId(AbstractArtifactResolutionException rex) {
    String id = rex.getGroupId() + ":" + rex.getArtifactId() + ":" + rex.getVersion();
    if(rex.getClassifier() != null) {
      id += ":" + rex.getClassifier();
    }
    if(rex.getType() != null) {
      id += ":" + rex.getType();
    }
    return id;
  }

  private String getErrorMessage(Exception ex) {
    Throwable lastCause = ex;
    Throwable cause = lastCause.getCause();

    String msg = lastCause.getMessage();
    while(cause != null && cause != lastCause) {
      msg = cause.getMessage();
//      if(lastCause instanceof ResourceDoesNotExistException) {
//        msg = ((ResourceDoesNotExistException) lastCause).getLocalizedMessage();
//      } else {
//      }
      lastCause = cause;
      cause = cause.getCause();
    }

    return msg;
  }

  private void handleBuildException(IFile pomFile, Exception ex) {
    String msg = Messages.getString("plugin.markerBuildError") + ex.getMessage();
    addMarker(pomFile, msg, 1, IMarker.SEVERITY_ERROR); //$NON-NLS-1$
    console.logError(msg);
  }

  private void handleProjectBuildingException(IFile pomFile, ProjectBuildingException ex) {
    Throwable cause = ex.getCause();
    if(cause instanceof XmlPullParserException) {
      XmlPullParserException pex = (XmlPullParserException) cause;
      console.logError(Messages.getString("plugin.markerParsingError") + getPomName(pomFile) + "; " + pex.getMessage());
      addMarker(pomFile, pex.getMessage(), pex.getLineNumber(), IMarker.SEVERITY_ERROR); //$NON-NLS-1$

    } else if(ex instanceof InvalidProjectModelException) {
      InvalidProjectModelException mex = (InvalidProjectModelException) ex;
      ModelValidationResult validationResult = mex.getValidationResult();
      String msg = Messages.getString("plugin.markerBuildError") + mex.getMessage();
      console.logError(msg);
      if(validationResult == null) {
        addMarker(pomFile, msg, 1, IMarker.SEVERITY_ERROR); //$NON-NLS-1$
      } else {
        for(Iterator it = validationResult.getMessages().iterator(); it.hasNext();) {
          String message = (String) it.next();
          addMarker(pomFile, message, 1, IMarker.SEVERITY_ERROR); //$NON-NLS-1$
          console.logError("  " + message);
        }
      }

    } else {
      handleBuildException(pomFile, ex);
    }
  }

  private String getPomName(IFile pomFile) {
    return pomFile.getProject().getName() + "/" + pomFile.getProjectRelativePath();
  }

  // type = "java-source"
  private Path materializeArtifactPath(MavenEmbedder embedder, MavenProject mavenProject, Artifact a, String type,
      String suffix, boolean download, IProgressMonitor monitor) throws Exception {
    File artifactFile = a.getFile();
    if(artifactFile == null) {
      console.logError("Missing artifact file for " + a.getId());
      return null;
    }

    String artifactLocation = artifactFile.getAbsolutePath();

    // XXX MNGECLIPSE-205
    File file;
    if("java-source".equals(type) && "tests".equals(a.getArtifactHandler().getClassifier())) {
      suffix = "test-sources";
      file = new File(artifactLocation.substring(0, artifactLocation.length() - "-tests.jar".length()) + "-" + suffix
          + ".jar");
    } else {
      // artifactLocation ends on '.jar' or '.zip'
      file = new File(artifactLocation.substring(0, artifactLocation.length() - ".jar".length()) + "-" + suffix
          + ".jar");
    }

    if(file.exists()) {
      // workaround to not download already existing archive
      return new Path(file.getAbsolutePath());
    }

    if(download) {
      monitor.beginTask("Resolving " + type + " " + a.getId(), IProgressMonitor.UNKNOWN);
      try {
        // TODO need to take into account issue with the "test-sources"
        Artifact f = embedder.createArtifactWithClassifier(a.getGroupId(), a.getArtifactId(), a.getVersion(), type,
            suffix);
        if(f != null) {
          embedder.resolve(f, mavenProject.getRemoteArtifactRepositories(), embedder.getLocalRepository());
          return new Path(f.getFile().getAbsolutePath());
        }
      } catch(AbstractArtifactResolutionException ex) {
        String name = ex.getGroupId() + ":" + ex.getArtifactId() + "-" + ex.getVersion() + "." + ex.getType();
        console.logError("Unable to download " + type + " for artifact " + name);
        if(!"java-source".equals(type) && !"javadoc".equals(type)) {
          console.logError("Error: " + ex.getOriginalMessage());
        }
      } finally {
        monitor.done();
      }
    }
    return null;
  }

  private String getJavaDocUrl(String artifactLocation, IProgressMonitor monitor) throws CoreException {
    // guess the javadoc url from the project url in the artifact's pom.xml
    File file = new File(artifactLocation.substring(0, artifactLocation.length() - 4) + ".pom");
    if(file.exists()) {
      Model model = mavenModelManager.readMavenModel(file);
      String url = model.getUrl();
      if(url != null) {
        url = url.trim();
        if(url.length() > 0) {
          if(!url.endsWith("/"))
            url += "/";
          return url + "apidocs/"; // assuming project is using maven-generated site
        }
      }
    }
    return null;
  }

  public void updateSourceFolders(IProject project, ResolverConfiguration configuration, IProgressMonitor monitor) {
    IFile pom = project.getFile(Maven2Plugin.POM_FILE_NAME);
    if(!pom.exists()) {
      return;
    }

    monitor.beginTask("Updating sources " + project.getName(), IProgressMonitor.UNKNOWN);
    long t1 = System.currentTimeMillis();
    try {
      Set sources = new HashSet();
      List entries = new ArrayList();

      MavenProject mavenProject = collectSourceEntries(project, entries, sources, configuration, monitor);

      monitor.subTask("Configuring Build Path");
      IJavaProject javaProject = JavaCore.create(project);

      if(mavenProject != null) {
        Map options = collectOptions(mavenProject);
        setOption(javaProject, options, JavaCore.COMPILER_COMPLIANCE);
        setOption(javaProject, options, JavaCore.COMPILER_SOURCE);
        setOption(javaProject, options, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM);

        String source = (String) options.get(JavaCore.COMPILER_SOURCE);
        if(source == null) {
          entries.add(JavaRuntime.getDefaultJREContainerEntry());
        } else {
          entries.add(getJREContainer(source));
        }
      }

      IClasspathEntry[] currentClasspath = javaProject.getRawClasspath();
      for(int i = 0; i < currentClasspath.length; i++ ) {
        // Delete all non container (e.g. JRE library) entries. See MNGECLIPSE-9 
        IClasspathEntry entry = currentClasspath[i];
        if(entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
          if(!JavaRuntime.JRE_CONTAINER.equals(entry.getPath().segment(0))) {
            entries.add(entry);
          }
        }
      }

      if(mavenProject != null) {
        String outputDirectory = toRelativeAndFixSeparator(project.getLocation().toFile(), //
            mavenProject.getBuild().getOutputDirectory());
        IFolder outputFolder = project.getFolder(outputDirectory);
        Util.createFolder(outputFolder);
        javaProject.setRawClasspath((IClasspathEntry[]) entries.toArray(new IClasspathEntry[entries.size()]),
            outputFolder.getFullPath(), monitor);
      } else {
        javaProject.setRawClasspath((IClasspathEntry[]) entries.toArray(new IClasspathEntry[entries.size()]), monitor);
      }

      long t2 = System.currentTimeMillis();
      console.logMessage("Updated source folders for project " + project.getName() + " " + (t2 - t1) / 1000 + "sec");

    } catch(Exception ex) {
      String msg = "Unable to update source folders " + project.getName() + "; " + ex.toString();
      console.logMessage(msg);
      Maven2Plugin.log(msg, ex);

    } finally {
      monitor.done();
    }
  }

  private void setOption(IJavaProject javaProject, Map options, String name) {
    String newValue = (String) options.get(name);
    if(newValue == null) {
      newValue = (String) JavaCore.getDefaultOptions().get(name);
    }

    String currentValue = javaProject.getOption(name, false);
    if(!newValue.equals(currentValue)) {
      javaProject.setOption(name, newValue);
    }
  }

  private IClasspathEntry getJREContainer(String version) {
    int n = VERSIONS.indexOf(version);
    if(n > -1) {
      Map jreContainers = getJREContainers();
      for(int i = n; i < VERSIONS.size(); i++ ) {
        IClasspathEntry entry = (IClasspathEntry) jreContainers.get(version);
        if(entry != null) {
          console.logMessage("JRE compliant to " + version + ". " + entry);
          return entry;
        }
      }
    }
    IClasspathEntry entry = JavaRuntime.getDefaultJREContainerEntry();
    console.logMessage("No JRE compliant to " + version + ". Using default JRE container " + entry);
    return entry;
  }

  private Map getJREContainers() {
    Map jreContainers = new HashMap();

    jreContainers.put(getJREVersion(JavaRuntime.getDefaultVMInstall()), JavaRuntime.getDefaultJREContainerEntry());

    IVMInstallType[] installTypes = JavaRuntime.getVMInstallTypes();
    for(int i = 0; i < installTypes.length; i++ ) {
      IVMInstall[] installs = installTypes[i].getVMInstalls();
      for(int j = 0; j < installs.length; j++ ) {
        IVMInstall install = installs[j];
        String version = getJREVersion(install);
        if(!jreContainers.containsKey(version)) {
          // in Eclipse 3.2 one could use JavaRuntime.newJREContainerPath(install)
          IPath jreContainerPath = new Path(JavaRuntime.JRE_CONTAINER).append(install.getVMInstallType().getId())
              .append(install.getName());
          jreContainers.put(version, JavaCore.newContainerEntry(jreContainerPath));
        }
      }
    }

    return jreContainers;
  }

  private String getJREVersion(IVMInstall install) {
    LibraryLocation[] libraryLocations = JavaRuntime.getLibraryLocations(install);
    if(libraryLocations != null) {
      for(int k = 0; k < libraryLocations.length; k++ ) {
        IPath path = libraryLocations[k].getSystemLibraryPath();
        String jarName = path.lastSegment();
        // TODO that won't be the case on Mac
        if("rt.jar".equals(jarName)) {
          try {
            JarFile jarFile = new JarFile(path.toFile());
            Manifest manifest = jarFile.getManifest();
            Attributes attributes = manifest.getMainAttributes();
            return attributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
          } catch(Exception ex) {
            console.logError("Unable to read " + path + " " + ex.getMessage());
          }
        }
      }
    }
    return null;
  }

  private MavenProject collectSourceEntries(IProject project, List sourceEntries, Set sources,
      ResolverConfiguration configuration, IProgressMonitor monitor) {
    if(monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    Maven2Plugin plugin = Maven2Plugin.getDefault();
    IPreferenceStore preferenceStore = plugin.getPreferenceStore();
    boolean offline = preferenceStore.getBoolean(Maven2PreferenceConstants.P_OFFLINE);
    boolean debug = preferenceStore.getBoolean(Maven2PreferenceConstants.P_DEBUG_OUTPUT);
    String globalSettings = preferenceStore.getString(Maven2PreferenceConstants.P_GLOBAL_SETTINGS_FILE);

    MavenEmbedder mavenEmbedder;
    try {
      // XXX should use project embedder with resolving from workspace?
      mavenEmbedder = EmbedderFactory.createMavenEmbedder(EmbedderFactory.createExecutionCustomizer(),
          new PluginConsoleMavenEmbeddedLogger(console, debug), globalSettings);
    } catch(MavenEmbedderException ex) {
      console.logError("Unable to create embedder; " + ex.toString());
      return null;
    }

    IFile pomResource = project.getFile(Maven2Plugin.POM_FILE_NAME);

    console.logMessage("Reading " + pomResource.getFullPath());

    monitor.subTask("Reading " + pomResource.getFullPath());
    File pomFile = pomResource.getLocation().toFile();

    MavenProject mavenProject;
//    try {
//      mavenProject = mavenEmbedder.readProject(pomFile);
//    } catch(Exception ex) {
//      console.logError("Unable to read project " + pomResource.getFullPath() + "; " + ex.toString());
//      return null;
//    }

    File basedir = pomResource.getLocation().toFile().getParentFile();
    File projectBaseDir = project.getLocation().toFile();

    monitor.subTask("Generating Sources " + pomResource.getFullPath());
    try {
      console.logMessage("Generating sources " + pomResource.getFullPath());

      MavenExecutionRequest request = EmbedderFactory.createMavenExecutionRequest(mavenEmbedder, offline, debug);

      request.setUseReactor(false);
      request.setRecursive(configuration.shouldIncludeModules());

      request.setBaseDirectory(pomFile.getParentFile());
      request.setGoals(Collections.singletonList("process-test-resources"));
      request.addEventMonitor(new PluginConsoleEventMonitor(console));
      request.setTransferListener(new TransferListenerAdapter(monitor, console, indexManager));
      // request.setPomFile(pomFile.getAbsolutePath());
      // request.setGoals(Arrays.asList("generate-sources,generate-resources,generate-test-sources,generate-test-resources".split(",")));
      // request.setProfiles(...);
      // request.setReactorFailureBehavior(MavenExecutionRequest.REACTOR_FAIL_AT_END);

      MavenExecutionResult result = mavenEmbedder.execute(request);

      // TODO optimize project refresh
      monitor.subTask("Refreshing");
      project.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1));

      mavenProject = result.getProject();

      ReactorManager reactorManager = result.getReactorManager();
      if(reactorManager != null && reactorManager.getSortedProjects() != null) {
        if(configuration.shouldIncludeModules()) {
          for(Iterator it = reactorManager.getSortedProjects().iterator(); it.hasNext();) {
            addDirs(project, sources, sourceEntries, (MavenProject) it.next(), basedir, projectBaseDir);
          }
        } else {
          addDirs(project, sources, sourceEntries, //
              (MavenProject) reactorManager.getSortedProjects().iterator().next(), //
              basedir, projectBaseDir);
        }
      }

      if(result.hasExceptions()) {
        for(Iterator it = result.getExceptions().iterator(); it.hasNext();) {
          Exception ex = (Exception) it.next();
          console.logError("Build error for " + pomResource.getFullPath() + "; " + ex.toString());
        }
      }

      if(mavenProject == null) {
        try {
          mavenProject = mavenEmbedder.readProject(pomFile);
        } catch(Exception ex2) {
          console.logError("Unable to read project " + pomResource.getFullPath() + "; " + ex2.toString());
          return null;
        }
      }

    } catch(Exception ex) {
      String msg = "Build error for " + pomResource.getFullPath();
      console.logError(msg + "; " + ex.toString());
      Maven2Plugin.log(msg, ex);

      try {
        mavenProject = mavenEmbedder.readProject(pomFile);
      } catch(Exception ex2) {
        console.logError("Unable to read project " + pomResource.getFullPath() + "; " + ex.toString());
        return null;
      }

      addDirs(project, sources, sourceEntries, mavenProject, basedir, projectBaseDir);
    }

    return mavenProject;
  }

  private Map collectOptions(MavenProject mavenProject) {
    Map options = new HashMap();

    String source = getBuildOption(mavenProject, "maven-compiler-plugin", "source");
    if(source != null) {
      if(VERSIONS.contains(source)) {
        console.logMessage("Setting source compatibility: " + source);
        setVersion(options, JavaCore.COMPILER_SOURCE, source);
        setVersion(options, JavaCore.COMPILER_COMPLIANCE, source);
      } else {
        console.logError("Invalid compiler source " + source + ". Using default");
      }
    }

    String target = getBuildOption(mavenProject, "maven-compiler-plugin", "target");
    if(target != null) {
      if(VERSIONS.contains(target)) {
        console.logMessage("Setting target compatibility: " + source);
        setVersion(options, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, target);
      } else {
        console.logError("Invalid compiler target " + target + ". Using default");
      }
    }

    return options;
  }

  private void addDirs(IContainer project, Set sources, List sourceEntries, MavenProject mavenProject, File basedir,
      File projectBaseDir) {
    addSourceDirs(project, sources, sourceEntries, mavenProject.getCompileSourceRoots(), basedir, projectBaseDir);
    addSourceDirs(project, sources, sourceEntries, mavenProject.getTestCompileSourceRoots(), basedir, projectBaseDir);

    addResourceDirs(project, sources, sourceEntries, mavenProject.getBuild().getResources(), basedir, projectBaseDir);
    addResourceDirs(project, sources, sourceEntries, mavenProject.getBuild().getTestResources(), basedir,
        projectBaseDir);

    // HACK to support xmlbeans generated classes MNGECLIPSE-374
    File generatedClassesDir = new File(mavenProject.getBuild().getDirectory(), "generated-classes" + File.separator
        + "xmlbeans");
    IResource generatedClasses = project.findMember(toRelativeAndFixSeparator(projectBaseDir, //
        generatedClassesDir.getAbsolutePath()));
    if(generatedClasses != null && generatedClasses.isAccessible() && generatedClasses.getType() == IResource.FOLDER) {
      sourceEntries.add(JavaCore.newLibraryEntry(generatedClasses.getFullPath(), null, null));
    }
  }

  private void addSourceDirs(IContainer project, Set sources, List sourceEntries, List sourceRoots, File basedir,
      File projectBaseDir) {
    for(Iterator it = sourceRoots.iterator(); it.hasNext();) {
      String sourceRoot = (String) it.next();
      if(new File(sourceRoot).isDirectory()) {
        IResource r = project.findMember(toRelativeAndFixSeparator(projectBaseDir, sourceRoot));
        if(r != null && sources.add(r.getFullPath().toString())) {
          sourceEntries.add(JavaCore
              .newSourceEntry(r.getFullPath() /*, new IPath[] { new Path( "**"+"/.svn/"+"**")} */));
          console.logMessage("Adding source folder " + r.getFullPath());
        }
      }
    }
  }

  private void addResourceDirs(IContainer project, Set sources, List sourceEntries, List resources, File basedir,
      File projectBaseDir) {
    for(Iterator it = resources.iterator(); it.hasNext();) {
      Resource resource = (Resource) it.next();
      File resourceDirectory = new File(resource.getDirectory());
      if(resourceDirectory.exists() && resourceDirectory.isDirectory()) {
        IResource r = project.findMember(toRelativeAndFixSeparator(projectBaseDir, resource.getDirectory()));
        if(r != null && sources.add(r.getFullPath().toString())) {
          sourceEntries.add(JavaCore.newSourceEntry(r.getFullPath(), new IPath[] {new Path("**")}, r.getFullPath())); //, new IPath[] { new Path( "**"+"/.svn/"+"**")} ) );
          console.logMessage("Adding resource folder " + r.getFullPath());
        }
      }
    }
  }

  private String toRelativeAndFixSeparator(File basedir, String absolutePath) {
    String relative;
    if(absolutePath.equals(basedir.getAbsolutePath())) {
      relative = ".";
    } else if(absolutePath.startsWith(basedir.getAbsolutePath())) {
      relative = absolutePath.substring(basedir.getAbsolutePath().length() + 1);
    } else {
      relative = absolutePath;
    }
    return relative.replace('\\', '/'); //$NON-NLS-1$ //$NON-NLS-2$
  }

  public void enableMavenNature(IProject project, ResolverConfiguration configuration, IProgressMonitor monitor)
      throws CoreException, JavaModelException {
    monitor.subTask("Enable Maven nature");

    ArrayList newNatures = new ArrayList();
    newNatures.add(JavaCore.NATURE_ID);
    newNatures.add(Maven2Plugin.NATURE_ID);

    IProjectDescription description = project.getDescription();
    String[] natures = description.getNatureIds();
    for(int i = 0; i < natures.length; ++i) {
      String id = natures[i];
      if(!Maven2Plugin.NATURE_ID.equals(id) && !JavaCore.NATURE_ID.equals(natures[i])) {
        newNatures.add(natures[i]);
      }
    }
    description.setNatureIds((String[]) newNatures.toArray(new String[newNatures.size()]));
    project.setDescription(description, monitor);

    IJavaProject javaProject = JavaCore.create(project);
    if(javaProject != null) {
      HashSet containerEntrySet = new HashSet();
      IClasspathContainer container = getMaven2ClasspathContainer(javaProject);
      if(container != null) {
        IClasspathEntry[] entries = container.getClasspathEntries();
        for(int i = 0; i < entries.length; i++ ) {
          containerEntrySet.add(entries[i].getPath().toString());
        }
      }

      // remove classpath container from JavaProject
      IClasspathEntry[] entries = javaProject.getRawClasspath();
      ArrayList newEntries = new ArrayList();
      for(int i = 0; i < entries.length; i++ ) {
        IClasspathEntry entry = entries[i];
        if(!isMaven2ClasspathContainer(entry.getPath()) && !containerEntrySet.contains(entry.getPath().toString())) {
          newEntries.add(entry);
        }
      }

      newEntries.add(createContainerEntry(configuration));

      javaProject.setRawClasspath((IClasspathEntry[]) newEntries.toArray(new IClasspathEntry[newEntries.size()]),
          monitor);
    }
  }

  public void disableMavenNature(IProject project, IProgressMonitor monitor) {
    monitor.subTask("Disable Maven nature");

    try {
      project.deleteMarkers(Maven2Plugin.MARKER_ID, true, IResource.DEPTH_INFINITE);

      IProjectDescription description = project.getDescription();
      String[] natures = description.getNatureIds();
      ArrayList newNatures = new ArrayList();
      for(int i = 0; i < natures.length; ++i) {
        if(!Maven2Plugin.NATURE_ID.equals(natures[i])) {
          newNatures.add(natures[i]);
        }
      }
      description.setNatureIds((String[]) newNatures.toArray(new String[newNatures.size()]));
      project.setDescription(description, null);

      IJavaProject javaProject = JavaCore.create(project);
      if(javaProject != null) {
        // remove classpatch container from JavaProject
        IClasspathEntry[] entries = javaProject.getRawClasspath();
        ArrayList newEntries = new ArrayList();
        for(int i = 0; i < entries.length; i++ ) {
          if(!isMaven2ClasspathContainer(entries[i].getPath())) {
            newEntries.add(entries[i]);
          }
        }
        javaProject.setRawClasspath((IClasspathEntry[]) newEntries.toArray(new IClasspathEntry[newEntries.size()]),
            null);
      }

    } catch(CoreException ex) {
      Maven2Plugin.log(ex);
    }
  }

  public static boolean isMaven2ClasspathContainer(IPath containerPath) {
    return containerPath != null && containerPath.segmentCount() > 0
        && Maven2Plugin.CONTAINER_ID.equals(containerPath.segment(0));
  }

  public static IClasspathContainer getMaven2ClasspathContainer(IJavaProject project) throws JavaModelException {
    IClasspathEntry[] entries = project.getRawClasspath();
    for(int i = 0; i < entries.length; i++ ) {
      IClasspathEntry entry = entries[i];
      if(entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER && isMaven2ClasspathContainer(entry.getPath())) {
        return JavaCore.getClasspathContainer(entry.getPath(), project);
      }
    }
    return null;
  }

  private static String getBuildOption(MavenProject project, String artifactId, String optionName) {
    String option = getBuildOption(project.getBuild().getPlugins(), artifactId, optionName);
    if(option != null) {
      return option;
    }
    PluginManagement pluginManagement = project.getBuild().getPluginManagement();
    if(pluginManagement != null) {
      return getBuildOption(pluginManagement.getPlugins(), artifactId, optionName);
    }
    return null;
  }

  private static String getBuildOption(List plugins, String artifactId, String optionName) {
    for(Iterator it = plugins.iterator(); it.hasNext();) {
      Plugin plugin = (Plugin) it.next();
      if(artifactId.equals(plugin.getArtifactId())) {
        Xpp3Dom o = (Xpp3Dom) plugin.getConfiguration();
        if(o != null && o.getChild(optionName) != null) {
          return o.getChild(optionName).getValue();
        }
      }
    }
    return null;
  }

  private static final List VERSIONS = Arrays.asList("1.1,1.2,1.3,1.4,1.5,1.6,1.7".split(","));

  private static void setVersion(Map options, String name, String value) {
    if(value == null) {
      return;
    }
    String current = (String) options.get(name);
    if(current == null) {
      options.put(name, value);
    } else {
      int oldIndex = VERSIONS.indexOf(current);
      int newIndex = VERSIONS.indexOf(value.trim());
      if(newIndex > oldIndex) {
        options.put(name, value);
      }
    }
  }

  private static void addMarker(IResource resource, String message, int lineNumber, int severity) {
    try {
      if(resource.isAccessible()) {
        IMarker marker = resource.createMarker(Maven2Plugin.MARKER_ID);
        marker.setAttribute(IMarker.MESSAGE, message);
        marker.setAttribute(IMarker.SEVERITY, severity);
        if(lineNumber == -1) {
          lineNumber = 1;
        }
        marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
      }
    } catch(CoreException ex) {
      Maven2Plugin.getDefault().getConsole().logError("Unable to add marker; " + ex.toString());
    }
  }

  private static void deleteMarkers(IResource resource) {
    try {
      if(resource.isAccessible()) {
        resource.deleteMarkers(Maven2Plugin.MARKER_ID, false, IResource.DEPTH_ZERO);
      }
    } catch(CoreException ex) {
      Maven2Plugin.getDefault().getConsole().logError("Unable to delete marker; " + ex.toString());
    }
  }

  public IProject importProject(File pomFile, Model model, ResolverConfiguration configuration, IProgressMonitor monitor)
      throws CoreException {
    String projectName = model.getArtifactId();
    IWorkspace workspace = ResourcesPlugin.getWorkspace();

    IWorkspaceRoot root = workspace.getRoot();
    IProject project = root.getProject(projectName);
    if(project.exists()) {
      return null;
    }

    if(pomFile.getAbsolutePath().startsWith(workspace.getRoot().getLocation().toFile().getAbsolutePath())) {
      project.create(monitor);
    } else {
      IProjectDescription description = workspace.newProjectDescription(projectName);
      description.setLocation(new Path(pomFile.getParentFile().getAbsolutePath()));
      project.create(description, monitor);
    }

    if(!project.isOpen()) {
      project.open(monitor);
    }

    configureProject(project, configuration, monitor);

    return project;
  }

  public void configureProject(IProject project, ResolverConfiguration configuration, IProgressMonitor monitor)
      throws CoreException {
    BuildPathManager buildpathManager = Maven2Plugin.getDefault().getBuildpathManager();
    buildpathManager.enableMavenNature(project, configuration, monitor);
    buildpathManager.updateSourceFolders(project, configuration, monitor);
//    buildpathManager.updateClasspathContainer(project, monitor);
  }

  private boolean hasDynamicWebProjectNature(IProject project) {
    try {
      if(project.hasNature("org.eclipse.wst.common.modulecore.ModuleCoreNature")
          || project.hasNature("org.eclipse.wst.common.project.facet.core.nature")) {
        return true;
      }
    } catch(Exception e) {
      console.logError("Unable to inspect nature: " + e);
    }
    return false;
  }

  /**
   * Schedules classpath container of the given project to be refreshed in a background job. This method returns
   * immediately.
   */
  public void scheduleUpdateClasspathContainer(IProject project) {
    refreshJob.queueRefresh(project);
  }

  /**
   * Schedules classpath containers of the given collection of projects to be refreshed in a background job. This method
   * returns immediately.
   */
  public void scheduleUpdateClasspathContainer(Collection projects) {
    refreshJob.queueRefresh(projects);
  }

  static class RefreshJob extends WorkspaceJob {

    private static final long PROCESSING_DELAY = 1000L;

    private final BuildPathManager buildPathManager;

    private final Maven2Console console;

    private final ArrayList queue = new ArrayList();

    public RefreshJob(BuildPathManager buildPathManager, Maven2Console console) {
      super("Maven classpath container refresh job");
      this.buildPathManager = buildPathManager;
      this.console = console;
    }

    public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
      while(true) {
        Set projects = new HashSet();
        synchronized(queue) {
          projects.addAll(queue);
          queue.clear();
        }
        if(projects.isEmpty()) {
          break;
        }

        Map resolved = new HashMap();
        for(Iterator piter = projects.iterator(); piter.hasNext();) {
          IProject project = (IProject) piter.next();
          try {
            buildPathManager.internalUpdateClasspathContainer(project, resolved, monitor);
          } catch(Exception e) {
            console.logError("Unable to refresh classpath container: " + e);
          }
        }
        buildPathManager.setClasspathContainer(resolved, monitor);
      }
      return Status.OK_STATUS;
    }

    public void queueRefresh(IProject project) {
      synchronized(queue) {
        queue.add(project);
      }
      schedule(PROCESSING_DELAY);
    }

    public void queueRefresh(Collection projects) {
      if(projects == null || projects.isEmpty())
        return;
      synchronized(queue) {
        queue.addAll(projects);
      }
      schedule(PROCESSING_DELAY);
    }
  }

}
