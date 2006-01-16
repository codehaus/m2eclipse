
package org.maven.ide.eclipse.builder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.container.Maven2ClasspathContainer;


public class Maven2Builder extends IncrementalProjectBuilder {

  /*
   * (non-Javadoc)
   * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
   *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
   */
  protected IProject[] build( int kind, Map args, IProgressMonitor monitor) throws CoreException {
    IProject project = getProject();
    if(project.getNature( Maven2Plugin.NATURE_ID )==null) return null;
    
    if(kind==AUTO_BUILD || kind==INCREMENTAL_BUILD) {
      Verifier verifier = new Verifier();
      getDelta( project ).accept( verifier, IContainer.EXCLUDE_DERIVED );
      if(!verifier.updated) {
        return null;
      }
    }
    
    updateClasspath( monitor, project );
    
    return null;
  }

  private void updateClasspath( IProgressMonitor monitor, IProject project ) throws JavaModelException {
    IJavaProject javaProject = JavaCore.create( project );
    IClasspathEntry[] classPaths = javaProject.getRawClasspath();
    for( int i = 0; i < classPaths.length && !monitor.isCanceled(); i++ ) {
      IClasspathEntry entry = classPaths[i];
      IPath path = entry.getPath();
      if( Maven2ClasspathContainer.isMaven2ClasspathContainer( path ) ) {
        IFile pomFile = javaProject.getProject().getFile( Maven2Plugin.POM_FILE_NAME);

        Set entries = new HashSet();
        Set moduleArtifacts = new HashSet();
        Maven2Plugin.getDefault().resolveClasspathEntries( entries, moduleArtifacts, pomFile, true, monitor );
        
        Maven2ClasspathContainer container = ( Maven2ClasspathContainer ) JavaCore.getClasspathContainer( path, javaProject );
        container.setEntries( entries );
        JavaCore.setClasspathContainer( container.getPath(), new IJavaProject[] { javaProject },
            new IClasspathContainer[] { container}, monitor );
      }
    }
  }

  
  private static final class Verifier implements IResourceDeltaVisitor {
    boolean updated;

    public boolean visit( IResourceDelta delta ) {
      if(Maven2Plugin.POM_FILE_NAME.equals(delta.getResource().getName())) {
        updated = true;
      }
      return !updated;  // finish earlier if at least one pom found
    }

  }
  
}

