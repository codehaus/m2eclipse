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

package org.maven.ide.eclipse.wizards;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Model;

/**
 * @author Eugene Kuleshov
 */
public class MavenProjectInfo {
  private final MavenProjectInfo parent;

  private final String label;

  private final File pomFile;

  private final Model model;

  /**
   * List of MavenProjectInfo
   */
  private final List projects = new ArrayList();


  public MavenProjectInfo(String label, File pomFile, Model model, MavenProjectInfo parent) {
    this.label = label;
    this.pomFile = pomFile;
    this.model = model;
    this.parent = parent;
  }

  public void add(MavenProjectInfo mavenProjectInfo) {
    projects.add(mavenProjectInfo);
  }
  
  public String getLabel() {
    return this.label;
  }
  
  public File getPomFile() {
    return this.pomFile;
  }
  
  public Model getModel() {
    return this.model;
  }
  
  public List getProjects() {
    return this.projects;
  }
 
  public MavenProjectInfo getParent() {
    return this.parent;
  }
  
}