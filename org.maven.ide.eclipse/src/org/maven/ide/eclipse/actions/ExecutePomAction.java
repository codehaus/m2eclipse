package org.maven.ide.eclipse.actions;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.launch.Maven2LaunchConstants;
import org.maven.ide.eclipse.launch.Maven2LaunchMainTab;
import org.maven.ide.eclipse.util.ITraceable;
import org.maven.ide.eclipse.util.Tracer;


/**
 * Action which executes from context menu when pom.xml selected
 * 
 * @author Dmitri Maximovich
 */
public class ExecutePomAction implements ILaunchShortcut, Maven2LaunchConstants, ITraceable {
  private static final boolean TRACE_ENABLED = Boolean.valueOf(Platform.getDebugOption("org.maven.ide.eclipse/actions")).booleanValue();

  private boolean showDialog = false;

  
  public boolean isTraceEnabled() {
    return TRACE_ENABLED;
  }

  public void launch(ISelection selection, String mode ) {
    if (!(selection instanceof IStructuredSelection)) {
      return;
    }
    IStructuredSelection structuredSelection = (IStructuredSelection) selection;
    
    IResource resource = null;

    Object object = structuredSelection.getFirstElement();
    if (object instanceof IResource) {
      resource = ( IResource ) object;
    } else if (object instanceof IAdaptable) {
      resource = (IResource)((IAdaptable) object).getAdapter(IResource.class);
    }
    if( resource==null) {
      return;
    }

    File f = resource.getParent().getLocation().toFile();
    IPath basedir = new Path(f.getAbsolutePath());
    
    Tracer.trace(this, "Launching from basedir", basedir);
    
    ILaunchConfiguration launchConfiguration = getLaunchConfiguration(basedir);
    if( launchConfiguration == null ) {
      return;
    }
    
    if (!showDialog){
      try {
        // if no goals specified
        String goals = launchConfiguration.getAttribute(ATTR_GOALS, (String)null);
        showDialog = (goals == null || goals.trim().length() == 0);
      } catch (CoreException e) {
        Tracer.trace(this, "Error creating new launch configuration", "", e);
        Maven2Plugin.log(e);
      }
    }
    
    if (showDialog) {
      Tracer.trace(this, "Opening dialog for launch configuration", launchConfiguration.getName());
      DebugUITools.saveBeforeLaunch();
      DebugUITools.openLaunchConfigurationDialog(Maven2Plugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(), 
          launchConfiguration, Maven2LaunchMainTab.ID_EXTERNAL_TOOLS_LAUNCH_GROUP, null);
    } else {
      Tracer.trace(this, "Launching configuration", launchConfiguration.getName());
      DebugUITools.launch(launchConfiguration, ILaunchManager.RUN_MODE);
    }
    
  }

  private ILaunchConfiguration getLaunchConfiguration(IPath basedir) {
    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType launchConfigurationType = launchManager.getLaunchConfigurationType(LAUNCH_CONFIGURATION_TYPE_ID);

    // scan existing launch configurations
    if (!showDialog){
      try {
        ILaunch[] launches = launchManager.getLaunches();
        ILaunchConfiguration[] launchConfigurations = null;
        
        if (launches.length > 0){
          for (int i=0;i<launches.length;i++){
            if (launches[i].getLaunchConfiguration().getType().equals(launchConfigurationType)){
              launchConfigurations = new ILaunchConfiguration[]{launches[i].getLaunchConfiguration()};
            }
          }
        }
        if (launchConfigurations == null){
          launchConfigurations = launchManager.getLaunchConfigurations(launchConfigurationType);
        }
        for( int i = 0; i < launchConfigurations.length; i++ ) {
          ILaunchConfiguration cfg = launchConfigurations[i];
          Tracer.trace(this, "Processing existing launch configuration", cfg.getName());
          // don't forget to substitute variables
          String workDir = Maven2Plugin.substituteVar(cfg.getAttribute(ATTR_POM_DIR, (String)null));
          if (workDir == null) {
            Tracer.trace(this, "Launch configuration doesn't have workdir!");
            continue;
          }
          Tracer.trace(this, "Workdir", workDir);
          IPath workPath = new Path(workDir);
          if (basedir.equals(workPath)) {
            Tracer.trace(this, "Found matching existing configuration", cfg.getName());
            return cfg;
          }
        }
      } catch (CoreException e) {
        Tracer.trace(this, "Error scanning existing launch configurations", "", e);
        Maven2Plugin.log(e);
      }
  
      Tracer.trace(this, "No existing configurations found, creating new");
    }

    Tracer.trace(this, "No existing configurations found, creating new");

    String newName = launchManager.generateUniqueLaunchConfigurationNameFrom(basedir.lastSegment());
    try {
      Tracer.trace(this, "New configuration name", newName);
      ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null, newName);
      workingCopy.setAttribute(ATTR_POM_DIR, basedir.toString());
      
      // set other defaults if needed
      // Maven2LaunchMainTab maintab = new Maven2LaunchMainTab();
      // maintab.setDefaults(workingCopy);
      // maintab.dispose();
      
      ILaunchConfiguration newConfiguration = workingCopy.doSave();
      return newConfiguration;
    } catch (Exception e) {
      String msg = "Error creating new launch configuration";
      Tracer.trace(this, msg, newName, e);
      Maven2Plugin.log(msg, e);
    }
    return null;
  }

  public void launch( IEditorPart editor, String mode ) {
    // nothing for now
  }

  protected void setShowDialog( boolean showDialog) {
    this.showDialog = showDialog;
  }
  
}

