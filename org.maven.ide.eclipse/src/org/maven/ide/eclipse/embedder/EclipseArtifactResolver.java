
package org.maven.ide.eclipse.embedder;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.DefaultArtifactResolver;
import org.apache.maven.artifact.resolver.ResolutionListener;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

import org.eclipse.core.resources.IFile;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.launch.console.Maven2Console;


public class EclipseArtifactResolver extends DefaultArtifactResolver {
  private Maven2Console console;
  private MavenModelManager modelManager;

  private ResolutionListener resolutionListener;
  
  
  public EclipseArtifactResolver() {
    Maven2Plugin plugin = Maven2Plugin.getDefault();
    this.console = plugin.getConsole();
    this.modelManager = plugin.getMavenModelManager();
  }

  public void resolve(Artifact artifact, List remoteRepositories, ArtifactRepository localRepository) throws ArtifactResolutionException,
      ArtifactNotFoundException {
    if(!resolveAsEclipseProject(artifact)) {
      super.resolve(artifact, remoteRepositories, localRepository);
    }
  }

  public void resolveAlways(Artifact artifact, List remoteRepositories, ArtifactRepository localRepository) throws ArtifactResolutionException,
      ArtifactNotFoundException {
    if(!resolveAsEclipseProject(artifact)) {
      super.resolveAlways(artifact, remoteRepositories, localRepository);
    }
  }

  public ArtifactResolutionResult resolveTransitively(Set artifacts, Artifact originatingArtifact,
      ArtifactRepository localRepository, List remoteRepositories, ArtifactMetadataSource source, ArtifactFilter filter)
      throws ArtifactResolutionException, ArtifactNotFoundException {
    addProjectParentPom(artifacts, originatingArtifact);
    ArtifactResolutionResult result = super.resolveTransitively(artifacts, originatingArtifact, localRepository, remoteRepositories, source, filter);
    return result;
  }

  public ArtifactResolutionResult resolveTransitively(Set artifacts, Artifact originatingArtifact,
      List remoteRepositories, ArtifactRepository localRepository, ArtifactMetadataSource source, List listeners)
      throws ArtifactResolutionException, ArtifactNotFoundException {
    addProjectParentPom(artifacts, originatingArtifact);
    ArtifactResolutionResult result = super.resolveTransitively(artifacts, originatingArtifact, remoteRepositories, localRepository, source, listeners);
    return result;
  }

  public ArtifactResolutionResult resolveTransitively(Set artifacts, Artifact originatingArtifact,
      List remoteRepositories, ArtifactRepository localRepository, ArtifactMetadataSource source)
      throws ArtifactResolutionException, ArtifactNotFoundException {
    addProjectParentPom(artifacts, originatingArtifact);
    ArtifactResolutionResult result = super.resolveTransitively(artifacts, originatingArtifact, remoteRepositories, localRepository, source);
    return result;
  }

  public ArtifactResolutionResult resolveTransitively(Set artifacts, Artifact originatingArtifact, Map managedVersions,
      ArtifactRepository localRepository, List remoteRepositories, ArtifactMetadataSource source,
      ArtifactFilter filter, List listeners) throws ArtifactResolutionException, ArtifactNotFoundException {
    addProjectParentPom(artifacts, originatingArtifact);
    ArtifactResolutionResult result = super.resolveTransitively(artifacts, originatingArtifact, managedVersions, localRepository,
        remoteRepositories, source, filter, listeners);
    return result;
  }

  public ArtifactResolutionResult resolveTransitively(Set artifacts, Artifact originatingArtifact, Map managedVersions,
      ArtifactRepository localRepository, List remoteRepositories, ArtifactMetadataSource source, ArtifactFilter filter)
      throws ArtifactResolutionException, ArtifactNotFoundException {
    addProjectParentPom(artifacts, originatingArtifact);
    ArtifactResolutionResult result = super.resolveTransitively(artifacts, originatingArtifact, managedVersions, localRepository,
        remoteRepositories, source, filter);
    return result;
  }

  public ArtifactResolutionResult resolveTransitively(Set artifacts, Artifact originatingArtifact, Map managedVersions,
      ArtifactRepository localRepository, List remoteRepositories, ArtifactMetadataSource source)
      throws ArtifactResolutionException, ArtifactNotFoundException {
    addProjectParentPom(artifacts, originatingArtifact);
    ArtifactResolutionResult result = super.resolveTransitively(artifacts, originatingArtifact, managedVersions, localRepository,
        remoteRepositories, source);
    return result;
  }

  private void addProjectParentPom(Set artifacts, Artifact originatingArtifact) {
    MavenModelManager modelManager = Maven2Plugin.getDefault().getMavenModelManager();

    IFile pomFile = modelManager.getArtifactFile(originatingArtifact);
    if(pomFile == null) {
      return;
    }

    // Currently the version, groupId, and artifactId of the parent have
    // to be fully specified in a pom.  But that might change according
    // to this: http://docs.codehaus.org/display/MAVEN/Release+Management      
    // If that is used somewhere then this code will have to be updated
    Model model = modelManager.getMavenModel(pomFile);
    if(model == null) {
      return;
    }

    Parent parent = model.getParent();
    if(parent == null) {
      return;
    }

    Artifact artifact = artifactFactory.createProjectArtifact(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
    File file = pomFile.getParent().getLocation().append(parent.getRelativePath()).toFile();
    if(file.exists()) {
      artifact.setFile(file);
      artifact.setResolved(true);

      artifacts.add(artifact);
    }
  }
  
  private boolean resolveAsEclipseProject(Artifact artifact) {
    if(artifact == null) {
      // according to the DefaultArtifactResolver source code, it looks like artifact can be null
      return false;
    }

    IFile file = modelManager.getArtifactFile(artifact);
    if(file == null || !file.exists()) {
      // TODO enable it back, but only when "debug" is turned on in preferences
      // console.logMessage( artifact.getId() + ": no POM file in Eclipse workspace" );
      return false;
    }

//    if(!"pom".equals(artifact.getType())) {
//      return false;
//    }

    artifact.setFile(file.getLocation().toFile());
    artifact.setResolved(true);

    String loc = artifact.getFile().getAbsolutePath();
    console.logMessage(artifact.getId() + " in Eclipse Workspace " + loc);
    return true;
  }

}