
package org.maven.ide.eclipse;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.model.Model;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;


/**
 * This class maintain the mapping between Eclipse projects and
 * Maven models, and be able to lookup projects and models or artifacts.
 * 
 * @author Scott Cytacki
 * @author Eugene Kuleshov
 */
public class MavenModelManager {
  private Map models;
  private Map artifacts;

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
            Maven2Plugin.getDefault().getConsole().logError("Project " + project.getName() + " is missing pom.xml");
          } else {
            updateMavenModel(pomFile, true, monitor);
          }
        }
      } catch(CoreException ex) {
        Maven2Plugin.getDefault().getConsole().logError("Unable to read project " + project.getName() + "; " + ex.getMessage());
      }
    }
  }

  public Model updateMavenModel(IFile pomFile, boolean recursive, IProgressMonitor monitor) throws CoreException {
    Model mavenModel = readMavenModel(pomFile, monitor);
    if(mavenModel == null) {
      Maven2Plugin.getDefault().getConsole().logMessage("Unable to read model for " + pomFile.getFullPath().toString());
      return null;
    }

    models.put(pomFile.getLocation().toString(), mavenModel);
    String artifactKey = getArtifactKey(mavenModel);
    artifacts.put(artifactKey, pomFile);

    Maven2Plugin.getDefault().getConsole().logMessage(
        "Updated model " + pomFile.getFullPath().toString() + " : " + artifactKey);

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

  private Model readMavenModel(final IFile pomFile, IProgressMonitor monitor) throws CoreException {
    return (Model) Maven2Plugin.getDefault().executeInEmbedder(new MavenEmbedderCallback() {
      public Object run(MavenEmbedder mavenEmbedder, IProgressMonitor monitor) throws Exception {
        return mavenEmbedder.readModel(pomFile.getLocation().toFile());
      }
    }, monitor);
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
