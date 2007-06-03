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

package org.maven.ide.eclipse.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.maven.ide.eclipse.Maven2Plugin;

/**
 * Helper IPropertyTester implementation to check if receiver can be launched with Maven.
 * E.g. it is pom.xml file of folder or project that has pom.xml. 
 *
 * @author Eugene Kuleshov
 */
public class MavenPropertyTester extends PropertyTester {

  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    IAdaptable adaptable = (IAdaptable) receiver;
    
    IProject projectAdapter = (IProject) adaptable.getAdapter(IProject.class);
    if(projectAdapter!=null) {
      return projectAdapter.getFile(Maven2Plugin.POM_FILE_NAME).exists();
    }
    
    IFolder folderAdapter = (IFolder) adaptable.getAdapter(IFolder.class);
    if(folderAdapter!=null) {
      return folderAdapter.getFile(Maven2Plugin.POM_FILE_NAME).exists();
    }

    IFile fileAdapter = (IFile) adaptable.getAdapter(IFile.class);
    if(fileAdapter!=null) {
      return fileAdapter.exists() && Maven2Plugin.POM_FILE_NAME.equals(fileAdapter.getName());
    }
    
    return false;
  }

}

