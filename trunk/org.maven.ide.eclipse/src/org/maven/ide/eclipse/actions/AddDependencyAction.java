
package org.maven.ide.eclipse.actions;

import java.util.Set;

import org.apache.maven.project.MavenProject;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.index.Indexer;
import org.maven.ide.eclipse.index.Indexer.FileInfo;


public class AddDependencyAction implements IObjectActionDelegate {
  private IStructuredSelection selection;
  private IWorkbenchPart targetPart;
  private Shell shell;

  
  public void run( IAction action ) {
    Object o = selection.iterator().next();
    if( o instanceof IProject ) {
      IProject project = ( IProject ) o;

      Maven2Plugin plugin = getPlugin();
      MavenProject mavenProject = plugin.getMavenProject( project.getFile( Maven2Plugin.POM_FILE_NAME ), true);
      Set artifacts = mavenProject==null ? null : mavenProject.getArtifacts();

      Maven2RepositorySearchDialog dialog = new Maven2RepositorySearchDialog( getShell(), plugin.getIndexer(), artifacts );
      if( dialog.open() == Window.OK ) {
        Indexer.FileInfo fileInfo = ( FileInfo ) dialog.getFirstResult();
        if( fileInfo != null ) {
          plugin.addDependency( project, fileInfo.getDependency() );
        }
      }
    }
  }

  public void setActivePart( IAction action, IWorkbenchPart targetPart ) {
    if( targetPart != null ) {
      this.shell = targetPart.getSite().getShell();
      this.targetPart = targetPart;
    }
  }

  public void selectionChanged( IAction action, ISelection selection ) {
    if( selection instanceof IStructuredSelection ) {
      this.selection = ( IStructuredSelection ) selection;
    }
  }

  protected Shell getShell() {
    if( shell != null ) return shell;

    IWorkbench workbench = getPlugin().getWorkbench();
    if( workbench == null ) return null;

    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    if( window == null ) return null;

    return window.getShell();
  }

  protected Maven2Plugin getPlugin() {
    return Maven2Plugin.getDefault();
  }

}

