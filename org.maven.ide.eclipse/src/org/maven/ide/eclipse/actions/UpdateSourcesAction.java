
package org.maven.ide.eclipse.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
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
      Set sources = new TreeSet();      
      try {
        resolveSources(sources, project, monitor);
        updateProject(sources, project, monitor);
        
      } catch( Exception ex ) {
        // TODO show popup error dialog
        Maven2Plugin.log( "Unable to update project source folders", ex);
        
      }
      return Status.OK_STATUS;
    }

    
    private void resolveSources( Set sources, IProject project, IProgressMonitor monitor) {
      IFile pom = project.getFile( Maven2Plugin.POM_FILE_NAME);
      if( pom.exists()) {
        Maven2Plugin plugin = Maven2Plugin.getDefault();
        plugin.resolveSourceEntries(sources, project, pom, true, monitor);
      }    
    }  
    
    private void updateProject( Set sources, IProject project2, IProgressMonitor monitor ) throws JavaModelException {
      ArrayList sourceEntries = new ArrayList();
      for( Iterator it = sources.iterator(); it.hasNext(); ) {
        String name = ( String ) it.next();
        IResource resource = project.findMember(name);
        sourceEntries.add(JavaCore.newSourceEntry( resource.getFullPath() /*, new IPath[] { new Path( "**"+"/.svn/"+"**")} */));
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
    }
    
  }

}

