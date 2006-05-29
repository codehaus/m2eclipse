
package org.maven.ide.eclipse.wizards;

import org.apache.maven.model.Model;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.maven.ide.eclipse.Maven2Plugin;

/**
 * TODO
 * 
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (pom.xml).
 */
public class Maven2PomWizardPage extends WizardPage {
  private Text projectText;

  private ISelection selection;

  private Maven2ArtifactComponent pomComponent;


  /**
   * Constructor for SampleNewWizardPage.
   * 
   * @param pageName
   */
  public Maven2PomWizardPage( ISelection selection) {
    super( "wizardPage");
    setTitle( "Maven2 POM");
    setDescription( "This wizard creates a new POM (pom.xml) descriptor for Maven2.");
    this.selection = selection;
  }

  /**
   * @see IDialogPage#createControl(Composite)
   */
  public void createControl( Composite parent) {
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    layout.verticalSpacing = 5;
    layout.makeColumnsEqualWidth = false;

    Composite container = new Composite( parent, SWT.NULL);
    container.setLayout( layout);

    ModifyListener modifyingListener = new ModifyListener() {
      public void modifyText( ModifyEvent e) {
        dialogChanged();
      }
    };

    GridData gridData = new GridData();
    gridData.horizontalIndent = 7;
    Label label = new Label( container, SWT.NULL);
    label.setLayoutData(gridData);
    label.setText( "&Project:");

    projectText = new Text( container, SWT.BORDER | SWT.SINGLE);
    projectText.setEditable(false);
    projectText.setLayoutData( new GridData( GridData.FILL_HORIZONTAL));
    projectText.addModifyListener( modifyingListener);

    Button button = new Button( container, SWT.PUSH);
    final GridData gridData_2 = new GridData();
    button.setLayoutData(gridData_2);
    button.setText( "Browse...");
    button.addSelectionListener( new SelectionAdapter() {
        public void widgetSelected( SelectionEvent e) {
          handleBrowse();
        }
      });

    gridData = new GridData( GridData.FILL, GridData.FILL, true, true, 3, 1);
    gridData.verticalIndent = 5;
    
    pomComponent = new Maven2ArtifactComponent(container, SWT.NONE, modifyingListener);
    pomComponent.setLayoutData(gridData);
    
    initialize();
    dialogChanged();
    setControl( container);
  }

  /**
   * Tests if the current workbench selection is a suitable container to use.
   */
  private void initialize() {
    if( selection != null && selection.isEmpty() == false && 
        selection instanceof IStructuredSelection) {
      IStructuredSelection ssel = ( IStructuredSelection) selection;
      if( ssel.size() > 1) {
        return;
      }
      Object obj = ssel.getFirstElement();
      if( obj instanceof IResource) {
        IContainer container;
        if( obj instanceof IContainer) {
          container = ( IContainer) obj;
        } else {
          container = ( ( IResource) obj).getParent();
        }
        projectText.setText( container.getFullPath().toString());
        pomComponent.setArtifactId( container.getName());
      }
    }
    
    pomComponent.setVersion( "0.0.1");
    pomComponent.setPackaging( "jar");
    // TODO
  }

  /**
   * Uses the standard container selection dialog to choose the new value for
   * the container field.
   */
  void handleBrowse() {
//    ContainerSelectionDialog dialog = new ContainerSelectionDialog( getShell(), 
//        ResourcesPlugin.getWorkspace().getRoot(), false, "Select project");
//    if( dialog.open()==Window.OK) {
//      Object[] result = dialog.getResult();
//      if( result.length==1) {
//        projectText.setText( ( ( Path) result[ 0]).toString());
//      }
//    }
    
    IJavaModel javaModel = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());

    IJavaProject[] projects;
    try {
      projects = javaModel.getJavaProjects();
    } catch (JavaModelException e) {
      Maven2Plugin.log(e);
      projects = new IJavaProject[0];
    }
    
    ILabelProvider labelProvider = new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
    ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), labelProvider);
    dialog.setTitle("Select Project"); 
    dialog.setMessage("Choose project where POM will be created"); 
    dialog.setElements(projects);
    
    IJavaProject javaProject = javaModel.getJavaProject( getProject());
    if (javaProject!=null) {
      dialog.setInitialSelections(new Object[] { javaProject });
    }
    
    if (dialog.open() == Window.OK) {     
      projectText.setText(((IJavaProject)dialog.getFirstResult()).getProject().getFullPath().toString());
    }     
  }

  /**
   * Ensures that both text fields are set.
   */
  void dialogChanged() {
    IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember( new Path( getProject()));

    if( getProject().length() == 0) {
      updateStatus( "Project must be specified");
      return;
    }
    if( container==null || ( container.getType() & IResource.PROJECT)==0) {
      updateStatus( "Project must exist");
      return;
    }
    if( !container.isAccessible()) {
      updateStatus( "Project must be writable");
      return;
    }

    // TODO
    if(pomComponent.getGroupId().length()==0) {
      updateStatus("Group Id must be specified");
    }
    
    if(pomComponent.getArtifactId().length()==0) {
      updateStatus("Artifact Id must be specified");
    }

    if(pomComponent.getVersion().length()==0) {
      updateStatus("Version must be specified");
    }
    
    if(pomComponent.getPackaging().length()==0) {
      updateStatus("Packaging must be specified");
    }
    
    updateStatus( null);
  }

  private void updateStatus( String message) {
    setErrorMessage( message);
    setPageComplete( message == null);
  }

  public String getProject() {
    return projectText.getText();
  }

  public Model getModel() {
    return pomComponent.getModel();
  }
  
}

