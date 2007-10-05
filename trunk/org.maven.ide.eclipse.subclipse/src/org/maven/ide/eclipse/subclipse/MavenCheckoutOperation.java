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
import org.tigris.subversion.subclipse.ui.operations.CheckoutAsProjectOperation;
import org.tigris.subversion.svnclientadapter.SVNRevision;

import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.embedder.BuildPathManager;
import org.maven.ide.eclipse.embedder.ResolverConfiguration;


/**
 * Checkout operation
 * 
 * @author Eugene Kuleshov
 */
public class MavenCheckoutOperation implements IRunnableWithProgress {

  private ResolverConfiguration configuration;

  private List mavenProjects;

  private SVNRevision svnRevision;

  private File location;

  private boolean workspaceLocation;

  public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

    List remoteFolderList = new ArrayList();
    List localFolderList = new ArrayList();

    // sort nested projects
    for(Iterator it = mavenProjects.iterator(); it.hasNext();) {
      MavenProjectSVNInfo info = (MavenProjectSVNInfo) it.next();
      String remoteFolderPath = info.getRemoteFolder().getRepositoryRelativePath();

      boolean isNestedPath = false;
      for(Iterator it2 = mavenProjects.iterator(); it2.hasNext();) {
        MavenProjectSVNInfo info2 = (MavenProjectSVNInfo) it2.next();
        if(info != info2) {
          String path = info2.getRemoteFolder().getRepositoryRelativePath();
          if(remoteFolderPath.startsWith(path)) {
            isNestedPath = true;
            break;
          }
        }
      }
      if(!isNestedPath) {
        remoteFolderList.add(info.getRemoteFolder());
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
      operation.setSvnRevision(svnRevision);
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

      ISVNRemoteFolder remoteFolder = info.getRemoteFolder();
      String remoteFolderPath = remoteFolder.getRepositoryRelativePath();

      try {
        int n = remoteFolderList.indexOf(remoteFolder);
        if(n > -1) {
          // project is already in workspace
          buildpathManager.configureProject((IProject) localFolderList.get(n), configuration, monitor);

        } else {
          // module project that need to e imported
          File pomFile = findPomFile(remoteFolderPath, remoteFolderList, localFolderList);
          if(pomFile == null) {
            Maven2Plugin.getDefault().getConsole().logError("Can't find POM file for " + remoteFolderPath);
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

  private File findPomFile(String remoteFolderPath, List remoteFolderList, List localFolderList) {
    for(int i = 0; i < remoteFolderList.size(); i++ ) {
      ISVNRemoteFolder folder = (ISVNRemoteFolder) remoteFolderList.get(i);
      String path = folder.getRepositoryRelativePath();
      if(remoteFolderPath.startsWith(path)) {
        IProject parentProject = (IProject) localFolderList.get(i);
        File parentFolder = parentProject.getLocation().toFile();
        return new File(parentFolder, remoteFolderPath.substring(path.length()) + File.separator
            + Maven2Plugin.POM_FILE_NAME);
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

  public void setSVNRevision(SVNRevision svnRevision) {
    this.svnRevision = svnRevision;
  }

  public void setLocation(File location) {
    this.location = location;
  }

  public void setWorkspaceLocation(boolean workspaceLocation) {
    this.workspaceLocation = workspaceLocation;
  }

}
