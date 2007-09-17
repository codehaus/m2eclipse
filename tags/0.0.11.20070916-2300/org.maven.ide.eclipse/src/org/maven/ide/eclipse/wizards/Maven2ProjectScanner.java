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
import java.util.Iterator;
import java.util.List;

import org.apache.maven.model.Model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.embedder.MavenModelManager;

/**
 * @author Eugene Kuleshov
 */
class Maven2ProjectScanner implements IRunnableWithProgress {
  private final File folder;
  private final List projects;

  Maven2ProjectScanner(File folder, List projects) {
    this.folder = folder;
    this.projects = projects;
  }

  public void run(IProgressMonitor monitor) throws InterruptedException {
    monitor.beginTask("Scanning folders", IProgressMonitor.UNKNOWN);
    try {
      scanFolder(this.folder, new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
    } finally {
      monitor.done();
    }
  }

  private void scanFolder(File file, IProgressMonitor monitor) throws InterruptedException {
    if(monitor.isCanceled()) {
      throw new InterruptedException();
    }

    monitor.subTask(file.getAbsolutePath());
    monitor.worked(1);

    if(!file.exists() || !file.isDirectory()) {
      return;
    }

    File pomFile = new File(file, Maven2Plugin.POM_FILE_NAME);
    MavenProjectInfo mavenProjectInfo = readMavenProjectInfo(pomFile);
    if(mavenProjectInfo != null) {
      projects.add(mavenProjectInfo);
      return; // don't scan subfolders of the Maven project
    }

    File[] files = file.listFiles();
    for(int i = 0; i < files.length; i++ ) {
      if(files[i].isDirectory()) {
        scanFolder(files[i], monitor);
      }
    }
  }

  private MavenProjectInfo readMavenProjectInfo(File pomFile) {
    if(!pomFile.exists()) {
      return null;
    }

    MavenModelManager modelManager = Maven2Plugin.getDefault().getMavenModelManager();
    try {
      Model model = modelManager.readMavenModel(pomFile);
      MavenProjectInfo mavenProjectInfo = new MavenProjectInfo(pomFile, model);

      for(Iterator it = model.getModules().iterator(); it.hasNext();) {
        String module = (String) it.next();
        File modulePom = new File(pomFile.getParent(), module + "/" + Maven2Plugin.POM_FILE_NAME);
        MavenProjectInfo moduleMavenProjectInfo = readMavenProjectInfo(modulePom);
        if(moduleMavenProjectInfo != null) {
          mavenProjectInfo.add(moduleMavenProjectInfo);
        }
      }
      return mavenProjectInfo;
    } catch(CoreException ex) {
      Maven2Plugin.getDefault().getConsole().logError("Unable to read model " + pomFile.getAbsolutePath());
    }

    return null;
  }
}
