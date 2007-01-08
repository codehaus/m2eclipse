
package org.maven.ide.eclipse.embedder;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidArtifactRTException;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Model;
import org.apache.maven.project.InvalidProjectModelException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.validation.ModelValidationResult;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.Messages;
import org.maven.ide.eclipse.container.Maven2ClasspathContainerInitializer;
import org.maven.ide.eclipse.index.MavenRepositoryIndexManager;
import org.maven.ide.eclipse.launch.console.Maven2Console;
import org.maven.ide.eclipse.preferences.Maven2PreferenceConstants;
import org.maven.ide.eclipse.util.Util;


public class ClassPathResolver {
  private final MavenEmbedderManager embedderManager;
  private final Maven2Console console;
  private final MavenModelManager mavenModelManager;
  private final IPreferenceStore preferenceStore;
  private final MavenRepositoryIndexManager indexManager;

  
  public ClassPathResolver(MavenEmbedderManager embedderManager, Maven2Console console,
      MavenModelManager mavenModelManager, MavenRepositoryIndexManager indexManager, IPreferenceStore preferenceStore) {
    this.embedderManager = embedderManager;
    this.console = console;
    this.mavenModelManager = mavenModelManager;
    this.indexManager = indexManager;
    this.preferenceStore = preferenceStore;
  }

  public void resolveClasspathEntries(Set libraryEntries, Set moduleArtifacts, IFile pomFile, boolean recursive,
      IProgressMonitor monitor) {
    console.logMessage("Reading " + pomFile.getFullPath());

    IProject currentProject = pomFile.getProject();

    monitor.beginTask("Reading " + pomFile.getFullPath(), IProgressMonitor.UNKNOWN);
    try {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      boolean offline = preferenceStore.getBoolean(Maven2PreferenceConstants.P_OFFLINE);
      boolean downloadSources = !offline & preferenceStore.getBoolean(Maven2PreferenceConstants.P_DOWNLOAD_SOURCES);
      boolean downloadJavadoc = !offline & preferenceStore.getBoolean(Maven2PreferenceConstants.P_DOWNLOAD_JAVADOC);

      MavenEmbedder embedder = embedderManager.getProjectEmbedder();

      ReadProjectTask readProjectTask = new ReadProjectTask(pomFile, console, indexManager, preferenceStore);
      MavenProject mavenProject = (MavenProject) readProjectTask.run(embedder, new SubProgressMonitor(monitor, 1));
      if(mavenProject == null) {
        return;
      }

      // deleteMarkers(pomFile);
      // TODO use version?
      moduleArtifacts.add(mavenProject.getGroupId() + ":" + mavenProject.getArtifactId());

      Set artifacts = mavenProject.getArtifacts();
      for(Iterator it = artifacts.iterator(); it.hasNext();) {
        if(monitor.isCanceled()) {
          throw new OperationCanceledException();
        }

        final Artifact a = (Artifact) it.next();

        monitor.subTask("Processing " + a.getId());
        String artifactLocation = a.getFile().getAbsolutePath();

        // The artifact filename cannot be used here to determine
        // the type because eclipse project artifacts don't have jar or zip file names.
        // TODO use version?
        if(!moduleArtifacts.contains(a.getGroupId() + ":" + a.getArtifactId())
            && ("jar".equals(a.getType()) || "zip".equals(a.getType()))) {

          moduleArtifacts.add(a.getGroupId() + ":" + a.getArtifactId());

          IFile artifactFile = mavenModelManager.getArtifactFile(a);
          if(artifactFile != null) {
            IProject artifactProject = artifactFile.getProject();
            if(artifactProject.getFullPath().equals(currentProject.getFullPath())) {
              // This is another artifact in our current project so we should not
              // add our own project to ourself
              continue;
            }

            libraryEntries.add(JavaCore.newProjectEntry(artifactProject.getFullPath(), false));
            continue;
          }

          Path srcPath = materializeArtifactPath(embedder, mavenProject, a, "java-source", "sources", downloadSources, monitor);

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
            resolveClasspathEntries(libraryEntries, moduleArtifacts, (IFile) memberPom, true, 
                new SubProgressMonitor(monitor, 1));
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
          return url + "apidocs/";  // assuming project is used maven-generated site
        }
      }              
    }
    return null;
  }

  
  public static final class ReadProjectTask implements MavenEmbedderCallback {
    private final IFile file;
    private final Maven2Console console;
    private final IPreferenceStore preferenceStore;
    private final MavenRepositoryIndexManager indexManager;

