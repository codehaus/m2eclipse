
package org.maven.ide.eclipse.embedder;

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


public class EclipseArtifactResolver extends DefaultArtifactResolver {
  private Maven2Plugin plugin;

  private ResolutionListener resolutionListener;
  
  
  public EclipseArtifactResolver() {
    plugin = Maven2Plugin.getDefault();
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

    MavenModelManager modelManager = plugin.getMavenModelManager();
    IFile file = modelManager.getArtifactFile(artifact);
    if(file == null) {
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
    plugin.getConsole().logMessage(artifact.getId() + " in Eclipse Workspace " + loc);
    return true;
  }

}
