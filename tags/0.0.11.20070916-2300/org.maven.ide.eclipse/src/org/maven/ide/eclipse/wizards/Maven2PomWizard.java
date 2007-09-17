
package org.maven.ide.eclipse.wizards;

/*
 * Licensed to the Codehaus Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.ByteArrayInputStream;
import java.io.File;
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
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.maven.ide.eclipse.Maven2Plugin;


/**
 * New POM wizard
 */
public class Maven2PomWizard extends Wizard implements INewWizard {
  private Maven2PomWizardPage artifactPage;
  private Maven2DependenciesWizardPage dependenciesPage;

  private ISelection selection;

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
    if(!resource.exists() || (resource.getType() & IResource.FOLDER | IResource.PROJECT) == 0) {
      // TODO show warning popup
      throwCoreException("Folder \"" + projectName + "\" does not exist.");
    }

    IContainer container = (IContainer) resource;
    final IFile file = container.getFile(new Path(Maven2Plugin.POM_FILE_NAME));
    if( file.exists()) {
      // TODO show warning popup
      throwCoreException( "POM already exists");
    }
    
    final File pom = file.getLocation().toFile();
        
    try {
      StringWriter w = new StringWriter();

      MavenEmbedder mavenEmbedder = Maven2Plugin.getDefault().getMavenEmbedderManager().getWorkspaceEmbedder();
      mavenEmbedder.writeModel(w, model, true);

      file.create( new ByteArrayInputStream( w.toString().getBytes( "ASCII" ) ), true, null );

      getShell().getDisplay().asyncExec(new Runnable() {
          public void run() {
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            try {
              IDE.openEditor(page, file, true);
            } catch(PartInitException e) {
            }
          }
        });
      
    } catch( Exception ex ) {
      Maven2Plugin.log( "Unable to create POM "+pom+"; "+ex.getMessage(), ex);
    
    }
  }
  
  private void throwCoreException(String message) throws CoreException {
    throw new CoreException(new Status(IStatus.ERROR, Maven2Plugin.PLUGIN_ID, IStatus.OK, message, null));
  }

  /**
   * We will accept the selection in the workbench to see if
   * we can initialize from it.
   * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
   */
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    this.selection = selection;
  }
    
}

