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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ResolutionListener;
import org.apache.maven.artifact.versioning.VersionRange;


public class EclipseResolutionListener implements ResolutionListener {

  public void endProcessChildren(Artifact arg0) {
    // TODO Auto-generated method endProcessChildren
    
  }

  public void includeArtifact(Artifact arg0) {
    // TODO Auto-generated method includeArtifact
    
  }

  public void manageArtifact(Artifact arg0, Artifact arg1) {
    // TODO Auto-generated method manageArtifact
    
  }

  public void omitForCycle(Artifact arg0) {
    // TODO Auto-generated method omitForCycle
    
  }

  public void omitForNearer(Artifact arg0, Artifact arg1) {
    // TODO Auto-generated method omitForNearer
    
  }

  public void restrictRange(Artifact arg0, Artifact arg1, VersionRange arg2) {
    // TODO Auto-generated method restrictRange
    
  }

  public void selectVersionFromRange(Artifact arg0) {
    // TODO Auto-generated method selectVersionFromRange
    
  }

  public void startProcessChildren(Artifact arg0) {
    // TODO Auto-generated method startProcessChildren
    
  }

  public void testArtifact(Artifact arg0) {
    // TODO Auto-generated method testArtifact
    
  }

  public void updateScope(Artifact arg0, String arg1) {
    // TODO Auto-generated method updateScope
    
  }

  public void updateScopeCurrentPom(Artifact arg0, String arg1) {
    // TODO Auto-generated method updateScopeCurrentPom
    
  }
  
}