    public ReadProjectTask(IFile file, Maven2Console console, MavenRepositoryIndexManager indexManager, IPreferenceStore preferenceStore) {
      this.file = file;
      this.console = console;
      this.indexManager = indexManager;
      this.preferenceStore = preferenceStore;
    }

    public Object run(MavenEmbedder mavenEmbedder, IProgressMonitor monitor) {
      monitor.beginTask("Reading " + file.getFullPath(), IProgressMonitor.UNKNOWN);
      try {
        monitor.subTask("Reading " + file.getFullPath());
        boolean offline = preferenceStore.getBoolean(Maven2PreferenceConstants.P_OFFLINE);
        boolean debug = preferenceStore.getBoolean(Maven2PreferenceConstants.P_DEBUG_OUTPUT);
        
        File pomFile = this.file.getLocation().toFile();

        MavenExecutionRequest request = EmbedderFactory.createMavenExecutionRequest(mavenEmbedder, offline, debug);
        request.setPomFile(pomFile.getAbsolutePath());
        request.setBaseDirectory(pomFile.getParentFile());
        request.setTransferListener(new TransferListenerAdapter(monitor, console, indexManager));

        MavenExecutionResult result = mavenEmbedder.readProjectWithDependencies(request);

        Util.deleteMarkers(this.file);

        if(!result.hasExceptions()) {
          return result.getMavenProject();
        }
        
        for(Iterator it = result.getExceptions().iterator(); it.hasNext();) {
          Exception ex = (Exception) it.next();
          if(ex instanceof ProjectBuildingException) {
            handleProjectBuildingException((ProjectBuildingException) ex);

          } else if(ex instanceof AbstractArtifactResolutionException) {
            String msg = ex.getMessage()
                .replaceAll("----------", "")
                .replaceAll("\r\n\r\n", "\n")
                .replaceAll("\n\n", "\n");
            Util.addMarker(this.file, msg, 1, IMarker.SEVERITY_ERROR);
            console.logError(msg);

            try {
              // TODO
              return mavenEmbedder.readProject(pomFile);
            
            } catch(ProjectBuildingException ex2) {
              handleProjectBuildingException(ex2);
            
            } catch(Exception ex2) {
              handleBuildException(ex2);
              
            }
            
          } else {
            handleBuildException(ex);
            
          }
        }

//      } catch(Exception ex) {
//        Util.deleteMarkers(this.file);
//        Util.addMarker(this.file, "Unable to read project; " + ex.toString(), 1, IMarker.SEVERITY_ERROR);
//        
//        String msg = "Unable to read " + file.getLocation() + "; " + ex.toString();
//        console.logError(msg);
//        Maven2Plugin.log(msg, ex);
      
      } finally {
        monitor.done();
      }

      return null;
    }

    private void handleBuildException(Exception ex) {
      String msg = Messages.getString("plugin.markerBuildError") + ex.getMessage();
      Util.addMarker(this.file, msg, 1, IMarker.SEVERITY_ERROR); //$NON-NLS-1$
      console.logError(msg);
    }

    private void handleProjectBuildingException(ProjectBuildingException ex) {
      Throwable cause = ex.getCause();
      if(cause instanceof XmlPullParserException) {
        XmlPullParserException pex = (XmlPullParserException) cause;
        String msg = Messages.getString("plugin.markerParsingError") + pex.getMessage();
        Util.addMarker(this.file, msg, pex.getLineNumber(), IMarker.SEVERITY_ERROR); //$NON-NLS-1$
        console.logError(msg + " at line " + pex.getLineNumber());
      } else if(ex instanceof InvalidProjectModelException) {
        InvalidProjectModelException mex = (InvalidProjectModelException) ex;
        ModelValidationResult validationResult = mex.getValidationResult();
        String msg = Messages.getString("plugin.markerBuildError") + mex.getMessage();
        console.logError(msg);
        if(validationResult == null) {
          Util.addMarker(this.file, msg, 1, IMarker.SEVERITY_ERROR); //$NON-NLS-1$
        } else {
          for(Iterator it = validationResult.getMessages().iterator(); it.hasNext();) {
            String message = (String) it.next();
            Util.addMarker(this.file, message, 1, IMarker.SEVERITY_ERROR); //$NON-NLS-1$
            console.logError("  " + message);
          }
        }
      } else {
        handleBuildException(ex);
      }
    }
  }
  
}

