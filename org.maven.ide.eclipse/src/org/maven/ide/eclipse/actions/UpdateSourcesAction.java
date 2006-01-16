
package org.maven.ide.eclipse.actions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.maven.ide.eclipse.Maven2Plugin;


public class UpdateSourcesAction implements IObjectActionDelegate {
  private IAction action;
  private IWorkbenchPart targetPart;
  private ISelection selection;
  

  public void setActivePart( IAction action, IWorkbenchPart targetPart ) {
    this.action = action;
    this.targetPart = targetPart;
  }

  public void selectionChanged( IAction action, ISelection selection ) {
    this.action = action;
    this.selection = selection;
  }

  
  public void run( IAction action ) {
    IStructuredSelection structuredSelection = ( IStructuredSelection ) selection;
    for( Iterator it = structuredSelection.iterator(); it.hasNext(); ) {
      Object element = it.next();
      IProject project = null;
      if( element instanceof IProject ) {
        project = ( IProject ) element;
      } else if( element instanceof IAdaptable ) {
        project = ( IProject ) ( ( IAdaptable ) element ).getAdapter( IProject.class );
      }
      if( project != null ) {
        new UpdateSourcesJob( project).schedule();
      }
    }
  }

  
  private static final class UpdateSourcesJob extends Job {
    private final IProject project;


    private UpdateSourcesJob( IProject project) {
      super( "Updating Source Folders");
      this.project = project;
    }

    protected IStatus run( IProgressMonitor monitor ) {
      IFile pom = project.getFile( Maven2Plugin.POM_FILE_NAME);
      if( !pom.exists()) {
        return Status.OK_STATUS;
      }
      
      Maven2Plugin plugin = Maven2Plugin.getDefault();
      try {
        List sourceEntries = new ArrayList(); 
        plugin.resolveSourceEntries(sourceEntries, project, pom, true, monitor);
        
        Set sources = new HashSet();
        for( Iterator it = sourceEntries.listIterator(); it.hasNext(); ) {
          IClasspathEntry entry = ( IClasspathEntry ) it.next();
          if(!sources.add( entry.getPath().toString()) ) {
              it.remove();
          }
        }
        
        IJavaProject javaProject = JavaCore.create(project);
        
        IClasspathEntry[] currentClasspath = javaProject.getRawClasspath();
        for( int i = 0; i < currentClasspath.length; i++ ) {
          if( currentClasspath[i].getEntryKind()!=IClasspathEntry.CPE_SOURCE) {
            sourceEntries.add( currentClasspath[i]);
          }
        }
        
        IClasspathEntry[] entries = ( IClasspathEntry[] ) sourceEntries.toArray( new IClasspathEntry[ sourceEntries.size()]);
        javaProject.setRawClasspath(entries, monitor);
        
        plugin.getConsole().logMessage("Updated source folders for project "+project.getName());

      } catch( Exception ex ) {
        String msg = "Unable to update project source folders "+ex.toString();
        plugin.getConsole().logMessage( msg );
        Maven2Plugin.log( msg, ex);
      }
      return Status.OK_STATUS;
    }
    
  }

}

