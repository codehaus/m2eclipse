
package org.maven.ide.eclipse;

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
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

import org.eclipse.core.resources.IFile;
import org.maven.ide.eclipse.launch.console.Maven2Console;


public class EclipseArtifactResolver extends DefaultArtifactResolver {
  private Maven2Console console;

  public EclipseArtifactResolver() {
    this.console = Maven2Plugin.getDefault().getConsole();
  }

  public void resolve( Artifact artifact, List arg1, ArtifactRepository arg2 ) throws ArtifactResolutionException,
      ArtifactNotFoundException {
    if( !resolveAsEclipseProject( artifact ) ) {
      super.resolve( artifact, arg1, arg2 );
    }
  }

  public void resolveAlways( Artifact artifact, List arg1, ArtifactRepository arg2 )
      throws ArtifactResolutionException, ArtifactNotFoundException {
    if( !resolveAsEclipseProject( artifact ) ) {
      super.resolveAlways( artifact, arg1, arg2 );
    }
  }

  public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
      ArtifactRepository arg2, List arg3, ArtifactMetadataSource arg4, ArtifactFilter arg5 )
      throws ArtifactResolutionException, ArtifactNotFoundException {
    addProjectParentPom( artifacts, originatingArtifact );
    return super.resolveTransitively( artifacts, originatingArtifact, arg2, arg3, arg4, arg5 );
  }

  private void addProjectParentPom( Set artifacts, Artifact originatingArtifact ) {
    MavenModelManager modelManager = Maven2Plugin.getDefault().getMavenModelManager();
    
    IFile pomFile = modelManager.getArtifactFile( originatingArtifact );
    if( pomFile == null ) {
      return;
    }
    
    // Currently the version, groupId, and artifactId of the parent have
    // to be fully specified in a pom.  But that might change according
    // to this: http://docs.codehaus.org/display/MAVEN/Release+Management      
    // If that is used somewhere then this code will have to be updated
    Model model = modelManager.getMavenModel( pomFile );
    if( model == null ) {
      return;
    }

    Parent parent = model.getParent();
    if( parent == null ) {
      return;
    }

    Artifact artifact = artifactFactory.createProjectArtifact( parent.getGroupId(), parent.getArtifactId(), parent.getVersion() );
    File file = pomFile.getParent().getLocation().append( model.getParent().getRelativePath() ).toFile();
    if( file.exists() ) {
      artifact.setFile( file );
      artifact.setResolved( true );

      artifacts.add( artifact );
    }
  }

  public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact, List arg2,
      ArtifactRepository arg3, ArtifactMetadataSource arg4, List arg5 ) throws ArtifactResolutionException,
      ArtifactNotFoundException {
    addProjectParentPom( artifacts, originatingArtifact );
    return super.resolveTransitively( artifacts, originatingArtifact, arg2, arg3, arg4, arg5 );
  }

  public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact, List arg2,
      ArtifactRepository arg3, ArtifactMetadataSource arg4 ) throws ArtifactResolutionException,
      ArtifactNotFoundException {
    addProjectParentPom( artifacts, originatingArtifact );
    return super.resolveTransitively( artifacts, originatingArtifact, arg2, arg3, arg4 );
  }

  public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact, Map arg2,
      ArtifactRepository arg3, List arg4, ArtifactMetadataSource arg5, ArtifactFilter arg6, List arg7 )
      throws ArtifactResolutionException, ArtifactNotFoundException {
    addProjectParentPom( artifacts, originatingArtifact );
    return super.resolveTransitively( artifacts, originatingArtifact, arg2, arg3, arg4, arg5, arg6, arg7 );
  }

  public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact, Map arg2,
      ArtifactRepository arg3, List arg4, ArtifactMetadataSource arg5, ArtifactFilter arg6 )
      throws ArtifactResolutionException, ArtifactNotFoundException {
    addProjectParentPom( artifacts, originatingArtifact );
    return super.resolveTransitively( artifacts, originatingArtifact, arg2, arg3, arg4, arg5, arg6 );
  }

  public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact, Map arg2,
      ArtifactRepository arg3, List arg4, ArtifactMetadataSource arg5 ) throws ArtifactResolutionException,
      ArtifactNotFoundException {
    addProjectParentPom( artifacts, originatingArtifact );
    return super.resolveTransitively( artifacts, originatingArtifact, arg2, arg3, arg4, arg5 );
  }

  private boolean resolveAsEclipseProject( Artifact artifact ) {
    if( artifact == null ) {
      // according to the DefaultArtifactResolver source code, it looks like artifact can be null
      return false;
    }
          
    MavenModelManager modelManager = Maven2Plugin.getDefault().getMavenModelManager();    
    IFile file = modelManager.getArtifactFile( artifact );
    if( file==null || !file.exists() ) {
      // TODO enable it back, but only when "debug" is turned on in preferences
      // console.logMessage( artifact.getId() + ": no POM file in Eclipse workspace" );
      return false;
    }

    artifact.setFile( file.getLocation().toFile() );
    artifact.setResolved( true );

    String loc = artifact.getFile().getAbsolutePath();
    console.logMessage( artifact.getId() + ": resolved to Eclipse workspace - found at " + loc );
    return true;
  }
  
}

