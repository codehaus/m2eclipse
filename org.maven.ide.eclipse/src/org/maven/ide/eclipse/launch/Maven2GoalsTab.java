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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;
import org.eclipse.ui.externaltools.internal.model.IExternalToolsHelpContextIds;
import org.maven.ide.eclipse.Maven2Plugin;


public class Maven2GoalsTab extends AbstractLaunchConfigurationTab {

  private ILaunchConfiguration configuration;

  private Button autoBuildGoals;
  private Button manualBuildGoals;
  private Button duringCleanGoals;
  private Button afterCleanGoals;

  private Text autoBuildGoalsText;
  private Text manualBuildGoalsText;
  private Text duringCleanGoalsText;
  private Text afterCleanGoalsText;

  private Map attributeToGoals = new HashMap();


  private SelectionListener selectionListener = new SelectionAdapter() {

    public void widgetSelected(SelectionEvent e) {
      String attribute = null;
      Object source = e.getSource();
      Text text = null;
      if(source == autoBuildGoals) {
        attribute = Maven2LaunchConstants.ATTR_GOALS_AUTO_BUILD;
        text = autoBuildGoalsText;
      } else if(source == manualBuildGoals) {
        attribute = Maven2LaunchConstants.ATTR_GOALS_MANUAL_BUILD;
        text = manualBuildGoalsText;
      } else if(source == duringCleanGoals) {
        attribute = Maven2LaunchConstants.ATTR_GOALS_CLEAN;
        text = duringCleanGoalsText;
      } else if(source == afterCleanGoals) {
          attribute = Maven2LaunchConstants.ATTR_GOALS_AFTER_CLEAN;
          text = afterCleanGoalsText;
      }

      setTargets(attribute, text);
      updateLaunchConfigurationDialog();
    }
  };


  protected void createTargetsComponent(Composite parent) {
    Label afterCleanGoalsLabel = new Label(parent, SWT.NONE);
    afterCleanGoalsLabel.setText("After a \"Clean\"");
    afterCleanGoalsText = new Text(parent, SWT.SINGLE | SWT.WRAP | SWT.BORDER);
    afterCleanGoalsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    afterCleanGoals = createPushButton(parent, "Select...", null);
    afterCleanGoals.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
    afterCleanGoals.addSelectionListener(selectionListener);
    
    Label manualBuildGoalsLabel = new Label(parent, SWT.NONE);
    manualBuildGoalsLabel.setText("Manual Build");
    manualBuildGoalsText = new Text(parent, SWT.SINGLE | SWT.WRAP | SWT.BORDER);
    manualBuildGoalsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    manualBuildGoals = createPushButton(parent, "Select...", null);
    manualBuildGoals.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
    manualBuildGoals.addSelectionListener(selectionListener);
    
    Label autoBuildGoalsLabel = new Label(parent, SWT.NONE);
    autoBuildGoalsLabel.setText("Auto Build");
    autoBuildGoalsText = new Text(parent, SWT.SINGLE | SWT.WRAP | SWT.BORDER);
    autoBuildGoalsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    autoBuildGoals = createPushButton(parent, "Select...", null);
    autoBuildGoals.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
    autoBuildGoals.addSelectionListener(selectionListener);
    
    Label duringCleanGoalsLabel = new Label(parent, SWT.NONE);
    duringCleanGoalsLabel.setText("During a \"Clean\"");
    duringCleanGoalsText = new Text(parent, SWT.SINGLE | SWT.WRAP | SWT.BORDER);
    duringCleanGoalsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    duringCleanGoals = createPushButton(parent, "Select...", null);
    duringCleanGoals.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
    duringCleanGoals.addSelectionListener(selectionListener);
  }

  protected void setTargets(String attribute, Text text) {
    Maven2GoalSelectionDialog dialog = new Maven2GoalSelectionDialog(getShell());

    if(dialog.open() != Window.OK) {
      return;
    }
    
    Object[] targetsSelected = dialog.getResult();

    System.err.println(targetsSelected);
    
//    if(targetsSelected == null) {//default
//      text.setEnabled(true);
//      attributeToGoals.remove(attribute);
//      setTargetsForUser(text, DEFAULT_TARGET_SELECTED, null);
//    } else if(targetsSelected.length() == 0) {
//      text.setEnabled(false);
//      attributeToGoals.remove(attribute);
//      text.setText(NOT_ENABLED);
//    } else {
//      text.setEnabled(true);
//      attributeToGoals.put(attribute, targetsSelected);
//      setTargetsForUser(text, targetsSelected, null);
//    }
  }

