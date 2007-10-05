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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import org.apache.maven.model.Model;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.wizards.AbstractProjectScanner;
import org.maven.ide.eclipse.wizards.MavenProjectInfo;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;

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
    if(folders.length>1) {
      return folders.length + "projects";
    } else {
      return folders[0].getUrl().toString();  
    }
  }

  public void run(IProgressMonitor monitor) throws InterruptedException {

    for(int i = 0; i < folders.length; i++ ) {
      if(monitor.isCanceled()) {
        break;
      }

      MavenProjectInfo mavenProjectInfo = readMavenProjectInfo(monitor, folders[i], null);
      if(mavenProjectInfo!=null) {
        addProject(mavenProjectInfo);
      }
    }
  }

  private MavenProjectInfo readMavenProjectInfo(IProgressMonitor monitor, ISVNRemoteFolder remoteFolder, MavenProjectInfo parentInfo) {
//        ISVNResource[] members = folder.members(monitor, ISVNRemoteFolder.FILE_MEMBERS);
//        for(int j = 0; j < members.length; j++ ) {
//          ISVNResource resource = members[j];
//          if(resource.getName().equals("pom.xml")) {
//            
//            ISVNRemoteFile remoteFile = resource.getRepository().getRemoteFile(resourceUrl);

    if(monitor.isCanceled()) {
      return null;
    }
    
    ISVNRepositoryLocation repository = remoteFolder.getRepository();
    String folderPath = remoteFolder.getRepositoryRelativePath();

    monitor.subTask("Reading " + folderPath);
    monitor.worked(1);

    try {
      ISVNRemoteFile remotePomFile = repository.getRemoteFile(folderPath + "/" + Maven2Plugin.POM_FILE_NAME);
      
      IStorage storage = remotePomFile.getStorage(monitor);
      InputStream is = storage.getContents();
      
      Model model = modelManager.readMavenModel(new InputStreamReader(is));

      String label = remoteFolder.getName() + "/" + Maven2Plugin.POM_FILE_NAME;
      File pomFile = null;  // TODO calculate location from the root
      
      MavenProjectInfo projectInfo = new MavenProjectSVNInfo(label, pomFile, model, remoteFolder, parentInfo);
      
      for(Iterator it = model.getModules().iterator(); it.hasNext();) {
        String module = (String) it.next();
      
        ISVNRemoteFolder moduleFolder = repository.getRemoteFolder(folderPath + "/" + module);
        MavenProjectInfo moduleInfo = readMavenProjectInfo(monitor, moduleFolder, projectInfo); 
        if(moduleInfo != null) {
          projectInfo.add(moduleInfo);
        }
      }
      
      return projectInfo;
      
    } catch(CoreException ex) {
      // TODO Auto-generated catch block
      ex.printStackTrace();
      
    }
    
    return null;
  }

}
