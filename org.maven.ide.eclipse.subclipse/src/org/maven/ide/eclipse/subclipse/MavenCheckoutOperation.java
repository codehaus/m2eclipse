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

package org.maven.ide.eclipse.subclipse;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.ui.operations.CheckoutAsProjectOperation;
import org.tigris.subversion.svnclientadapter.SVNRevision;

import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.embedder.BuildPathManager;
import org.maven.ide.eclipse.embedder.ResolverConfiguration;
import org.maven.ide.eclipse.wizards.MavenProjectInfo;


/**
 * Checkout operation
 * 
 * @author Eugene Kuleshov
 */
public class MavenCheckoutOperation implements IRunnableWithProgress {

  private ResolverConfiguration configuration;

  private List mavenProjects;

  private SVNRevision revision;

  private File location;

  private boolean workspaceLocation;

  private ISVNRemoteFolder[] folders;

  public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

    if(mavenProjects == null) {
      MavenProjectSVNScanner scanner = new MavenProjectSVNScanner(folders, revision, //
          Maven2Plugin.getDefault().getMavenModelManager());
      scanner.run(monitor);

      if(configuration.shouldIncludeModules()) {
        mavenProjects = scanner.getProjects();
      } else {
        mavenProjects = new ArrayList();
        collectChildProjects(mavenProjects, scanner.getProjects());
      }
    }

    List remoteFolderList = new ArrayList();
    List localFolderList = new ArrayList();
    List remoteFolderUrls = new ArrayList();

    // sort nested projects
    for(Iterator it = mavenProjects.iterator(); it.hasNext();) {
      MavenProjectSVNInfo info = (MavenProjectSVNInfo) it.next();
      String folderUrl = info.getFolderUrl().toString();

      boolean isNestedPath = false;
      for(Iterator it2 = mavenProjects.iterator(); it2.hasNext();) {
        MavenProjectSVNInfo info2 = (MavenProjectSVNInfo) it2.next();
        if(info != info2) {
          String path = info2.getFolderUrl().toString();
          if(folderUrl.startsWith(path)) {
            isNestedPath = true;
            break;
          }
        }
      }
      if(!isNestedPath) {
        ISVNRepositoryLocation repository = info.getRepository();
        String folderPath = folderUrl.substring(repository.getUrl().toString().length());
        ISVNRemoteFolder folder = repository.getRemoteFolder(folderPath);
        // TODO check if folder is not null
        remoteFolderList.add(folder);
        remoteFolderUrls.add(folderUrl);
        localFolderList.add(workspaceRoot.getProject(info.getModel().getArtifactId()));
      }
    }

    // checkout from SVN
    ISVNRemoteFolder[] remoteFolders = (ISVNRemoteFolder[]) remoteFolderList
        .toArray(new ISVNRemoteFolder[remoteFolderList.size()]);

    IProject[] localFolders = (IProject[]) localFolderList.toArray(new IProject[localFolderList.size()]);

    IPath locationPath = workspaceLocation ? null : Path.fromOSString(location.getAbsolutePath());

    try {
      CheckoutAsProjectOperation operation = new CheckoutAsProjectOperation(null, remoteFolders, localFolders,
          locationPath);
      operation.setSvnRevision(revision);
      operation.run(monitor);
    } catch(Exception ex) {
      String msg = "Checkout error; " + ex.toString();
      Maven2Plugin.getDefault().getConsole().logError(msg);
      Maven2Plugin.log(msg, ex);
      return; // TODO should we return ERROR status?
    }

    // update projects and import the missing ones
    BuildPathManager buildpathManager = Maven2Plugin.getDefault().getBuildpathManager();
    for(Iterator it = mavenProjects.iterator(); it.hasNext();) {
      MavenProjectSVNInfo info = (MavenProjectSVNInfo) it.next();
      monitor.subTask(info.getLabel());

      String folderUrl = info.getFolderUrl().toString();

      try {
        int n = remoteFolderUrls.indexOf(folderUrl);
        if(n > -1) {
          // project is already in workspace
          buildpathManager.configureProject((IProject) localFolderList.get(n), configuration, monitor);

        } else {
          // module project that need to e imported
          File pomFile = findPomFile(folderUrl, remoteFolderList, localFolderList);
          if(pomFile == null) {
            Maven2Plugin.getDefault().getConsole().logError("Can't find POM file for " + folderUrl);
          } else {
            buildpathManager.importProject(pomFile, info.getModel(), configuration, monitor);
          }
        }
      } catch(CoreException ex) {
        Maven2Plugin.getDefault().getConsole().logError(
            "Unable to create project for " + info.getModel().getId() + "; " + ex.toString());
      }

    }
  }

  private void collectChildProjects(List mavenProjects, List childProjects) {
    for(Iterator it = childProjects.iterator(); it.hasNext();) {
      MavenProjectInfo info = (MavenProjectInfo) it.next();
      mavenProjects.add(info);
      collectChildProjects(mavenProjects, info.getProjects());
    }
  }

  private File findPomFile(String folderUrl, List remoteFolderList, List localFolderList) {
    for(int i = 0; i < remoteFolderList.size(); i++ ) {
      ISVNRemoteFolder folder = (ISVNRemoteFolder) remoteFolderList.get(i);
      String url = folder.getUrl().toString();
      if(folderUrl.startsWith(url)) {
        IProject parentProject = (IProject) localFolderList.get(i);
        File parentFolder = parentProject.getLocation().toFile();
        return new File(parentFolder, folderUrl.substring(url.length()) + File.separator + Maven2Plugin.POM_FILE_NAME);
      }
    }
    return null;
  }

  public void setConfiguration(ResolverConfiguration configuration) {
    this.configuration = configuration;
  }

  public void setMavenProjectInfos(List mavenProjects) {
    this.mavenProjects = mavenProjects;
  }

  public void setLocation(File location) {
    this.location = location;
  }

  public void setWorkspaceLocation(boolean workspaceLocation) {
    this.workspaceLocation = workspaceLocation;
  }

  public void setFolders(ISVNRemoteFolder[] folders) {
    this.folders = folders;
  }

  public void setSVNRevision(SVNRevision revision) {
    this.revision = revision;
  }

}
