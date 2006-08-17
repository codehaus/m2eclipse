
package org.maven.ide.eclipse.wizards;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.model.Model;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.MavenEmbedderCallback;


/**
 * TODO
 * 
 * This is a sample new wizard. Its role is to create a new file 
 * resource in the provided container. If the container resource
 * (a folder or a project) is selected in the workspace 
 * when the wizard is opened, it will accept it as the target
 * container. The wizard creates one file with the extension
 * "pom.xml". If a sample multi-page editor (also available
 * as a template) is registered for the same extension, it will
 * be able to open it.
 */
public class Maven2PomWizard extends Wizard implements INewWizard {
	private Maven2PomWizardPage artifactPage;
	private Maven2DependenciesWizardPage dependenciesPage;

  private ISelection selection;
  private IWorkbench workbench;

	/**
	 * Constructor for Maven2PomWizard.
	 */
	public Maven2PomWizard() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		artifactPage = new Maven2PomWizardPage(selection);
		dependenciesPage = new Maven2DependenciesWizardPage();
		
    addPage(artifactPage);
    addPage(dependenciesPage);
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	public boolean performFinish() {
    final String projectName = artifactPage.getProject();
    final Model model = artifactPage.getModel();
    model.getDependencies().addAll( Arrays.asList( dependenciesPage.getDependencies() ) );

    IRunnableWithProgress op = new IRunnableWithProgress() {
      public void run( IProgressMonitor monitor ) throws InvocationTargetException {
        monitor.beginTask( "Creating POM", 1 );
        try {
          doFinish( projectName, model, monitor );
          monitor.worked( 1 );
        } catch( CoreException e ) {
          throw new InvocationTargetException( e );
        } finally {
          monitor.done();
        }
      }
    };

    try {
      getContainer().run( true, false, op );
    } catch( InterruptedException e ) {
      return false;
    } catch( InvocationTargetException e ) {
      Throwable realException = e.getTargetException();
      MessageDialog.openError( getShell(), "Error", realException.getMessage() );
      return false;
    }
    return true;
  }
	
	/**
	 * The worker method. It will find the container, create the
	 * file if missing or just replace its contents, and open
	 * the editor on the newly created file.
	 */
	void doFinish( String projectName, final Model model, IProgressMonitor monitor) throws CoreException {
		// monitor.beginTask("Creating " + fileName, 2);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(projectName));
		if( !resource.exists() || resource.getType()!=IResource.FOLDER ) {
      // TODO show warning popup
      throwCoreException( "Folder \"" + projectName + "\" does not exist." );
    }

		IContainer container = (IContainer) resource;
		final IFile file = container.getFile(new Path(Maven2Plugin.POM_FILE_NAME));
		if( file.exists()) {
      // TODO show warning popup
		  throwCoreException( "POM already exists");
    }
    
    final File pom = file.getLocation().toFile();
        
    try {
      final StringWriter w = new StringWriter();
      Maven2Plugin.getDefault().executeInEmbedder(new MavenEmbedderCallback() {
        public Object run( MavenEmbedder mavenEmbedder, IProgressMonitor monitor ) {
          try {
            mavenEmbedder.writeModel( w, model );
          } 
          catch( IOException ex ) {
            Maven2Plugin.log( "Unable to write POM "+pom+"; "+ex.getMessage(), ex);
          }
          return null;
        }
      }, new NullProgressMonitor());
      

      file.create( new ByteArrayInputStream( w.toString().getBytes( "ASCII" ) ), true, null );
//    monitor.worked(1);
      
//    monitor.setTaskName("Opening file for editing...");
      getShell().getDisplay().asyncExec( new Runnable() {
        public void run() {
          IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
          try {
            IDE.openEditor( page, file, true );
          } catch( PartInitException e ) {
          }
        }
      } );
//    monitor.worked(1);
      
    } catch( Exception ex ) {
      Maven2Plugin.log( "Unable to create POM "+pom+"; "+ex.getMessage(), ex);
    
    }
	}
	
	private void throwCoreException(String message) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, "org.maven.ide.eclipse", IStatus.OK, message, null);
		throw new CoreException(status);
	}

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
    this.selection = selection;
	}
    
}

