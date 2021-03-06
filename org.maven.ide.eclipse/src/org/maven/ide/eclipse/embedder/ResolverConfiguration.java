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

package org.maven.ide.eclipse.embedder;

import java.util.Arrays;
import java.util.List;

/**
 * Resolver configuration holder
 *
 * @author Eugene Kuleshov
 */
public class ResolverConfiguration {

  private boolean includeModules;
  private boolean resolveWorkspaceProjects;
  private String activeProfiles;

  public ResolverConfiguration() {
    this(false, true, "");
  }
  
  public ResolverConfiguration(boolean includeModules, boolean resolveWorkspaceProjects, String activeProfiles) {
    this.includeModules = includeModules;
    this.resolveWorkspaceProjects = resolveWorkspaceProjects;
    this.activeProfiles = activeProfiles;
  }

  public boolean shouldIncludeModules() {
    return this.includeModules;
  }
  
  public boolean shouldResolveWorkspaceProjects() {
    return this.resolveWorkspaceProjects;
  }

  public String getActiveProfiles() {
    return this.activeProfiles;
  }
  
  public List getActiveProfileList() {
    return Arrays.asList(activeProfiles.split(",\\s\\|"));
  }

  public void setResolveWorkspaceProjects(boolean resolveWorkspaceProjects) {
    this.resolveWorkspaceProjects = resolveWorkspaceProjects;
  }
  
  public void setIncludeModules(boolean includeModules) {
    this.includeModules = includeModules;
  }
  
  public void setActiveProfiles(String activeProfiles) {
    this.activeProfiles = activeProfiles;
  }
  
}
