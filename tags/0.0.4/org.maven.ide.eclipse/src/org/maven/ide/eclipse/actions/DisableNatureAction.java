
package org.maven.ide.eclipse.actions;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.container.Maven2ClasspathContainer;


public class DisableNatureAction implements IObjectActionDelegate {
  private ISelection selection;
  private IWorkbenchPart targetPart;

  /*
   * (non-Javadoc)
   * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
   */
  public void run( IAction action) {
    if( selection instanceof IStructuredSelection) {
      IStructuredSelection structuredSelection = ( IStructuredSelection) selection;
      for( Iterator it = structuredSelection.iterator(); it.hasNext();) {
        Object element = it.next();
        IProject project = null;
        if( element instanceof IProject) {
          project = ( IProject) element;
        } else if( element instanceof IAdaptable) {
          project = ( IProject) (( IAdaptable) element).getAdapter( IProject.class);
        }
        if( project != null) {
          disableNature( project, structuredSelection.size()==1);
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
   *      org.eclipse.jface.viewers.ISelection)
   */
  public void selectionChanged( IAction action, ISelection selection) {
    this.selection = selection;
  }

  /*
   * (non-Javadoc) 
   * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
   *      org.eclipse.ui.IWorkbenchPart)
   */
  public void setActivePart( IAction action, IWorkbenchPart targetPart) {
    this.targetPart = targetPart;
  }

  private void disableNature( IProject project, boolean isSingle) {
    try {
      IProjectDescription description = project.getDescription();
      String[] natures = description.getNatureIds();
      ArrayList newNatures = new ArrayList();
      for( int i = 0; i < natures.length; ++i) {
        if( !Maven2Plugin.NATURE_ID.equals( natures[ i])) {
          newNatures.add(natures[i]);
        }
      }
      description.setNatureIds( ( String[] ) newNatures.toArray( new String[ newNatures.size() ] ));
      project.setDescription( description, null);
      
      IJavaProject javaProject = JavaCore.create(project);
      if(javaProject!=null) {
        // remove classpatch container from JavaProject
        IClasspathEntry[] entries = javaProject.getRawClasspath();
        ArrayList newEntries = new ArrayList();
        for( int i = 0; i < entries.length; i++) {
          if( !Maven2ClasspathContainer.isMaven2ClasspathContainer( entries[ i].getPath())) {
            newEntries.add(entries[i]);
          }
        }
        javaProject.setRawClasspath(( IClasspathEntry[] ) newEntries.toArray( new IClasspathEntry[newEntries.size()]), null);
      }
      
    } catch( CoreException ex) {
      Maven2Plugin.getDefault().getConsole().logError( "Can't disable nature "+ex.toString() );
      Maven2Plugin.log(ex);
    
    }
  }

}

