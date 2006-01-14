
package org.maven.ide.eclipse.actions;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
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
import org.maven.ide.eclipse.container.Maven2ClasspathContainer;
import org.maven.ide.eclipse.wizards.Maven2PomWizard;


public class ToggleNatureAction implements IObjectActionDelegate {
  private ISelection selection;
  private IWorkbenchPart targetPart;

  /*
   * (non-Javadoc)
   * 
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
          toggleNature( project, structuredSelection.size()==1);
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
  public void selectionChanged( IAction action, ISelection selection) {
    this.selection = selection;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
   *      org.eclipse.ui.IWorkbenchPart)
   */
  public void setActivePart( IAction action, IWorkbenchPart targetPart) {
    this.targetPart = targetPart;
  }

  /**
   * Toggles sample nature on a project
   * 
   * @param project to have sample nature added or removed
   */
  private void toggleNature( IProject project, boolean isSingle) {
    try {
      IFile pom = project.getFile( Maven2Plugin.POM_FILE_NAME);
      if( isSingle && !pom.exists()) {
        Maven2PomWizard wizard = new Maven2PomWizard();
        
        Maven2Plugin plugin = Maven2Plugin.getDefault();
        IWorkbench workbench = plugin.getWorkbench();
        wizard.init(workbench, (IStructuredSelection) selection);
        
        Shell shell = workbench.getActiveWorkbenchWindow().getShell();
        WizardDialog wizardDialog = new WizardDialog( shell, wizard);
        wizardDialog.create();
        wizardDialog.getShell().setText("Create new POM");
        if(wizardDialog.open()==Window.CANCEL) {
          return;
        }
      }
      
      IProjectDescription description = project.getDescription();
      String[] natures = description.getNatureIds();
      for( int i = 0; i < natures.length; ++i) {
        if( Maven2Plugin.NATURE_ID.equals( natures[ i])) {
          removeMaven2Nature( project, i);
          removeMaven2ClasspathContainer( project);
          return;
        }
      }

      addMaven2Nature( project);
      removeMaven2ClasspathContainer(project);
      addMaven2ClasspathContainer(project);
      
    } catch( CoreException ex) {
      Maven2Plugin.log(ex);
    
    }
  }

  private void addMaven2Nature( IProject project) throws CoreException {
    IProjectDescription description = project.getDescription();
    String[] natures = description.getNatureIds();
    String[] newNatures = new String[ natures.length + 1];
    System.arraycopy( natures, 0, newNatures, 0, natures.length);
    newNatures[ natures.length] = Maven2Plugin.NATURE_ID;
    description.setNatureIds( newNatures);
    project.setDescription( description, null);
  }

  private void addMaven2ClasspathContainer( IProject project) throws JavaModelException {
    IJavaProject p = JavaCore.create(project);
    if(p!=null) {
      IClasspathEntry[] entries = p.getRawClasspath();
      IClasspathEntry[] newEntries = new IClasspathEntry[ entries.length + 1];
      System.arraycopy( entries, 0, newEntries, 0, entries.length);
      newEntries[ entries.length] = JavaCore.newContainerEntry( new Path( Maven2Plugin.CONTAINER_ID));
      p.setRawClasspath(newEntries, null);
    }
  }
  
  private void removeMaven2ClasspathContainer( IProject project) throws JavaModelException {
    IJavaProject p = JavaCore.create(project);
    if(p!=null) {
      // remove classpatch container from JavaProject
      IClasspathEntry[] entries = p.getRawClasspath();
      for( int i = 0; i < entries.length; i++) {
        if( Maven2ClasspathContainer.isMaven2ClasspathContainer( entries[ i].getPath())) {
          // Remove maven2 classpath container
          IClasspathEntry[] newEntries = new IClasspathEntry[ entries.length - 1];
          System.arraycopy( entries, 0, newEntries, 0, i);
          System.arraycopy( entries, i + 1, newEntries, i, entries.length - i - 1);
          p.setRawClasspath(newEntries, null);  // TODO add monitor?
        }
      }
    }
  }

  private void removeMaven2Nature( IProject project, int i) throws CoreException {
    IProjectDescription description = project.getDescription();
    String[] natures = description.getNatureIds();
    String[] newNatures = new String[ natures.length - 1];
    System.arraycopy( natures, 0, newNatures, 0, i);
    System.arraycopy( natures, i + 1, newNatures, i, natures.length - i - 1);
    description.setNatureIds( newNatures);
    project.setDescription( description, null);
  }

}

