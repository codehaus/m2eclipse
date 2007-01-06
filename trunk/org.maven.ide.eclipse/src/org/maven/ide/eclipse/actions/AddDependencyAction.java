
package org.maven.ide.eclipse.actions;

import java.util.Collections;
import java.util.Set;

import org.apache.maven.project.MavenProject;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
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
import org.maven.ide.eclipse.embedder.ClassPathResolver;
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;
import org.maven.ide.eclipse.index.Indexer;
import org.maven.ide.eclipse.index.Indexer.FileInfo;


public class AddDependencyAction implements IObjectActionDelegate {
  private IStructuredSelection selection;
  private IWorkbenchPart targetPart;

  public void run(IAction action) {
    Object o = selection.iterator().next();
    final IFile file;
    if(o instanceof IProject) {
      file = ((IProject) o).getFile(Maven2Plugin.POM_FILE_NAME);
    } else if(o instanceof IFile) {
      file = (IFile) o;
    } else {
      return;
    }

    Maven2Plugin plugin = Maven2Plugin.getDefault();
    MavenEmbedderManager embedderManager = plugin.getMavenEmbedderManager();
    
    MavenProject mavenProject;
    try {
      mavenProject = (MavenProject) embedderManager.executeInEmbedder("Read Project", new ClassPathResolver.ReadProjectTask(
          file, plugin.getConsole(), plugin.getMavenRepositoryIndexManager(), plugin.getPreferenceStore()));
    } catch(CoreException ex) {
      Maven2Plugin.log(ex);
      return;
    }

    Set artifacts = mavenProject == null ? Collections.EMPTY_SET : mavenProject.getArtifacts();

    Maven2RepositorySearchDialog dialog = new Maven2RepositorySearchDialog(getShell(), artifacts, Indexer.JAR_NAME);
    if(dialog.open() == Window.OK) {
      Indexer.FileInfo fileInfo = (FileInfo) dialog.getFirstResult();
      if(fileInfo != null) {
        try {
          embedderManager.addDependency(file, fileInfo.getDependency());
        } catch(Exception ex) {
          Maven2Plugin.log("Can't add dependency to " + file, ex);
        }
      }
    }
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    this.targetPart = targetPart;
  }

  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    }
  }

  protected Shell getShell() {
    Shell shell = null;
    if(targetPart != null) {
      shell = targetPart.getSite().getShell();
    }
    if(shell != null) {
      return shell;
    }

    IWorkbench workbench = Maven2Plugin.getDefault().getWorkbench();
    if(workbench == null) {
      return null;
    }

    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    return window == null ? null : window.getShell();
  }

}
