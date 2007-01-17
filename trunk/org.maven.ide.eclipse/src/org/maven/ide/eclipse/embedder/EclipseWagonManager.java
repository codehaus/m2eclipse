
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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.manager.DefaultWagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;


public class EclipseWagonManager extends DefaultWagonManager {

//  private Set letGoes = Collections.synchronizedSet(new HashSet());

  
//  public void letGoThrough(Artifact artifact) {
//    letGoes.add(artifact);
//  }
//
//  public void cleanLetGone(Artifact artifact) {
//    letGoes.remove(artifact);
//  }

  public void getArtifact(Artifact artifact, List remoteRepositories) {
//    if(letGoes.contains(artifact)) {
      try {
        super.getArtifact(artifact, remoteRepositories);
      } catch(TransferFailedException ex) {
        // ignore, we will just pretend it didn't happen.
//        artifact.setResolved(true);
        System.out.println("EclipseWagonManager.getArtifact()");
      } catch(ResourceDoesNotExistException ex) {
        // ignore, we will just pretend it didn't happen.
//        artifact.setResolved(true);
        System.out.println("EclipseWagonManager.getArtifact()");
      }
//      letGoes.remove(artifact);
//    } else {
      artifact.setResolved(true);
//    }
  }

  public void getArtifact(Artifact artifact, ArtifactRepository repository) {
    try {
      super.getArtifact(artifact, repository);
    } catch(TransferFailedException ex) {
      // ignore, we will just pretend it didn't happen.
      System.out.println("EclipseWagonManager.getArtifact()");
    } catch(ResourceDoesNotExistException ex) {
      // ignore, we will just pretend it didn't happen.
      System.out.println("EclipseWagonManager.getArtifact() " + ex.toString());
    }
    artifact.setResolved(true);
  }

  public void putArtifact(File source, Artifact artifact, ArtifactRepository deploymentRepository) {
  }

  public void putArtifactMetadata(File source, ArtifactMetadata artifactMetadata, ArtifactRepository repository) {
  }

  public void getArtifactMetadata(ArtifactMetadata metadata, ArtifactRepository remoteRepository, File destination,
      String checksumPolicy) {
  }
  
}

