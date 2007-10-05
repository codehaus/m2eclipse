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
import java.text.ParseException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * @author Eugene Kuleshov
 */
public class MavenCheckoutLocationPage extends WizardPage {

  private Button useDefaultWorkspaceLocationButton;
  private Label locationLabel;
  private Text locationText;

  private Button headRevisionButton;
  private Button revisionButton;
  private Text revisionText;
  private Button revisionBrowseButton;
  
  protected MavenCheckoutLocationPage() {
    super("Project Location");
    setTitle("Project Location");
    setDescription("Select project location");
  }

  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    final GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 2;
    composite.setLayout(gridLayout);

    setControl(composite);

    SelectionAdapter selectionAdapter = new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        updateSelection();
      }
    };
    
    useDefaultWorkspaceLocationButton = new Button(composite, SWT.CHECK);
    useDefaultWorkspaceLocationButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    useDefaultWorkspaceLocationButton.setText("Use default workspace location");
    useDefaultWorkspaceLocationButton.addSelectionListener(selectionAdapter);
    useDefaultWorkspaceLocationButton.setSelection(true);

    locationLabel = new Label(composite, SWT.NONE);
    locationLabel.setText("Location:");

    locationText = new Text(composite, SWT.BORDER);
    final GridData gd_locationText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    locationText.setLayoutData(gd_locationText);

    final Group revisionToCheckGroup = new Group(composite, SWT.NONE);
    revisionToCheckGroup.setText("Revision to check out");
    revisionToCheckGroup.setLayout(new GridLayout(3, false));
    final GridData revisionToCheckGroupData = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
    revisionToCheckGroupData.verticalIndent = 10;
    revisionToCheckGroup.setLayoutData(revisionToCheckGroupData);

    headRevisionButton = new Button(revisionToCheckGroup, SWT.RADIO);
    headRevisionButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
    headRevisionButton.setText("Head Revision");
    headRevisionButton.setSelection(true);

    revisionButton = new Button(revisionToCheckGroup, SWT.RADIO);
    revisionButton.setText("Revision");
    revisionButton.addSelectionListener(selectionAdapter);

    revisionText = new Text(revisionToCheckGroup, SWT.BORDER);
    revisionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

    revisionBrowseButton = new Button(revisionToCheckGroup, SWT.NONE);
    revisionBrowseButton.setText("Browse...");
    
    updateSelection();
  }

  private void updateSelection() {
    boolean defaultWorkspaceLocation = isDefaultWorkspaceLocation(); 
    locationLabel.setEnabled(!defaultWorkspaceLocation);
    locationText.setEnabled(!defaultWorkspaceLocation);
    
    boolean selectRevision = revisionButton.getSelection();
    revisionText.setEnabled(selectRevision);
    revisionBrowseButton.setEnabled(selectRevision);
  }

  public boolean isDefaultWorkspaceLocation() {
    return useDefaultWorkspaceLocationButton.getSelection();
  }
  
  public File getLocation() {
    if(isDefaultWorkspaceLocation()) {
      return ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
    } else {
      return new File(locationText.getText());
    }
  }
  
  public boolean isHeadRevision() {
    return !revisionButton.getSelection();
  }
  
  public SVNRevision getRevision() {
    if(isHeadRevision()) {
      return SVNRevision.HEAD;
    }
    SVNRevision revision = null;
    try {
      revision = SVNRevision.getRevision(revisionText.getText().trim());
    } catch (ParseException e) {
    }
    return revision==null ? SVNRevision.HEAD : revision;
  }

}

