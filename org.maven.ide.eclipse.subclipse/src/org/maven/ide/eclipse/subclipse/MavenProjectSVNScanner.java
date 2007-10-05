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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import org.apache.maven.model.Model;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.tigris.subversion.subclipse.core.ISVNFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.ISVNResource;

import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.wizards.AbstractProjectScanner;
import org.maven.ide.eclipse.wizards.MavenProjectInfo;


public class MavenProjectSVNScanner extends AbstractProjectScanner {

  private final File baseLocation;

  private final ISVNRemoteFolder[] folders;

  private final MavenModelManager modelManager;

  MavenProjectSVNScanner(File baseLocation, ISVNRemoteFolder[] folders, MavenModelManager modelManager) {
    this.baseLocation = baseLocation;
    this.folders = folders;
    this.modelManager = modelManager;
  }

  public String getDescription() {
    if(folders.length > 1) {
      return folders.length + "projects";
    } else {
      return folders[0].getUrl().toString();
    }
  }

  public void run(IProgressMonitor monitor) throws InterruptedException, InvocationTargetException {
    for(int i = 0; i < folders.length; i++ ) {
      if(monitor.isCanceled()) {
        throw new InterruptedException();
      }

      try {
        MavenProjectInfo mavenProjectInfo = readMavenProjectInfo(monitor, folders[i], null);
        if(mavenProjectInfo != null) {
          addProject(mavenProjectInfo);
        }
      } catch(CoreException ex) {
        throw new InvocationTargetException(ex);
      }
    }
  }

  private MavenProjectInfo readMavenProjectInfo(IProgressMonitor monitor, ISVNRemoteFolder remoteFolder,
      MavenProjectInfo parentInfo) throws CoreException {
    if(monitor.isCanceled()) {
      return null;
    }

    ISVNRepositoryLocation repository = remoteFolder.getRepository();
    String folderPath = remoteFolder.getRepositoryRelativePath();

    monitor.subTask("Reading " + folderPath);
    monitor.worked(1);

    ISVNResource[] members = remoteFolder.members(monitor, ISVNFolder.FILE_MEMBERS | ISVNFolder.EXISTING_MEMBERS);
    ISVNRemoteFile remotePomFile = null;
    for(int i = 0; i < members.length; i++ ) {
      if(members[i].getName().equals(Maven2Plugin.POM_FILE_NAME)) {
        remotePomFile = (ISVNRemoteFile) members[i];
        break;
      }
    }
    if(remotePomFile == null) {
      throw new CoreException(new Status(IStatus.ERROR, MavenSubclipsePlugin.PLUGIN_ID, 0, //
          "Folder " + remoteFolder.getRepositoryRelativePath() + " don't have Maven project", null));
    }

    IStorage storage = remotePomFile.getStorage(monitor);
    InputStream is = storage.getContents();

    String label = remoteFolder.getName() + "/" + Maven2Plugin.POM_FILE_NAME;
    Model model = modelManager.readMavenModel(new BufferedReader(new InputStreamReader(is)));
    MavenProjectInfo projectInfo = new MavenProjectSVNInfo(label, null, model, remoteFolder, parentInfo);

    for(Iterator it = model.getModules().iterator(); it.hasNext();) {
      String module = (String) it.next();

      ISVNRemoteFolder moduleFolder = repository.getRemoteFolder(folderPath + "/" + module);
      MavenProjectInfo moduleInfo = readMavenProjectInfo(monitor, moduleFolder, projectInfo);
      if(moduleInfo != null) {
        projectInfo.add(moduleInfo);
      }
    }

    return projectInfo;
  }

}
