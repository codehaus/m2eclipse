package org.maven.ide.eclipse.actions;

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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.launch.Maven2LaunchConstants;
import org.maven.ide.eclipse.launch.Maven2LaunchMainTab;
import org.maven.ide.eclipse.util.ITraceable;
import org.maven.ide.eclipse.util.Tracer;
import org.maven.ide.eclipse.util.Util;


/**
 * Maven launch shortcut
 * 
 * @author Dmitri Maximovich
 * @author Eugene Kuleshov
 */
public class ExecutePomAction implements ILaunchShortcut, IExecutableExtension, ITraceable {
  private static final boolean TRACE_ENABLED = Boolean.valueOf(Platform.getDebugOption("org.maven.ide.eclipse/actions")).booleanValue();

  private boolean showDialog = false;
  private String goalName = null;

  
  public boolean isTraceEnabled() {
    return TRACE_ENABLED;
  }

  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    if("WITH_DIALOG".equals(data)) {
      this.showDialog = true;
    } else {
      this.goalName = (String) data;
    }
  }
  
  public void launch(IEditorPart editor, String mode) {
    IEditorInput editorInput = editor.getEditorInput();
    if(editorInput instanceof IFileEditorInput) {
      launch(((IFileEditorInput) editorInput).getFile().getParent().getLocation());
    }
  }

  public void launch(ISelection selection, String mode) {
    if(selection instanceof IStructuredSelection) {
      IStructuredSelection structuredSelection = (IStructuredSelection) selection;
      Object object = structuredSelection.getFirstElement();
  
      IPath basedir = null;
      if(object instanceof IProject || object instanceof IFolder) {
        basedir = ((IResource) object).getLocation();
      } else if(object instanceof IFile) {
        basedir = ((IFile) object).getParent().getLocation();
      } else if(object instanceof IAdaptable) {
        IAdaptable adaptable = (IAdaptable) object;
        Object adapter = adaptable.getAdapter(IProject.class);
        if(adapter!=null) {
          basedir = ((IResource) adapter).getLocation();
        } else {
          adapter = adaptable.getAdapter(IFolder.class);
          if(adapter!=null) {
            basedir = ((IResource) adapter).getLocation();
          } else {
            adapter = adaptable.getAdapter(IFile.class);
            if(adapter!=null) {
              basedir = ((IFile) object).getParent().getLocation();
            }
          }
        }
      }
  
      launch(basedir);
    }
  }

  private void launch(IPath basedir) {
    if(basedir == null) {
      return;
    }
    
    Tracer.trace(this, "Launching from basedir", basedir);
    
    ILaunchConfiguration launchConfiguration = getLaunchConfiguration(basedir);
    if(launchConfiguration == null) {
      return;
    }
    
    boolean openDialog = showDialog;
    if(!openDialog) {
      try {
        // if no goals specified
        String goals = launchConfiguration.getAttribute(Maven2LaunchConstants.ATTR_GOALS, (String) null);
        openDialog = goals == null || goals.trim().length() == 0;
      } catch (CoreException ex) {
        Maven2Plugin.log(ex);
      }
    }
    
    if(openDialog) {
      Tracer.trace(this, "Opening dialog for launch configuration", launchConfiguration.getName());
      DebugUITools.saveBeforeLaunch();
      DebugUITools.openLaunchConfigurationDialog(Maven2Plugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(), 
          launchConfiguration, Maven2LaunchMainTab.ID_EXTERNAL_TOOLS_LAUNCH_GROUP, null);
    } else {
      Tracer.trace(this, "Launching configuration", launchConfiguration.getName());
      DebugUITools.launch(launchConfiguration, ILaunchManager.RUN_MODE);
    }
  }

  private ILaunchConfiguration createLaunchConfiguration(IPath baseDir, String goal) {
    try {
      ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
      ILaunchConfigurationType launchConfigurationType = launchManager.getLaunchConfigurationType(Maven2LaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);
  
      ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null, "Executing " + goal + " on " + baseDir);
      workingCopy.setAttribute(Maven2LaunchConstants.ATTR_POM_DIR, baseDir.toOSString());
      workingCopy.setAttribute(Maven2LaunchConstants.ATTR_GOALS, goal);
      workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_SCOPE, "${project}");
      workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_RECURSIVE, true);
  
      return workingCopy;
    } catch(CoreException ex) {
      Tracer.trace(this, "Error creating new launch configuration", "", ex);
    }
    return null;
  }
  
  private ILaunchConfiguration getLaunchConfiguration(IPath basedir) {
    if(goalName!=null) {
        return createLaunchConfiguration(basedir, goalName);
    }
    
    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType launchConfigurationType = launchManager.getLaunchConfigurationType(Maven2LaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);

    // scan existing launch configurations
    if(!showDialog) {
      try {
        ILaunch[] launches = launchManager.getLaunches();
        ILaunchConfiguration[] launchConfigurations = null;

        if(launches.length > 0) {
          for(int i = 0; i < launches.length; i++ ) {
            ILaunchConfiguration config = launches[i].getLaunchConfiguration();
            if(config!=null && launchConfigurationType.equals(config.getType())) {
              launchConfigurations = new ILaunchConfiguration[] {config};
            }
          }
        }
        if(launchConfigurations == null) {
          launchConfigurations = launchManager.getLaunchConfigurations(launchConfigurationType);
        }
        for(int i = 0; i < launchConfigurations.length; i++ ) {
          ILaunchConfiguration cfg = launchConfigurations[i];
          Tracer.trace(this, "Processing existing launch configuration", cfg.getName());
          // don't forget to substitute variables
          String workDir = Util.substituteVar(cfg.getAttribute(Maven2LaunchConstants.ATTR_POM_DIR, (String) null));
          if(workDir == null) {
            Tracer.trace(this, "Launch configuration doesn't have workdir!");
            continue;
          }
          Tracer.trace(this, "Workdir", workDir);
          IPath workPath = new Path(workDir);
          if(basedir.equals(workPath)) {
            Tracer.trace(this, "Found matching existing configuration", cfg.getName());
            return cfg;
          }
        }
      } catch(CoreException e) {
        Tracer.trace(this, "Error scanning existing launch configurations", "", e);
      }

      Tracer.trace(this, "No existing configurations found, creating new");
    }

    Tracer.trace(this, "No existing configurations found, creating new");

    String newName = launchManager.generateUniqueLaunchConfigurationNameFrom(basedir.lastSegment());
    try {
      Tracer.trace(this, "New configuration name", newName);
      ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null, newName);
      workingCopy.setAttribute(Maven2LaunchConstants.ATTR_POM_DIR, basedir.toString());
      
      // set other defaults if needed
      // Maven2LaunchMainTab maintab = new Maven2LaunchMainTab();
      // maintab.setDefaults(workingCopy);
      // maintab.dispose();
      
      ILaunchConfiguration newConfiguration = workingCopy.doSave();
      return newConfiguration;
    } catch (Exception e) {
      String msg = "Error creating new launch configuration";
      Tracer.trace(this, msg, newName, e);
    }
    return null;
  }

}

