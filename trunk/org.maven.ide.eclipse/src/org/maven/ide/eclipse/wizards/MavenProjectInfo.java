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
class MavenProjectInfo {
  public final File pomFile;

  public final Model model;

  /**
   * List of MavenProjectInfo
   */
  public final List projects = new ArrayList();

  public MavenProjectInfo(File pomFile, Model model) {
    this.pomFile = pomFile;
    this.model = model;
  }

  public void add(MavenProjectInfo mavenProjectInfo) {
    projects.add(mavenProjectInfo);
  }
}