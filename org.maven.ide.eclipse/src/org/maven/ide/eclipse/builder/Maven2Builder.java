
package org.maven.ide.eclipse.builder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathContainer;
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
  protected IProject[] build( int kind, Map args, IProgressMonitor monitor ) throws CoreException {
    IProject project = getProject();
    if( project.getNature( Maven2Plugin.NATURE_ID ) == null )
      return null;

    // if( kind == AUTO_BUILD || kind == INCREMENTAL_BUILD ) {
    Verifier verifier = new Verifier();
    IResourceDelta delta = getDelta(project);
    if(delta == null) {
      getProject().accept(verifier, IResource.DEPTH_INFINITE, IContainer.EXCLUDE_DERIVED);
    } else {
      delta.accept(verifier, IContainer.EXCLUDE_DERIVED);
    }
    if(!verifier.updated) {
      return null;
    }

    updateClasspath( monitor, project );

    return null;
  }

  private void updateClasspath( IProgressMonitor monitor, IProject project ) throws JavaModelException {
//    IClasspathEntry[] classPaths = javaProject.getRawClasspath();
//    for( int i = 0; i < classPaths.length && !monitor.isCanceled(); i++ ) {
//      IClasspathEntry entry = classPaths[i];
//      if( Maven2ClasspathContainer.isMaven2ClasspathContainer( entry.getPath() ) ) {

    Set entries = new HashSet();
    Set moduleArtifacts = new HashSet();
    IFile pomFile = project.getFile( Maven2Plugin.POM_FILE_NAME );
    Maven2Plugin.getDefault().resolveClasspathEntries( entries, moduleArtifacts, pomFile, true, monitor );

    Maven2ClasspathContainer container = new Maven2ClasspathContainer( entries );
    JavaCore.setClasspathContainer( container.getPath(), new IJavaProject[] { JavaCore.create( project ) },
        new IClasspathContainer[] { container }, monitor );

//      }
//    }
  }

  static final class Verifier implements IResourceDeltaVisitor, IResourceVisitor {
    boolean updated;

    public boolean visit( IResourceDelta delta ) {
      IResource resource = delta.getResource();
      return visit(resource);
    }

    public boolean visit(IResource resource) {
      if( resource.getType()==IResource.FILE && Maven2Plugin.POM_FILE_NAME.equals( resource.getName() ) ) {
        updated = true;
        
        // update model cache 
        Maven2Plugin plugin = Maven2Plugin.getDefault();
        try {
          plugin.getMavenModelManager().updateMavenModel((IFile) resource);
          plugin.deleteMarkers(resource);
        } catch( CoreException ex ) {
          // TODO ignore or add resource marker for the failure
        }
      }
      return true;
    }

  }

}
