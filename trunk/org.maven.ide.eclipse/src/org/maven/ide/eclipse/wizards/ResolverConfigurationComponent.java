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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.maven.ide.eclipse.Messages;
import org.maven.ide.eclipse.embedder.ResolverConfiguration;


/**
 * A foldable resolver configuration panel
 */
public class ResolverConfigurationComponent extends ExpandableComposite {

  /** The resolver configuration */
  private final ResolverConfiguration resolverConfiguration;
  
  Button resolveWorkspaceProjects;
  Button projectsForModules;
  Text profiles;

  /** Creates a new component. */
  public ResolverConfigurationComponent(final Composite parent, final ResolverConfiguration resolverConfiguration) {
    super(parent, ExpandableComposite.COMPACT | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED);

    this.resolverConfiguration = resolverConfiguration;

    setText(Messages.getString("resolverConfiguration.advanced"));

    Composite advancedComposite = new Composite(this, SWT.NONE);
    setClient(advancedComposite);
    addExpansionListener(new ExpansionAdapter() {
      public void expansionStateChanged(ExpansionEvent e) {
        parent.getShell().pack();
        parent.layout();
      }
    });

    GridLayout gridLayout = new GridLayout();
    gridLayout.marginLeft = 11;
    gridLayout.numColumns = 2;
    advancedComposite.setLayout(gridLayout);

    resolveWorkspaceProjects = new Button(advancedComposite, SWT.CHECK);
    resolveWorkspaceProjects.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    resolveWorkspaceProjects.setText(Messages.getString("resolverConfiguration.resolveWorkspaceProjects"));
    resolveWorkspaceProjects.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        resolverConfiguration.setResolveWorkspaceProjects(resolveWorkspaceProjects.getSelection());
      }
    });

    projectsForModules = new Button(advancedComposite, SWT.CHECK);
    projectsForModules.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    projectsForModules.setText(Messages.getString("resolverConfiguration.projectsForModules"));
    projectsForModules.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        resolverConfiguration.setIncludeModules(!projectsForModules.getSelection());
      }
    });

    Label profilesLabel = new Label(advancedComposite, SWT.NONE);
    profilesLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true));
    profilesLabel.setText(Messages.getString("resolverConfiguration.profiles"));

    profiles = new Text(advancedComposite, SWT.BORDER);
    profiles.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    profiles.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        resolverConfiguration.setActiveProfiles(profiles.getText());
      }
    });

    loadData();
  }

  public void loadData() {
    resolveWorkspaceProjects.setSelection(resolverConfiguration.shouldResolveWorkspaceProjects());
    projectsForModules.setSelection(!resolverConfiguration.shouldIncludeModules());
    profiles.setText(resolverConfiguration.getActiveProfiles());
  }

  public ResolverConfiguration getResolverConfiguration() {
    return this.resolverConfiguration;
  }
  
}
