/*
 * Licensed to the Codehaus Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The Codehaus Foundation licenses
 * this file to you under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package org.maven.ide.eclipse.subclipse;

import org.apache.maven.model.Model;

import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.svnclientadapter.SVNUrl;

import org.maven.ide.eclipse.wizards.MavenProjectInfo;

/**
 * @author Eugene Kuleshov
 */
public class MavenProjectSVNInfo extends MavenProjectInfo {

  private final SVNUrl folderUrl;
  private final ISVNRepositoryLocation repository;

  public MavenProjectSVNInfo(String label, Model model, SVNUrl folderUrl, ISVNRepositoryLocation repository, MavenProjectInfo parent) {
    super(label, null, model, parent);
    this.folderUrl = folderUrl;
    this.repository = repository;
  }
  
  public SVNUrl getFolderUrl() {
    return folderUrl;
  }
  
  public ISVNRepositoryLocation getRepository() {
    return repository;
  }

}
