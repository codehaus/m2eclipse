/*
 * Licensed to the Codehaus Foundation (ASF) under one or more contributor
 * license agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership. The ASF licenses this
 * file to you under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.maven.ide.eclipse.actions;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.wizards.Maven2PomWizard;


public class EnableNatureAction implements IObjectActionDelegate {
  private ISelection selection;

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
   */
  public void run(IAction action) {
    if(selection instanceof IStructuredSelection) {
      IStructuredSelection structuredSelection = (IStructuredSelection) selection;
      for(Iterator it = structuredSelection.iterator(); it.hasNext();) {
        Object element = it.next();
        IProject project = null;
        if(element instanceof IProject) {
          project = (IProject) element;
        } else if(element instanceof IAdaptable) {
          project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
        }
        if(project != null) {
          enableNature(project, structuredSelection.size() == 1);
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
   *      org.eclipse.jface.viewers.ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {
    this.selection = selection;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
   *      org.eclipse.ui.IWorkbenchPart)
   */
  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

  private void enableNature(IProject project, boolean isSingle) {
    try {
      Maven2Plugin plugin = Maven2Plugin.getDefault();
      IFile pom = project.getFile(Maven2Plugin.POM_FILE_NAME);
      if(isSingle && !pom.exists()) {
        IWorkbench workbench = plugin.getWorkbench();

        Maven2PomWizard wizard = new Maven2PomWizard();
        wizard.init(workbench, (IStructuredSelection) selection);

        Shell shell = workbench.getActiveWorkbenchWindow().getShell();
        WizardDialog wizardDialog = new WizardDialog(shell, wizard);
        wizardDialog.create();
        wizardDialog.getShell().setText("Create new POM");
        if(wizardDialog.open() == Window.CANCEL) {
          return;
        }
      }
      
      plugin.getClasspathResolver().enableMavenNature(project);

    } catch(CoreException ex) {
      Maven2Plugin.log(ex);
    }
  }

}
