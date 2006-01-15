
package org.maven.ide.eclipse.container;

import java.util.HashSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.maven.ide.eclipse.Maven2Plugin;


/**
 * Maven2ClasspathContainerInitializer
 *
 * @author Eugene Kuleshov
 */
public class Maven2ClasspathContainerInitializer extends ClasspathContainerInitializer {

  public void initialize( IPath containerPath, final IJavaProject project) {
    if( Maven2ClasspathContainer.isMaven2ClasspathContainer( containerPath)) {
//      new Job("Resolving Dependencies") {
//
//        protected IStatus run( IProgressMonitor monitor ) {
          IFile pomFile = project.getProject().getFile( Maven2Plugin.POM_FILE_NAME);
          
          HashSet entrySet = new HashSet();
          HashSet moduleArtifacts = new HashSet();
          Maven2Plugin.getDefault().resolveClasspathEntries( entrySet, moduleArtifacts, pomFile, true, new NullProgressMonitor());
          
          IClasspathEntry[] entries = ( IClasspathEntry[]) entrySet.toArray( new IClasspathEntry[ entrySet.size()]);
          Maven2ClasspathContainer container = new Maven2ClasspathContainer( entries);
         
          try {
            JavaCore.setClasspathContainer( container.getPath(), new IJavaProject[] { project },
                new IClasspathContainer[] { container}, new NullProgressMonitor() );
//                new IClasspathContainer[] { container}, monitor );
          } catch( JavaModelException ex ) {
            Maven2Plugin.log(ex);
          }

//          return Status.OK_STATUS;
//        }
//      }.schedule();      
    }
  }

}

