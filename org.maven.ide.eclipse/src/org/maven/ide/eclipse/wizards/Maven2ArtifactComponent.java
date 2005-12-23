package org.maven.ide.eclipse.wizards;

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
  private Text groupIdText;
  private Text artifactIdText;
  private Text versionText;
  private Text descriptionText;
  private Combo packagingCombo;

  
  public Maven2ArtifactComponent(Composite parent, int styles, ModifyListener modifyingListener) {
    super(parent, styles);

    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 2;

    Group artifactGroup = new Group(this, SWT.NONE);
    artifactGroup.setText("Artifact");
    artifactGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 3, 1));
    artifactGroup.setLayout(gridLayout);

    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    setLayout(layout);
    
    Label groupIdlabel = new Label(artifactGroup, SWT.NONE);
    groupIdlabel.setText("Group Id:");

    groupIdText = new Text(artifactGroup, SWT.BORDER);
    groupIdText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
    groupIdText.addModifyListener(modifyingListener);

    Label artifactIdLabel = new Label(artifactGroup, SWT.NONE);
    artifactIdLabel.setText("Artifact Id:");

    artifactIdText = new Text(artifactGroup, SWT.BORDER);
    artifactIdText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
    artifactIdText.addModifyListener(modifyingListener);

    Label versionLabel = new Label(artifactGroup, SWT.NONE);
    versionLabel.setText("Version:");

    versionText = new Text(artifactGroup, SWT.BORDER);
    versionText.setLayoutData(new GridData(50, SWT.DEFAULT));
    versionText.addModifyListener(modifyingListener);

    Label packagingLabel = new Label(artifactGroup, SWT.NONE);
    packagingLabel.setText("Packaging:");

    packagingCombo = new Combo(artifactGroup, SWT.NONE);
    packagingCombo.setItems(new String[] {"jar", "war", "ear", "rar"});
    packagingCombo.setLayoutData(new GridData(50, SWT.DEFAULT));
    packagingCombo.addModifyListener(modifyingListener);

    Label descriptionLabel = new Label(artifactGroup, SWT.NONE);
    descriptionLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false));
    descriptionLabel.setText("Description:");

    descriptionText = new Text(artifactGroup, SWT.BORDER);
    descriptionText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, true));
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
    this.artifactIdText.setText(groupId);
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
