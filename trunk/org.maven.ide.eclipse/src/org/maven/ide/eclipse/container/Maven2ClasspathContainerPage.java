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

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.maven.ide.eclipse.embedder.BuildPathManager;
import org.maven.ide.eclipse.embedder.ResolverConfiguration;


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

  private Text activeProfilesText;

  private ResolverConfiguration resolverConfiguration;

  public Maven2ClasspathContainerPage() {
    super("Maven2 Container");
  }

  // IClasspathContainerPageExtension

  public void initialize(IJavaProject javaProject, IClasspathEntry[] currentEntries) {
    this.javaProject = javaProject;
    // this.currentEntries = currentEntries;
  }

  // IClasspathContainerPage

  public IClasspathEntry getSelection() {
    return this.containerEntry;
  }

  public void setSelection(IClasspathEntry containerEntry) {
    this.containerEntry = containerEntry == null ? BuildPathManager.getDefaultContainerEntry() : containerEntry;
  }

  public void createControl(Composite parent) {
    setTitle("Maven Managed Dependencies");
    setDescription("Set the dependency resolver configuration");

    resolverConfiguration = BuildPathManager.getResolverConfiguration(containerEntry);

    final CTabFolder tabFolder = new CTabFolder(parent, SWT.FLAT);
    tabFolder.setBorderVisible(true);
    tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    tabFolder.setLayout(new FillLayout());
    setControl(tabFolder);

    final CTabItem configurationTabItem = new CTabItem(tabFolder, SWT.NONE);
    configurationTabItem.setText("Resolver Configuration");

    final Composite tabComposite = new Composite(tabFolder, SWT.NONE);
    final GridLayout gridLayout = new GridLayout();
    gridLayout.marginWidth = 10;
    gridLayout.marginHeight = 10;
    tabComposite.setLayout(gridLayout);
    configurationTabItem.setControl(tabComposite);

    Label label = new Label(tabComposite, SWT.NONE);
    label.setBounds(0, 0, 0, 16);
    label.setText("Project: " + javaProject.getElementName());

    includeModulesButton = new Button(tabComposite, SWT.CHECK);
    includeModulesButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    includeModulesButton.setBounds(0, 0, 337, 16);
    includeModulesButton.setText("Include dependencies and source folders from modules");
    includeModulesButton.setSelection(resolverConfiguration.shouldIncludeModules());

    resolveWorspaceProjectsButton = new Button(tabComposite, SWT.CHECK);
    resolveWorspaceProjectsButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    resolveWorspaceProjectsButton.setBounds(0, 0, 288, 16);
    resolveWorspaceProjectsButton.setText("Resolve depndencies from Workspace projects");
    resolveWorspaceProjectsButton.setSelection(resolverConfiguration.shouldResolveWorkspaceProjects());

//    /* uncomment when profiles will be fixed in the embedder
    Label profilesLabel = new Label(tabComposite, SWT.NONE);
    final GridData gd_profilesLabel = new GridData(SWT.LEFT, SWT.CENTER, true, false);
    gd_profilesLabel.verticalIndent = 5;
    profilesLabel.setLayoutData(gd_profilesLabel);
    profilesLabel.setBounds(0, 0, 191, 16);
    profilesLabel.setText("Active Profiles (comma separated):");

    activeProfilesText = new Text(tabComposite, SWT.BORDER);
    activeProfilesText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    activeProfilesText.setBounds(0, 0, 472, 22);
    activeProfilesText.setText(resolverConfiguration.getActiveProfiles());
//    */

    // TODO show/manage container entries
    final CTabItem dependenciesTabItem = new CTabItem(tabFolder, SWT.NONE);
    dependenciesTabItem.setText("Dependencies");
  }

  public boolean finish() {
    boolean newIncludeModules = includeModulesButton.getSelection();
    boolean newResolveWorspaceProjects = resolveWorspaceProjectsButton.getSelection();
    String newProfiles = activeProfilesText.getText();
    if(newIncludeModules != resolverConfiguration.shouldIncludeModules()
        || newResolveWorspaceProjects != resolverConfiguration.shouldResolveWorkspaceProjects()
        || !newProfiles.equals(resolverConfiguration.getActiveProfiles())) {

      ResolverConfiguration newConfiguration = new ResolverConfiguration(newIncludeModules, //
          newResolveWorspaceProjects, newProfiles);
      containerEntry = BuildPathManager.createContainerEntry(newConfiguration);
    }
    return true;
  }

}
