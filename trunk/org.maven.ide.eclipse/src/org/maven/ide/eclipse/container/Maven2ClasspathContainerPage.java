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

package org.maven.ide.eclipse.container;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.embedder.BuildPathManager;


/**
 * Maven2ClasspathContainerPage
 * 
 * @author Eugene Kuleshov
 */
public class Maven2ClasspathContainerPage extends WizardPage implements IClasspathContainerPage,
    IClasspathContainerPageExtension {
  
  private IJavaProject javaProject;
  private IClasspathEntry containerEntry;

  private Button resolveWorspaceProjectsButton;
  private Button includeModulesButton;
  private boolean resolveWorkspaceProjects;
  private boolean includeModules;

  
  public Maven2ClasspathContainerPage() {
    super("Maven2 Contener");
  }

  public void initialize(IJavaProject javaProject, IClasspathEntry[] currentEntries) {
    this.javaProject = javaProject;
    // this.currentEntries = currentEntries;
  }

  public IClasspathEntry getSelection() {
    return this.containerEntry;
  }

  public void setSelection(IClasspathEntry containerEntry) {
    this.containerEntry = containerEntry == null ? createDefaultEntry() : containerEntry;
  }

  private IClasspathEntry createDefaultEntry() {
    return JavaCore.newContainerEntry(new Path(Maven2Plugin.CONTAINER_ID));
  }

  public void createControl(Composite parent) {
    setTitle("Maven2 Managed Libraries");
    setDescription("Set the configuration details.");

    Composite control = new Composite(parent, SWT.NONE);
    control.setLayout(new GridLayout());

    setControl(control);

    Label label = new Label(control, SWT.NONE);
    label.setText(javaProject.getElementName() + " : " + containerEntry.getPath().toString());

    includeModules = BuildPathManager.isIncludingModules(containerEntry);
    
    includeModulesButton = new Button(control, SWT.CHECK);
    includeModulesButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    includeModulesButton.setText("Include Modules");
    includeModulesButton.setSelection(includeModules);

    resolveWorkspaceProjects = BuildPathManager.isResolvingWorkspaceProjects(containerEntry);

    resolveWorspaceProjectsButton = new Button(control, SWT.CHECK);
    resolveWorspaceProjectsButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    resolveWorspaceProjectsButton.setText("Resolve Worspace Projects");
    resolveWorspaceProjectsButton.setSelection(resolveWorkspaceProjects);
    
    // TODO show/manage container entries
  }

  public boolean finish() {
    boolean newIncludeModules = includeModulesButton.getSelection();
    boolean newResolveWorspaceProjects = resolveWorspaceProjectsButton.getSelection();
    if(newIncludeModules!=includeModules || newResolveWorspaceProjects!=resolveWorkspaceProjects) {
      IPath newPath = new Path(Maven2Plugin.CONTAINER_ID);
      if(newIncludeModules) {
        newPath = newPath.append(Maven2ClasspathContainer.INCLUDE_MODULES);
      }
      if(newResolveWorspaceProjects) {
        newPath = newPath.append(Maven2ClasspathContainer.RESOLVE_WORKSPACE_PROJECTS);
      }
      
      containerEntry = JavaCore.newContainerEntry(newPath, containerEntry.isExported());
    }
    return true;
  }
  
}
