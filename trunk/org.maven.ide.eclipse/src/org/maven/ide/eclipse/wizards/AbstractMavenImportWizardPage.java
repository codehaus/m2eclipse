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

package org.maven.ide.eclipse.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.maven.ide.eclipse.embedder.ResolverConfiguration;

/**
 * AbstractMavenImportWizardPage
 *
 * @author Eugene Kuleshov
 */
public abstract class AbstractMavenImportWizardPage extends WizardPage {

  protected Button projectsForModules;
  protected Button resolveWorkspaceProjects;
  protected Text activeProfiles;

  protected AbstractMavenImportWizardPage(String pageName) {
    super(pageName);
  }

  protected void createAdvancedSettings(final Composite composite, GridData gridData) {
    FormToolkit toolkit = new FormToolkit(Display.getCurrent());
  
    ExpandableComposite expandable = toolkit.createExpandableComposite(composite, ExpandableComposite.COMPACT | ExpandableComposite.TWISTIE);
    expandable.clientVerticalSpacing = 1;
    expandable.setLayoutData(gridData);
    expandable.setText("Ad&vanced");
    expandable.setFont(composite.getFont());
    expandable.setBackground(composite.getBackground());
  
    toolkit.paintBordersFor(expandable);
  
    final Composite advancedComposite = toolkit.createComposite(expandable, SWT.NONE);
    expandable.setClient(advancedComposite);
    expandable.addExpansionListener(new ExpansionAdapter() {
      public void expansionStateChanged(ExpansionEvent e) {
        getControl().getShell().pack();
        composite.layout();
      }
    });
  
    final GridLayout gridLayout = new GridLayout();
    gridLayout.marginLeft = 10;
    gridLayout.numColumns = 2;
    advancedComposite.setLayout(gridLayout);
    advancedComposite.setBackground(composite.getBackground());
    // toolkit.paintBordersFor(advancedComposite);
  
    resolveWorkspaceProjects = new Button(advancedComposite, SWT.CHECK);
    resolveWorkspaceProjects.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    resolveWorkspaceProjects.setText("&Resolve workspace projects");
    resolveWorkspaceProjects.setSelection(true);
  
    projectsForModules = new Button(advancedComposite, SWT.CHECK);
    projectsForModules.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    projectsForModules.setText("&Separate projects for modules");
    projectsForModules.setSelection(true);
  
    final Label profilesLabel = new Label(advancedComposite, SWT.NONE);
    profilesLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true));
    profilesLabel.setText("Pr&ofiles:");
  
    activeProfiles = new Text(advancedComposite, SWT.BORDER);
    activeProfiles.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
  }

  public boolean createProjectsForModules() {
    return this.projectsForModules.getSelection();
  }

  public boolean resolveWorkspaceProjects() {
    return this.resolveWorkspaceProjects.getSelection();
  }

  public String getActiveProfiles() {
    return this.activeProfiles.getText();
  }

  public ResolverConfiguration getResolverConfiguration() {
    boolean includeModules = !createProjectsForModules();
    boolean resolveWorkspaceProjects = resolveWorkspaceProjects();
    return new ResolverConfiguration(includeModules, resolveWorkspaceProjects, getActiveProfiles());
  }

}
