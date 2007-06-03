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

package org.maven.ide.eclipse.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.descriptor.MojoDescriptor;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.Messages;
import org.maven.ide.eclipse.util.ITraceable;
import org.maven.ide.eclipse.util.Tracer;
import org.maven.ide.eclipse.util.Util;


/**
 * Maven Launch dialog Main tab 
 * 
 * @author Dmitri Maximovich
 * @author Eugene Kuleshov
 */
public class Maven2LaunchMainTab extends AbstractLaunchConfigurationTab implements Maven2LaunchConstants, ITraceable {

  private static final boolean TRACE_ENABLED = Boolean.valueOf(Platform.getDebugOption("org.maven.ide.eclipse/launcher")).booleanValue();

  public static final String ID_EXTERNAL_TOOLS_LAUNCH_GROUP = "org.eclipse.ui.externaltools.launchGroup"; //$NON-NLS-1$

  private final boolean isBuilder;
  
  protected Text pomDirNameText;

  protected Text goalsText;
  protected Text goalsAutoBuildText;
  protected Text goalsManualBuildText;
  protected Text goalsCleanText;
  protected Text goalsAfterCleanText;
  
  protected Text profilesText;
  protected Table propsTable;

  public Maven2LaunchMainTab(boolean isBuilder) {
    this.isBuilder = isBuilder;
  }

  public boolean isTraceEnabled() {
    return TRACE_ENABLED;
  }

  public Image getImage() {
    return Maven2Plugin.getImage("icons/main_tab.gif");
  }

