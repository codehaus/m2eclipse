
package org.maven.ide.eclipse.container;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.maven.ide.eclipse.Maven2Plugin;


/**
 * Maven2ClasspathContainerInitializer
 * 
 * @author Eugene Kuleshov
 */
public class Maven2ClasspathContainerInitializer extends ClasspathContainerInitializer {

  public void initialize( IPath containerPath, final IJavaProject project ) {
    if( Maven2ClasspathContainer.isMaven2ClasspathContainer( containerPath ) ) {
      IClasspathContainer container;
      try {
        container = JavaCore.getClasspathContainer(containerPath, project);
      } catch(JavaModelException ex) {
        Maven2Plugin.getDefault().getConsole().logError("Unable to get container for "+containerPath.toString()+"; "+ex.getMessage());
        return;
      }

      Maven2ClasspathContainer mavenContainer;
      if(container==null) {
        mavenContainer = new Maven2ClasspathContainer();
      } else {
        mavenContainer = new Maven2ClasspathContainer(new HashSet(Arrays.asList(container.getClasspathEntries())));
      }

      try {
        JavaCore.setClasspathContainer( containerPath, new IJavaProject[] { project },
              new IClasspathContainer[] { mavenContainer }, new NullProgressMonitor() );
      } catch(JavaModelException ex) {
        Maven2Plugin.getDefault().getConsole().logError("Unable to set container for "+containerPath.toString()+"; "+ex.getMessage());
        return;
      }

      if(container!=null) {
        return;
      }
      
      new Job( "Initializing "+project.getProject().getName() ) {
        protected IStatus run( IProgressMonitor monitor ) {
          IFile pomFile = project.getProject().getFile( Maven2Plugin.POM_FILE_NAME );

          monitor.beginTask( "Initializing", 2 );
          
          HashSet entries = new HashSet();
          HashSet moduleArtifacts = new HashSet();
          Maven2Plugin.getDefault().resolveClasspathEntries( entries, moduleArtifacts, pomFile, true,
              new SubProgressMonitor(monitor, 1, 0) );

          Maven2ClasspathContainer container = new Maven2ClasspathContainer( entries );
          try {
            JavaCore.setClasspathContainer( container.getPath(), new IJavaProject[] { project },
                new IClasspathContainer[] { container }, new SubProgressMonitor(monitor, 1, 0) );
          } catch( JavaModelException ex ) {
            Maven2Plugin.log( ex );
          }

          return Status.OK_STATUS;
        }
      }.schedule();
    }
  }
  
  public boolean canUpdateClasspathContainer( IPath containerPath, IJavaProject project ) {
    return Maven2ClasspathContainer.isMaven2ClasspathContainer( containerPath );
  }
  
  public void requestClasspathContainerUpdate( IPath containerPath, final IJavaProject project, final IClasspathContainer containerSuggestion ) throws CoreException {
    IClasspathContainer currentContainer = getMaven2ClasspathContainer(project);
    if(currentContainer==null) {
      Maven2Plugin.getDefault().getConsole().logError( "Unable to find Maven classpath container" );
      return;
    }
    
    IClasspathEntry[] newEntries = containerSuggestion.getClasspathEntries();
    for( int j = 0; j < newEntries.length; j++ ) {
      final IClasspathEntry entry = newEntries[j];
      IPath entryPath = entry.getPath();
      IClasspathEntry oldEntry = getClasspathentry(currentContainer, entryPath);
      if(oldEntry==null) {
        Maven2Plugin.getDefault().getConsole().logError( "Unable to find entry for "+entryPath );
        continue;
      }
      
      final IPath oldSrcPath = oldEntry.getSourceAttachmentPath();
      final IPath newSrcPath = entry.getSourceAttachmentPath();
      if(oldSrcPath==null ? newSrcPath!=null : !oldSrcPath.equals( newSrcPath )) {
        if(newSrcPath==null) {
          removeSourceBundle( oldSrcPath, containerSuggestion, project );
        } else {
          installSourceBundle( newSrcPath, entryPath, containerSuggestion, project );
        }
      }

      // TODO update/install javadoc
//      IClasspathAttribute[] oldAttributes = oldEntry.getExtraAttributes();
//      IClasspathAttribute[] newAttributes = entry.getExtraAttributes();
//      if(newAttributes!=null) {
//        if(oldAttributes==null || newAttributes.length!=oldAttributes.length) {
//          Maven2Plugin.getDefault().getConsole().logMessage( " old attributes: "+Arrays.asList( oldAttributes ) );
//          Maven2Plugin.getDefault().getConsole().logMessage( " new attributes: "+Arrays.asList( newAttributes ) );
//        }
//      }
    }
  }

  private void installSourceBundle( final IPath srcPath, IPath entryPath, final IClasspathContainer container, final IJavaProject project ) {
    String entryName = entryPath.lastSegment();
    if(entryName.endsWith( ".zip" ) || entryName.endsWith( ".jar" )) {            
      final File target = new File( entryPath.toFile().getParentFile(), entryName.substring( 0, entryName.length()-4 )+"-sources.jar");
      final Display display = Maven2Plugin.getStandardDisplay();
      display.asyncExec(new Runnable() {
          public void run() {
            boolean res = MessageDialog.openConfirm( 
                Maven2Plugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(), 
                "Install Source Bundle", "Install source bundle to "+target.getAbsolutePath());
            if(res) {
              try {
                FileUtils.copyFile( srcPath.toFile(), target );
                JavaCore.setClasspathContainer( container.getPath(), 
                    new IJavaProject[] { project }, 
                    new IClasspathContainer[] { container }, 
                    null );
              } catch( IOException ex ) {
                Maven2Plugin.getDefault().getConsole().logError( "Unable to copy "+srcPath.toFile().getAbsolutePath()+
                    " to "+target.getAbsolutePath()+"; "+ex.getMessage() );
              } catch( JavaModelException ex ) {
                Maven2Plugin.getDefault().getConsole().logError( ex.getMessage() );
              }
            }
          }
        });
      
    }
  }

  private void removeSourceBundle( final IPath srcPath, final IClasspathContainer container, final IJavaProject project ) {
    final Display display = Maven2Plugin.getStandardDisplay();
    display.asyncExec(new Runnable() {
        public void run() {
          if(MessageDialog.openConfirm( display.getActiveShell(), 
              "Delete Source Bundle", "Delete source bundle "+srcPath)) {
            srcPath.toFile().delete();
            try {
              JavaCore.setClasspathContainer( container.getPath(), 
                  new IJavaProject[] { project }, 
                  new IClasspathContainer[] { container }, 
                  null );
            } catch( JavaModelException ex ) {
              Maven2Plugin.getDefault().getConsole().logError( ex.getMessage() );
            }
          }                
        }
      });
  }

  private IClasspathEntry getClasspathentry( IClasspathContainer container, IPath path ) {
    IClasspathEntry[] entries = container.getClasspathEntries();
    for( int i = 0; i < entries.length; i++ ) {
      IClasspathEntry entry = entries[i];
      if(path.equals( entry.getPath() )) {
        return entry;
      }
    }
    return null;
  }

  public static IClasspathContainer getMaven2ClasspathContainer( IJavaProject project ) throws JavaModelException {
    return JavaCore.getClasspathContainer( new Path( Maven2Plugin.CONTAINER_ID), project );
  }

}
