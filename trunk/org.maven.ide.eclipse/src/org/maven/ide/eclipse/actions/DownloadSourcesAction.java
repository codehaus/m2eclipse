
package org.maven.ide.eclipse.actions;

import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;


public class DownloadSourcesAction implements IObjectActionDelegate {

  private IStructuredSelection selection;

  public void run(IAction action) {
    IPackageFragmentRoot root = (IPackageFragmentRoot) this.selection.getFirstElement();

    IJavaElement parent = root.getParent();
    if(parent instanceof IJavaProject) {
      IJavaProject project = (IJavaProject) parent;
      try {
        IClasspathEntry classpathEntry = root.getRawClasspathEntry();
        IClasspathContainer classpathContainer = JavaCore.getClasspathContainer(classpathEntry.getPath(), project);
        IClasspathEntry[] classpathEntries = classpathContainer.getClasspathEntries();
        for(int i = 0; i < classpathEntries.length; i++ ) {
          IClasspathEntry entry = classpathEntries[i];
          if(entry.getPath().equals(root.getPath())) {
            String actionDefinitionId = action.getActionDefinitionId();
            String id = action.getId();
            System.err.println("### DownloadSourcesAction.run()");
            
          }
        }
      } catch(JavaModelException ex) {
        //
      }
    }
  }

  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    } else {
      this.selection = null;
    }
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

}

