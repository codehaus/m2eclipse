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

package org.maven.ide.eclipse.mylar;

import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.Model;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.core.TaskRepositoryManager;
import org.eclipse.mylar.tasks.ui.AbstractTaskRepositoryLinkProvider;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.embedder.MavenModelManager;

/**
 * Repository Link provider for Maven metadata
 * 
 * @author Eugene Kuleshov
 */
public class MavenRepositoryLinkProvider extends
    AbstractTaskRepositoryLinkProvider {

  public TaskRepository getTaskRepository(IResource resource,
      TaskRepositoryManager repositoryManager) {
    String url = getIssueManagementUrl(resource);
    if (url != null) {
      for (TaskRepository repository : repositoryManager.getAllRepositories()) {
        if (repository.getUrl().startsWith(url)) {
          return repository;
        }
      }
    }
    return null;
  }

  private String getIssueManagementUrl(IResource resource) {
    IProject project = resource.getProject();
    if (project == null || !project.isAccessible()) {
      return null;
    }

    IFile pomFile = (IFile) project.findMember(Maven2Plugin.POM_FILE_NAME);
    if (pomFile == null) {
      return null;
    }

    // currently only look in the given pom and don't scan trough parent model

    MavenModelManager modelManager = Maven2Plugin.getDefault().getMavenModelManager();
    Model model = modelManager.getMavenModel(pomFile);
    if (model == null) {
      try {
        model = modelManager.readMavenModel(pomFile.getLocation().toFile());
      } catch (CoreException ex) {
        // ignore
      }
    }

    String url = null;
    if (model != null) {
      IssueManagement issueManagement = model.getIssueManagement();
      if (issueManagement != null) {
        url = issueManagement.getUrl();
      }
    }
    return url;
  }

}