  public void createControl(Composite parent) {
    Composite mainComposite = new Composite(parent, SWT.NONE);
    setControl(mainComposite);
    //PlatformUI.getWorkbench().getHelpSystem().setHelp(mainComposite, IAntUIHelpContextIds.ANT_MAIN_TAB);
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    mainComposite.setLayout(layout);
    mainComposite.setLayoutData(gridData);
    mainComposite.setFont(parent.getFont());

    ModifyListener modyfyingListener = new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        entriesChanged();
      }
    };
    
    // pom file 
    final Group pomGroup = new Group(mainComposite, SWT.NONE);
    pomGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
    pomGroup.setText(Messages.getString("launch.pomGroup")); //$NON-NLS-1$
    pomGroup.setLayout(new GridLayout());

    this.pomDirNameText = new Text(pomGroup, SWT.BORDER);
    this.pomDirNameText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
    this.pomDirNameText.addModifyListener(modyfyingListener);

    final Composite pomDirButtonsComposite = new Composite(pomGroup, SWT.NONE);
    pomDirButtonsComposite.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
    final GridLayout pomDirButtonsGridLayout = new GridLayout();
    pomDirButtonsGridLayout.marginWidth = 0;
    pomDirButtonsGridLayout.marginHeight = 0;
    pomDirButtonsGridLayout.numColumns = 3;
    pomDirButtonsComposite.setLayout(pomDirButtonsGridLayout);

    final Button browseWorkspaceButton = new Button(pomDirButtonsComposite, SWT.NONE);
    browseWorkspaceButton.setText(Messages.getString("launch.browseWorkspace")); //$NON-NLS-1$
    browseWorkspaceButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), ResourcesPlugin.getWorkspace()
            .getRoot(), false, Messages.getString("launch.choosePomDir")); //$NON-NLS-1$
        int buttonId = dialog.open();
        if(buttonId == IDialogConstants.OK_ID) {
          Object[] resource = dialog.getResult();
          if(resource != null && resource.length > 0) {
            String fileLoc = VariablesPlugin.getDefault().getStringVariableManager().generateVariableExpression(
                "workspace_loc", ((IPath) resource[0]).toString()); //$NON-NLS-1$
            pomDirNameText.setText(fileLoc);
            entriesChanged();
          }
        }
      }
    });

    final Button browseFilesystemButton = new Button(pomDirButtonsComposite, SWT.NONE);
    browseFilesystemButton.setText(Messages.getString("launch.browseFs")); //$NON-NLS-1$
    browseFilesystemButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.NONE);
        dialog.setFilterPath(pomDirNameText.getText());
        String text = dialog.open();
        if(text != null) {
          pomDirNameText.setText(text);
          entriesChanged();
        }
      }
    });

    final Button browseVariablesButton = new Button(pomDirButtonsComposite, SWT.NONE);
    browseVariablesButton.setText(Messages.getString("launch.browseVariables")); //$NON-NLS-1$
    browseVariablesButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
        dialog.open();
        String variable = dialog.getVariableExpression();
        if(variable != null) {
          pomDirNameText.insert(variable);
        }
      }
    });

    // goals
    
    if(isBuilder) {
      Label autoBuildGoalsLabel = new Label(mainComposite, SWT.NONE);
      autoBuildGoalsLabel.setText("&Auto Build Goals:");
      goalsAutoBuildText = new Text(mainComposite, SWT.BORDER);
      goalsAutoBuildText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      goalsAutoBuildText.addModifyListener(modyfyingListener);
      goalsAutoBuildText.addFocusListener(new GoalsFocusListener(goalsAutoBuildText));
      Button goalsAutoBuildButton = new Button(mainComposite, SWT.NONE);
      goalsAutoBuildButton.setLayoutData(new GridData());
      goalsAutoBuildButton.setText("&Select...");
      goalsAutoBuildButton.addSelectionListener(new GoalSelectionAdapter(goalsAutoBuildText));

      Label manualBuildGoalsLabel = new Label(mainComposite, SWT.NONE);
      manualBuildGoalsLabel.setText("&Manual Build Goals:");
      goalsManualBuildText = new Text(mainComposite, SWT.BORDER);
      goalsManualBuildText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      goalsManualBuildText.addModifyListener(modyfyingListener);
      goalsManualBuildText.addFocusListener(new GoalsFocusListener(goalsManualBuildText));
      Button goalsManualBuildButton = new Button(mainComposite, SWT.NONE);
      goalsManualBuildButton.setLayoutData(new GridData());
      goalsManualBuildButton.setText("S&elect...");
      goalsManualBuildButton.addSelectionListener(new GoalSelectionAdapter(goalsManualBuildText));
      
      Label cleanBuildGoalsLabel = new Label(mainComposite, SWT.NONE);
      cleanBuildGoalsLabel.setText("&During a Clean Goals:");
      goalsCleanText = new Text(mainComposite, SWT.BORDER);
      goalsCleanText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      goalsCleanText.addModifyListener(modyfyingListener);
      goalsCleanText.addFocusListener(new GoalsFocusListener(goalsCleanText));
      Button goalsCleanButton = new Button(mainComposite, SWT.NONE);
      goalsCleanButton.setLayoutData(new GridData());
      goalsCleanButton.setText("Se&lect...");
      goalsCleanButton.addSelectionListener(new GoalSelectionAdapter(goalsCleanText));
      
      Label afterCleanGoalsLabel = new Label(mainComposite, SWT.NONE);
      afterCleanGoalsLabel.setText("&After a Clean Goals:");
      goalsAfterCleanText = new Text(mainComposite, SWT.BORDER);
      goalsAfterCleanText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      goalsAfterCleanText.addModifyListener(modyfyingListener);
      goalsAfterCleanText.addFocusListener(new GoalsFocusListener(goalsAfterCleanText));
      Button goalsAfterCleanButton = new Button(mainComposite, SWT.NONE);
      goalsAfterCleanButton.setLayoutData(new GridData());
      goalsAfterCleanButton.setText("Selec&t...");
      goalsAfterCleanButton.addSelectionListener(new GoalSelectionAdapter(goalsAfterCleanText));
      
      
    } else {
      Label goalsLabel = new Label(mainComposite, SWT.NONE);
      goalsLabel.setText(Messages.getString("launch.goalsLabel")); //$NON-NLS-1$
      goalsText = new Text(mainComposite, SWT.BORDER);
      goalsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      goalsText.addModifyListener(modyfyingListener);
      goalsText.addFocusListener(new GoalsFocusListener(goalsText));
      Button selectGoalsButton = new Button(mainComposite, SWT.NONE);
      selectGoalsButton.setLayoutData(new GridData());
      selectGoalsButton.setText(Messages.getString("launch.goals")); //$NON-NLS-1$
      selectGoalsButton.addSelectionListener(new GoalSelectionAdapter(goalsText));
    }

    Label profilesLabel = new Label(mainComposite, SWT.NONE);
    profilesLabel.setText(Messages.getString("launch.profilesLabel")); //$NON-NLS-1$
    // profilesLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

    profilesText = new Text(mainComposite, SWT.BORDER);
    profilesText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    profilesText.addModifyListener(modyfyingListener);

    // properties
    final Composite propsComposite = new Composite(mainComposite, SWT.NONE);
    propsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
    final GridLayout propsGridLayout = new GridLayout();
    propsGridLayout.marginWidth = 0;
    propsGridLayout.horizontalSpacing = 0;
    propsGridLayout.numColumns = 2;
    propsComposite.setLayout(propsGridLayout);

    this.propsTable = new Table(propsComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
    //this.tProps.setItemCount(10);
    this.propsTable.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
    this.propsTable.setLinesVisible(true);
    this.propsTable.setHeaderVisible(true);
    TableViewer tableViewer = new TableViewer(this.propsTable);
    tableViewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        TableItem[] selection = propsTable.getSelection();
        if(selection.length == 1) {
          editProperty(selection[0].getText(0), selection[0].getText(1));
        }
      }
    });

    final TableColumn propColumn = new TableColumn(this.propsTable, SWT.NONE, 0);
    propColumn.setWidth(100);
    propColumn.setText(Messages.getString("launch.propName")); //$NON-NLS-1$

    final TableColumn valueColumn = new TableColumn(this.propsTable, SWT.NONE, 1);
    valueColumn.setWidth(200);
    valueColumn.setText(Messages.getString("launch.propValue")); //$NON-NLS-1$

    final Composite propsButtonsComposite = new Composite(propsComposite, SWT.NONE);
    propsButtonsComposite.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false));
    final GridLayout propsButtonsGridLayout = new GridLayout();
    propsButtonsGridLayout.marginHeight = 0;
    propsButtonsGridLayout.marginLeft = 5;
    propsButtonsGridLayout.marginWidth = 0;
    propsButtonsComposite.setLayout(propsButtonsGridLayout);

    final Button addPropButton = new Button(propsButtonsComposite, SWT.NONE);
    addPropButton.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
    addPropButton.setText(Messages.getString("launch.propAddButton")); //$NON-NLS-1$
    addPropButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        addProperty();
      }
    });

    final Button removePropButton = new Button(propsButtonsComposite, SWT.NONE);
    removePropButton.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
    removePropButton.setText(Messages.getString("launch.propRemoveButton")); //$NON-NLS-1$
    removePropButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if(propsTable.getSelectionCount() > 0) {
          propsTable.remove(propsTable.getSelectionIndices());
          entriesChanged();
        }
      }
    });
  }

  void addProperty() {
    Maven2PropertyAddDialog dialog = new Maven2PropertyAddDialog(getShell(), Messages
        .getString("launch.propAddDialogTitle"), new String[] {}); //$NON-NLS-1$
    int buttonId = dialog.open();

    if(buttonId == IDialogConstants.OK_ID) {
      String[] result = dialog.getNameValuePair();
      TableItem item = new TableItem(propsTable, SWT.NONE);
      item.setText(0, result[0]);
      item.setText(1, result[1]);
      entriesChanged();
    }
  }

  void editProperty(String name, String value) {
    Maven2PropertyAddDialog dialog = new Maven2PropertyAddDialog(getShell(), Messages
        .getString("launch.propEditDialogTitle"), new String[] {name, value}); //$NON-NLS-1$
    int buttonId = dialog.open();

    if(buttonId == IDialogConstants.OK_ID) {
      String[] result = dialog.getNameValuePair();
      TableItem[] item = propsTable.getSelection();
      // we expect only one row selected
      item[0].setText(0, result[0]);
      item[0].setText(1, result[1]);
      entriesChanged();
    }
  }

  public void initializeFrom(ILaunchConfiguration configuration) {
    String pomDirName = getAttribute(configuration, ATTR_POM_DIR, ""); //$NON-NLS-1$
    if(isBuilder && pomDirName.length()==0) {
      pomDirName = "${workspace_loc:/" + configuration.getFile().getProject().getName() + "}";
    }
    this.pomDirNameText.setText(pomDirName);
    
    if(isBuilder) {
      this.goalsAutoBuildText.setText(getAttribute(configuration, ATTR_GOALS_AUTO_BUILD, "install")); //$NON-NLS-1$
      this.goalsManualBuildText.setText(getAttribute(configuration, ATTR_GOALS_MANUAL_BUILD, "install")); //$NON-NLS-1$
      this.goalsCleanText.setText(getAttribute(configuration, ATTR_GOALS_CLEAN, "clean")); //$NON-NLS-1$
      this.goalsAfterCleanText.setText(getAttribute(configuration, ATTR_GOALS_AFTER_CLEAN, "install")); //$NON-NLS-1$
    } else {
      this.goalsText.setText(getAttribute(configuration, ATTR_GOALS, "")); //$NON-NLS-1$
    }
    
    this.profilesText.setText(getAttribute(configuration, ATTR_PROFILES, "")); //$NON-NLS-1$

    try {
      propsTable.removeAll();
      List properties = configuration.getAttribute(ATTR_PROPERTIES, Collections.EMPTY_LIST);
      for(Iterator iter = properties.iterator(); iter.hasNext();) {
        String s = (String) iter.next();
        try {
          String[] ss = s.split("="); //$NON-NLS-1$
          TableItem item = new TableItem(propsTable, SWT.NONE);
          item.setText(0, ss[0]);
          if(ss.length > 1) {
            item.setText(1, ss[1]);
          }
        } catch(Exception e) {
          String msg = "Error parsing argument: " + s; //$NON-NLS-1$
          Maven2Plugin.log(msg, e);
        }
      }
    } catch(CoreException ex) {
    }
    setDirty(false);
  }
  
  private String getAttribute(ILaunchConfiguration configuration, String name, String defaultValue) {
    try {
      return configuration.getAttribute(name, defaultValue);
    } catch(CoreException ex) {
      return defaultValue;
    }
  }

  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
  }

  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    configuration.setAttribute(ATTR_POM_DIR, this.pomDirNameText.getText());
    
    if(isBuilder) {
      configuration.setAttribute(ATTR_GOALS_AUTO_BUILD, goalsAutoBuildText.getText());
      configuration.setAttribute(ATTR_GOALS_MANUAL_BUILD, this.goalsManualBuildText.getText());
      configuration.setAttribute(ATTR_GOALS_CLEAN, this.goalsCleanText.getText());
      configuration.setAttribute(ATTR_GOALS_AFTER_CLEAN, this.goalsAfterCleanText.getText());
      
      StringBuffer sb = new StringBuffer();
      if(goalsAfterCleanText.getText().trim().length()>0) {
        sb.append(IExternalToolConstants.BUILD_TYPE_FULL).append(',');
      }
      if(goalsManualBuildText.getText().trim().length()>0) {
        sb.append(IExternalToolConstants.BUILD_TYPE_INCREMENTAL).append(',');
      }
      if(goalsAutoBuildText.getText().trim().length()>0) {
        sb.append(IExternalToolConstants.BUILD_TYPE_AUTO).append(',');
      }
      if(goalsCleanText.getText().trim().length()>0) {
        sb.append(IExternalToolConstants.BUILD_TYPE_CLEAN);
      }
      configuration.setAttribute(IExternalToolConstants.ATTR_RUN_BUILD_KINDS, sb.toString());
      
    } else {
      configuration.setAttribute(ATTR_GOALS, this.goalsText.getText());
    }
    
    configuration.setAttribute(ATTR_PROFILES, this.profilesText.getText());

    TableItem[] items = this.propsTable.getItems();
    // store as String in "param=value" format 
    List properties = new ArrayList();
    for(int i = 0; i < items.length; i++ ) {
      String p = items[i].getText(0);
      String v = items[i].getText(1);
      if(p != null && p.trim().length() > 0) {
        String prop = p.trim() + "=" + (v == null ? "" : v); //$NON-NLS-1$ //$NON-NLS-2$
        properties.add(prop);
        Tracer.trace(this, "property", prop);
      }
    }
    configuration.setAttribute(ATTR_PROPERTIES, properties);
  }

  public String getName() {
    return Messages.getString("launch.mainTabName"); //$NON-NLS-1$
  }

  public boolean isValid(ILaunchConfiguration launchConfig) {
    setErrorMessage(null);

    String pomFileName = this.pomDirNameText.getText();
    if(pomFileName == null || pomFileName.trim().length() == 0) {
      setErrorMessage(Messages.getString("launch.pomDirectoryEmpty"));
      return false;
    }
    if(!isDirectoryExist(pomFileName)) {
      setErrorMessage(Messages.getString("launch.pomDirectoryDoesntExist"));
      return false;
    }
    return true;
  }

  protected boolean isDirectoryExist(String name) {
    if(name == null || name.trim().length() == 0) {
      return false;
    }
    String dirName = Util.substituteVar(name);
    if(dirName == null) {
      return false;
    }
    File pomDir = new File(dirName);
    if(!pomDir.exists()) {
      return false;
    }
    if(!pomDir.isDirectory()) {
      return false;
    }
    return true;
  }

  void entriesChanged() {
    setDirty(true);
    updateLaunchConfigurationDialog();
  }

  
  private final class GoalsFocusListener extends FocusAdapter {
    private Text text;

    public GoalsFocusListener(Text text) {
      this.text = text;
    }
    
    public void focusGained(FocusEvent e) {
      super.focusGained(e);
      text.setData("focus");
    }
  }


  private final class GoalSelectionAdapter extends SelectionAdapter {
    private Text text;

    public GoalSelectionAdapter(Text text) {
      this.text = text;
    }

    public void widgetSelected(SelectionEvent e) {
//        String fileName = Util.substituteVar(fPomDirName.getText());
//        if(!isDirectoryExist(fileName)) {
//          MessageDialog.openError(getShell(), Messages.getString("launch.errorPomMissing"), 
//              Messages.getString("launch.errorSelectPom")); //$NON-NLS-1$ //$NON-NLS-2$
//          return;
//        }
      Maven2GoalSelectionDialog dialog = new Maven2GoalSelectionDialog(getShell());
      int rc = dialog.open();
      if(rc == IDialogConstants.OK_ID) {
        Object[] o = dialog.getResult();
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < o.length; i++ ) {
          if(o[i] instanceof Maven2GoalSelectionDialog.LifecyclePhase) {
            sb.append(((Maven2GoalSelectionDialog.LifecyclePhase) o[i]).getName()).append(' ');
          } else if(o[i] instanceof MojoDescriptor) {
            MojoDescriptor mojoDescriptor = (MojoDescriptor) o[i];
            sb.append(mojoDescriptor.getFullGoalName()).append(' ');
          }
        }
        if(sb.charAt(sb.length() - 1) == ' ') {
          sb.deleteCharAt(sb.length() - 1);
        }
        //setNewGoals(fGoals, sb.toString());
        text.insert(sb.toString());
        entriesChanged();
      }
    }

    // fancy insert into fGoals field
    private void setNewGoals(Text field, String newText) {
      final boolean hadFocus = text.getData() != null;
      final int currPos = field.getCaretPosition();
      final String oldText = field.getText();

      if(!hadFocus) {
        // never had focus - add to the end
        if(oldText.length() > 0 && !oldText.endsWith(" ")) {
          newText = " " + newText;
        }
        field.setText(oldText + newText);
        return;
      }

      // had focus before

      // caret at the first position
      if(currPos == 0) {
        if(oldText.charAt(0) != ' ') {
          field.insert(newText + " ");
          return;
        }
        field.insert(newText);
        return;
      }

      // caret at the last position
      if(currPos == oldText.length()) {
        if(!oldText.endsWith(" ")) {
          field.insert(" " + newText);
          return;
        }
        field.insert(newText);
        return;
      }

      // caret somewhere in the middle
      if(oldText.charAt(currPos) != ' ') {
        newText = newText + " ";
      }
      if(oldText.charAt(currPos - 1) != ' ') {
        newText = " " + newText;
      }
      field.insert(newText);
    }
  }
  
}
