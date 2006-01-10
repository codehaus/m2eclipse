
package org.maven.ide.eclipse.builder;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.container.Maven2ClasspathContainer;


public class Maven2Builder extends IncrementalProjectBuilder {

  /*
   * (non-Javadoc)
   * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
   *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
   */
  protected IProject[] build( int kind, Map args, IProgressMonitor monitor) throws CoreException {
    IJavaProject project = JavaCore.create( getProject() );
    IClasspathEntry[] classPaths = project.getRawClasspath();

    for( int i = 0; i < classPaths.length && !monitor.isCanceled(); i++ ) {
      IClasspathEntry entry = classPaths[i];
      IPath path = entry.getPath();
      if( Maven2ClasspathContainer.isMaven2ClasspathContainer( path ) ) {
        IFile pomFile = project.getProject().getFile( Maven2Plugin.POM_FILE_NAME);

        Set entrySet = new HashSet();
        Maven2Plugin.getDefault().resolveClasspathEntries( entrySet, pomFile, true, monitor );
        
        IClasspathEntry[] entries = ( IClasspathEntry[]) entrySet.toArray( new IClasspathEntry[ entrySet.size()]);
        
        Arrays.sort( entries, new Comparator() {
            public int compare( Object o1, Object o2) {
              return o1.toString().compareTo( o2.toString());
            }
          } );
        
        Maven2ClasspathContainer container = ( Maven2ClasspathContainer ) JavaCore.getClasspathContainer( path, project );
        
        JavaCore.setClasspathContainer( container.getPath(), new IJavaProject[] { project },
            new IClasspathContainer[] { new Maven2ClasspathContainer( entries)}, monitor );
      }
    }
    
    return null;
  }

}

