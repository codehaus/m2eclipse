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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;
import org.eclipse.jdt.internal.debug.ui.launcher.VMArgumentsBlock;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;


public class Maven2JRETab extends JavaJRETab {

  private VMArgumentsBlock vmArgumentsBlock = new VMArgumentsBlock();

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {
    super.createControl(parent);

    Composite comp = (Composite) fJREBlock.getControl();
    ((GridData) comp.getLayoutData()).grabExcessVerticalSpace = true;
    ((GridData) comp.getLayoutData()).verticalAlignment = SWT.FILL;

    vmArgumentsBlock.createControl(comp);
    ((GridData) vmArgumentsBlock.getControl().getLayoutData()).horizontalSpan = 2;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
   */
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    super.performApply(configuration);
    vmArgumentsBlock.performApply(configuration);
    setLaunchConfigurationWorkingCopy(configuration);
  }

  private boolean useDefaultSeparateJRE(ILaunchConfigurationWorkingCopy configuration) {
    boolean deflt = false;
    String vmInstallType = null;
    String jreContainerPath = null;
    try {
      vmInstallType = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, (String) null);
      jreContainerPath = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH,
          (String) null);
    } catch(CoreException e) {
    }
    if(vmInstallType != null) {
      configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, (String) null);
    }
    if(jreContainerPath != null) {
      configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, (String) null);
    }
    IVMInstall defaultVMInstall = getDefaultVMInstall(configuration);
    if(defaultVMInstall != null) {
      IVMInstall vm = fJREBlock.getJRE();
      deflt = defaultVMInstall.equals(vm);
    }

    if(vmInstallType != null) {
      configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, vmInstallType);
    }
    if(jreContainerPath != null) {
      configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, jreContainerPath);
    }
    return deflt;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
   */
  public void initializeFrom(ILaunchConfiguration configuration) {
    super.initializeFrom(configuration);
    vmArgumentsBlock.initializeFrom(configuration);
    // fVMArgumentsBlock.setEnabled(!fJREBlock.isDefaultJRE());
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setLaunchConfigurationDialog(org.eclipse.debug.ui.ILaunchConfigurationDialog)
   */
  public void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog) {
    super.setLaunchConfigurationDialog(dialog);
    vmArgumentsBlock.setLaunchConfigurationDialog(dialog);
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#activated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
   */
  public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
    setLaunchConfigurationWorkingCopy(workingCopy);
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
   */
  public void setDefaults(ILaunchConfigurationWorkingCopy config) {
    super.setDefaults(config);
    IVMInstall defaultVMInstall = getDefaultVMInstall(config);
    if(defaultVMInstall != null) {
      setDefaultVMInstallAttributes(defaultVMInstall, config);
    }

  }

  private IVMInstall getDefaultVMInstall(ILaunchConfiguration config) {
    IVMInstall defaultVMInstall;
    try {
      defaultVMInstall = JavaRuntime.computeVMInstall(config);
    } catch(CoreException e) {
      //core exception thrown for non-Java project
      defaultVMInstall = JavaRuntime.getDefaultVMInstall();
    }
    return defaultVMInstall;
  }

  private void setDefaultVMInstallAttributes(IVMInstall defaultVMInstall, ILaunchConfigurationWorkingCopy config) {
    String vmName = defaultVMInstall.getName();
    String vmTypeID = defaultVMInstall.getVMInstallType().getId();
    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_NAME, vmName);
    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, vmTypeID);
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#deactivated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
   */
  public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
  }
}
