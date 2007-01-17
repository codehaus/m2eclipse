package org.maven.ide.eclipse.wizards;

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

import org.apache.maven.model.Model;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;


public class Maven2ArtifactComponent extends Composite {
  public static final String DEFAULT_PACKAGING = "jar";
  public static final String DEFAULT_VERSION = "0.0.1";
  
  private Text groupIdText;
  private Text artifactIdText;
  private Text versionText;
  private Text descriptionText;
  private Combo packagingCombo;
  
  private ModifyListener modifyingListener;

  
  public Maven2ArtifactComponent(Composite parent, int styles) {
    super(parent, styles);

    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 2;

    Group artifactGroup = new Group(this, SWT.NONE);
    artifactGroup.setText("Artifact");
    artifactGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 3, 1));
    artifactGroup.setLayout(gridLayout);

    GridLayout layout = new GridLayout();
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    layout.numColumns = 2;
    setLayout(layout);
    
    Label groupIdlabel = new Label(artifactGroup, SWT.NONE);
    groupIdlabel.setText("Group Id:");

    groupIdText = new Text(artifactGroup, SWT.BORDER);
    groupIdText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));

    Label artifactIdLabel = new Label(artifactGroup, SWT.NONE);
    artifactIdLabel.setText("Artifact Id:");

    artifactIdText = new Text(artifactGroup, SWT.BORDER);
    artifactIdText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));

    Label versionLabel = new Label(artifactGroup, SWT.NONE);
    versionLabel.setText("Version:");

    versionText = new Text(artifactGroup, SWT.BORDER);
    versionText.setLayoutData(new GridData(50, SWT.DEFAULT));

    Label packagingLabel = new Label(artifactGroup, SWT.NONE);
    packagingLabel.setText("Packaging:");

    packagingCombo = new Combo(artifactGroup, SWT.NONE);
    packagingCombo.setItems(new String[] {"jar", "war", "ear", "rar"});
    packagingCombo.setLayoutData(new GridData(50, SWT.DEFAULT));

    Label descriptionLabel = new Label(artifactGroup, SWT.NONE);
    descriptionLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false));
    descriptionLabel.setText("Description:");

    descriptionText = new Text(artifactGroup, SWT.BORDER);
    descriptionText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, true));
  }
  
  public void setModifyingListener( ModifyListener modifyingListener ) {
    this.modifyingListener = modifyingListener;

    groupIdText.addModifyListener(modifyingListener);
    artifactIdText.addModifyListener(modifyingListener);
    versionText.addModifyListener(modifyingListener);
    packagingCombo.addModifyListener(modifyingListener);
  }
  
  public void dispose() {
    super.dispose();
   
    if(modifyingListener!=null) {
      groupIdText.removeModifyListener( modifyingListener );
      artifactIdText.removeModifyListener(modifyingListener);
      versionText.removeModifyListener(modifyingListener);
      packagingCombo.removeModifyListener(modifyingListener);
    }
  }

  public String getArtifactId() {
    return this.artifactIdText.getText();
  }
  
  public String getGroupId() {
    return this.groupIdText.getText();
  }
  
  public String getVersion() {
    return this.versionText.getText();
  }
  
  public String getPackaging() {
    return this.packagingCombo.getText();
  }
  
  public String getDescription() {
    return this.descriptionText.getText();
  }
  
  public void setGroupId( String groupId) {
    this.groupIdText.setText(groupId);
  }
  
  public void setArtifactId( String artifact) {
    this.artifactIdText.setText(artifact);
  }
  
  public void setVersion( String version) {
    versionText.setText(version);
  }

  public void setPackaging( String packaging) {
    packagingCombo.setText(packaging);
  }
  
  public void setDescription( String description) {
    descriptionText.setText(description);
  }
  
  public Model getModel() {
    Model model = new Model();
    model.setModelVersion( "4.0.0");
    model.setGroupId( getGroupId());
    model.setArtifactId( getArtifactId());
    model.setVersion( getVersion());
    model.setPackaging( getPackaging());
    model.setDescription( getDescription());
    return model;
  }
  
}