  public void createControl(Composite parent) {
    Composite mainComposite = new Composite(parent, SWT.NONE);
    setControl(mainComposite);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(),
        IExternalToolsHelpContextIds.EXTERNAL_TOOLS_LAUNCH_CONFIGURATION_DIALOG_BUILDER_TAB);

    GridLayout layout = new GridLayout();
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    layout.numColumns = 2;
    layout.makeColumnsEqualWidth = false;
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    mainComposite.setLayout(layout);
    mainComposite.setLayoutData(gridData);
    mainComposite.setFont(parent.getFont());
    createTargetsComponent(mainComposite);
  }

  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    configuration.setAttribute(IExternalToolConstants.ATTR_TRIGGERS_CONFIGURED, true);
  }

  public void initializeFrom(ILaunchConfiguration configuration) {
    this.configuration = configuration;

    try {
      String autoTargets = configuration.getAttribute(Maven2LaunchConstants.ATTR_GOALS_AUTO_BUILD, "");
      String manualTargets = configuration.getAttribute(Maven2LaunchConstants.ATTR_GOALS_MANUAL_BUILD, "");
      String afterCleanTargets = configuration.getAttribute(Maven2LaunchConstants.ATTR_GOALS_AFTER_CLEAN, "");
      String duringCleanTargets = configuration.getAttribute(Maven2LaunchConstants.ATTR_GOALS_CLEAN, "");
      
      manualBuildGoalsText.setText(manualTargets);
      afterCleanGoalsText.setText(afterCleanTargets);
      duringCleanGoalsText.setText(duringCleanTargets);
      autoBuildGoalsText.setText(autoTargets);
    } catch(CoreException ce) {
      Maven2Plugin.log("Error reading configuration", ce); //$NON-NLS-1$
    }
  }

  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    StringBuffer buffer = new StringBuffer();
    if(afterCleanGoalsText.getText().length()!=0) {
      buffer.append(IExternalToolConstants.BUILD_TYPE_FULL).append(',');
    }
    if(manualBuildGoalsText.getText().length()!=0) {
      buffer.append(IExternalToolConstants.BUILD_TYPE_INCREMENTAL).append(',');
    }
    if(autoBuildGoalsText.getText().length()!=0) {
      buffer.append(IExternalToolConstants.BUILD_TYPE_AUTO).append(',');
    }
    if(duringCleanGoalsText.getText().length()!=0) {
      buffer.append(IExternalToolConstants.BUILD_TYPE_CLEAN);
    }
    configuration.setAttribute(IExternalToolConstants.ATTR_RUN_BUILD_KINDS, buffer.toString());

    String targets = (String) attributeToGoals.get(Maven2LaunchConstants.ATTR_GOALS_AFTER_CLEAN);
    configuration.setAttribute(Maven2LaunchConstants.ATTR_GOALS_AFTER_CLEAN, targets);
    targets = (String) attributeToGoals.get(Maven2LaunchConstants.ATTR_GOALS_AUTO_BUILD);
    configuration.setAttribute(Maven2LaunchConstants.ATTR_GOALS_AUTO_BUILD, targets);
    targets = (String) attributeToGoals.get(Maven2LaunchConstants.ATTR_GOALS_MANUAL_BUILD);
    configuration.setAttribute(Maven2LaunchConstants.ATTR_GOALS_MANUAL_BUILD, targets);
    targets = (String) attributeToGoals.get(Maven2LaunchConstants.ATTR_GOALS_CLEAN);
    configuration.setAttribute(Maven2LaunchConstants.ATTR_GOALS_CLEAN, targets);

    // configuration.setAttribute(Maven2LaunchConstants.ATTR_GOALS_UPDATED, true);
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
   */
  public String getName() {
    return "Goals";
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
   */
  public Image getImage() {
    return null;
  }
}
