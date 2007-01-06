
package org.maven.ide.eclipse.embedder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;

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
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.launch.console.Maven2Console;


/**
 * This class maintain the mapping between Eclipse projects and
 * Maven models, and be able to lookup projects and models or artifacts.
 * 
 * @author Scott Cytacki
 * @author Eugene Kuleshov
 */
public class MavenModelManager {
  private final MavenEmbedderManager embedderManager;
  private final Maven2Console console;

  private Map models;
  private Map artifacts;


  public MavenModelManager(MavenEmbedderManager embedderManager, Maven2Console console) {
    this.embedderManager = embedderManager;
    this.console = console;
  }

  public IFile getArtifactFile(Artifact a) {
    return (IFile) artifacts.get(getArtifactKey(a));
  }

  public Model getMavenModel(IFile pomFile) {
    return (Model) models.get(pomFile.getLocation().toString());
  }

  public synchronized void initMavenModel(IProgressMonitor monitor) {
    if(models!=null) {
      return;
    }

    models = new HashMap();
    artifacts = new HashMap();
    
    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    for(int i = 0; i < projects.length; i++ ) {
      IProject project = projects[i];
      try {
        if(project.isOpen() && project.hasNature(Maven2Plugin.NATURE_ID)) {
          IFile pomFile = project.getFile(Maven2Plugin.POM_FILE_NAME);
          if(pomFile == null) {
            console.logError("Project " + project.getName() + " is missing pom.xml");
          } else {
            updateMavenModel(pomFile, true, monitor);
          }
        }
      } catch(CoreException ex) {
        console.logError("Unable to read project " + project.getName() + "; " + ex.getMessage());
      }
    }
  }

  public Model updateMavenModel(IFile pomFile, boolean recursive, IProgressMonitor monitor) throws CoreException {
    Model mavenModel = readMavenModel(pomFile.getLocation().toFile());
    if(mavenModel == null) {
      console.logMessage("Unable to read model for " + pomFile.getFullPath().toString());
      return null;
    }

    models.put(pomFile.getLocation().toString(), mavenModel);
    String artifactKey = getArtifactKey(mavenModel);
    artifacts.put(artifactKey, pomFile);

    console.logMessage("Updated model " + pomFile.getFullPath().toString() + " : " + artifactKey);

    if(recursive) {
      IContainer parent = pomFile.getParent();
      for(Iterator it = mavenModel.getModules().iterator(); it.hasNext();) {
        if(monitor.isCanceled()) {
          throw new OperationCanceledException();
        }
        String module = (String) it.next();
        IResource memberPom = parent.findMember(module + "/" + Maven2Plugin.POM_FILE_NAME); //$NON-NLS-1$
        if(memberPom != null && memberPom.getType() == IResource.FILE) {
          updateMavenModel((IFile) memberPom, recursive, monitor);
        }
      }
    }

    return mavenModel;
  }

  public Model readMavenModel(File pomFile) throws CoreException {
    try {
      return embedderManager.getProjectEmbedder().readModel(pomFile);
    } catch(XmlPullParserException ex) {
      String msg = "Parsing error " + pomFile.getAbsolutePath()+"; " + ex.toString();
      console.logError(msg);
      throw new CoreException(new Status(IStatus.ERROR, Maven2Plugin.PLUGIN_ID, IStatus.ERROR, msg, ex));
    } catch(IOException ex) {
      String msg = "Can't read model " + pomFile.getAbsolutePath()+"; " + ex.toString();
      console.logError(msg);
      throw new CoreException(new Status(IStatus.ERROR, Maven2Plugin.PLUGIN_ID, IStatus.ERROR, msg, ex));
    }
  }

  private String getArtifactKey(Model model) {
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

  /**
   * Create a key that represents the artifact which can be used in a Map.
   */
  private String getArtifactKey(Artifact a) {
    return a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getVersion();
  }

}
