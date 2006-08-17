
package org.maven.ide.eclipse;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.model.Model;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;


/**
 * This class is intented to maintain the mapping between Eclipse projects and
 * Maven models, and be able to lookup projects and models or artifacts.
 * 
 * @author Scott Cytacki
 * @author Eugene Kuleshov
 */
public class MavenModelManager {
  private Map models = new HashMap();
  private Map artifacts = new HashMap();
  
  
  public IFile getArtfactFile(Artifact a) {
    return (IFile) artifacts.get(getArtifactKey(a));
  }

  public Model getMavenModel(IFile pomFile) {
    return (Model) models.get(pomFile);
  }
  
  public Model updateMavenModel(IFile pomFile) throws CoreException {
    Model model = readMavenModel(pomFile);
    models.put( pomFile, model );
    String artifactKey = getArtifactKey( model );
    artifacts.put(artifactKey, pomFile);
    
    Maven2Plugin.getDefault().getConsole().logMessage("Updated model " + pomFile.getFullPath().toString() +" : " + artifactKey);
    
    return model;
  }
  
  
  private Model readMavenModel(final IFile pomFile) throws CoreException {
    return (Model) Maven2Plugin.getDefault().executeInEmbedder( 
        "Loading Maven Model" , new MavenEmbedderCallback() {
            public Object run( MavenEmbedder mavenEmbedder, IProgressMonitor monitor ) throws Exception {
              return mavenEmbedder.readModel( pomFile.getLocation().toFile() );
            }
          });        
  }
  
  private String getArtifactKey(Model model) {
    String groupId = model.getGroupId();
    if(groupId == null) {
      // If the groupId is null in the model, then it needs to be inherited
      // from the parent.  And the parent's groupId has to be specified in the
      // in the parent element of the model.
      groupId = model.getParent().getGroupId();
    }
    
    return groupId + ":" + model.getArtifactId() + ":" + model.getVersion();
  }

  /**
   * Create a key that represents the artifact which can be used in a Map.
   */
  private String getArtifactKey(Artifact a) {
    return a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getVersion();    
  }

}

