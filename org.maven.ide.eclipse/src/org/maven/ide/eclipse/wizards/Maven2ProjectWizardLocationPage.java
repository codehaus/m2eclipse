package org.maven.ide.eclipse.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.maven.ide.eclipse.Messages;


/**
 * Wizard page responsible for gathering project information.
 *
 * This wizard page gathers information about the project to create. The user
 * must specify a project name and a location at which the project is to be
 * created. Input validation is performed in order to make sure that all the
 * project information is valid before letting the wizard continue.
 */
public class Maven2ProjectWizardLocationPage extends WizardPage {

  /** Text field holding the name of the project to create. */
  private Text projectNameText;

  /** Component to choose at which location to create the project. */
  private Maven2LocationComponent locationComponent;

  private Maven2ProjectWizardArtifactPage mavenArtifactPage;

  /**
   * Default constructor.
   *
   * Sets the title and description of this wizard page and marks it as not
   * being complete as user input is required for continuing.
   */
  public Maven2ProjectWizardLocationPage() {
    super( "Maven2ProjectWizardLocationPage" );
    setTitle( Messages.getString( "wizard.project.page.project.title" ) );
    setDescription( Messages.getString( "wizard.project.page.project.description" ) );
    setPageComplete( false );
  }
  
  public void setMavenArtifactPage( Maven2ProjectWizardArtifactPage mavenArtifactPage ) {
    this.mavenArtifactPage = mavenArtifactPage;
  }

  /**
   * {@inheritDoc}
   *
   * This wizard page contains a component to query the project name and a
   * <code>Maven2LocationComponent</code> which allows to specify whether the
   * project should be created in the workspace or at some given external
   * location.
   */
  public void createControl( Composite parent ) {
    Composite container = new Composite( parent, SWT.NULL );
    container.setLayout( new GridLayout(2, false) );

    ModifyListener modifyingListener = new ModifyListener() {
      public void modifyText( ModifyEvent e ) {
        validate();
      }
    };

    // project name
    GridData gridData = new GridData();
    gridData.horizontalIndent = 7;
    Label label = new Label( container, SWT.NULL );
    label.setLayoutData( gridData );
    label.setText( Messages.getString( "wizard.project.page.project.projectName" ) );
    projectNameText = new Text( container, SWT.BORDER | SWT.SINGLE );
    projectNameText.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
    projectNameText.addModifyListener( modifyingListener );

    // location group
    gridData = new GridData( SWT.FILL, SWT.TOP, false, false, 2, 1 );
    // gridData.verticalIndent = 5;
    locationComponent = new Maven2LocationComponent( container, SWT.NONE );
    locationComponent.setLayoutData( gridData );
    locationComponent.setModifyingListener( modifyingListener );

    initialize();

    setControl( container );
  }

  /**
   * Initializes the GUI components and validates the page.
   */
  private void initialize() {
    validate();
  }

  /**
   * Returns the name of the project to create as provided by the user.
   *
   * Note that any leading and trailing white space is preserved.
   *
   * @return  The name of the project to create. Is never <code>null</code>.
   */
  public String getProjectName() {
    return projectNameText.getText();
  }

  /**
   * Returns whether the user has chosen to create the project in the workspace
   * or at an external location.
   *
   * @return  <code>true</code> if the project is to be created in the workspace,
   *          <code>false</code> if it should be created at an external location.
   */
  public boolean isInWorkspace() {
    return locationComponent.isInWorkspace();
  }

  /**
   * Returns the path of the location where the project is to be created.
   *
   * According to the user input, the path either points to the workspace or
   * to a valid user specified location on the filesystem.
   *
   * @return  The path of the location where to create the project.
   *          Is never <code>null</code>.
   */
  public IPath getLocationPath() {
    return locationComponent.getLocationPath();
  }

  /**
   * Creates a project resource handle for the currently chosen project name.
   *
   * Note that this method does not create the actual project resource; this
   * should be done once the new project wizard has finished.
   *
   * @return  The resource handle for the project.
   *
   * @see #getProjectName()
   * @see org.eclipse.core.resources.IWorkspaceRoot#getProject(java.lang.String)
   */
  public IProject getProjectHandle() {
    return ResourcesPlugin.getWorkspace().getRoot().getProject( getProjectName() );
  }

  /** {@inheritDoc} */
  public void setVisible( boolean visible ) {
    super.setVisible( visible );

    if ( visible ) {
      projectNameText.setFocus();
    }
  }

  /**
   * Validates the contents of this wizard page.
   * <p>
   * Feedback about the validation is given to the user by displaying error
   * messages or informative messages on the wizard page. Depending on the
   * provided user input, the wizard page is marked as being complete or not.
   * <p>
   * If some error or missing input is detected in the user input, an error
   * message or informative message, respectively, is displayed to the user.
   * If the user input is complete and correct, the wizard page is marked as
   * begin complete to allow the wizard to proceed. To that end, the following
   * conditions must be met:
   * <ul>
   *   <li>The user must have provided a project name.</li>
   *   <li>The project name must be a valid project resource identifier.</li>
   *   <li>A project with the same name must not exist.</li>
   *   <li>A valid project location path must have been specified.</li>
   * </ul>
   * </p>
   *
   * @see org.eclipse.core.resources.IWorkspace#validateName(java.lang.String, int)
   * @see org.eclipse.core.resources.IWorkspace#validateProjectLocation(org.eclipse.core.resources.IProject, org.eclipse.core.runtime.IPath)
   * @see org.eclipse.jface.dialogs.DialogPage#setMessage(java.lang.String)
   * @see org.eclipse.jface.wizard.WizardPage#setErrorMessage(java.lang.String)
   * @see org.eclipse.jface.wizard.WizardPage#setPageComplete(boolean)
   */
  void validate() {
    final IWorkspace workspace = ResourcesPlugin.getWorkspace();

    final String name = getProjectName();

    // check wether the project name field is empty
    if ( name.trim().length() == 0 ) {
      setErrorMessage( null );
      setMessage( Messages.getString( "wizard.project.page.project.validator.projectName" ) );
      setPageComplete( false );
      return;
    }

    Maven2ArtifactComponent artifactComponent = mavenArtifactPage.getArtifactComponent();
    artifactComponent.setGroupId( name );
    artifactComponent.setArtifactId( name );
    
    // check whether the project name is valid
    final IStatus nameStatus = workspace.validateName( name, IResource.PROJECT );
    if ( !nameStatus.isOK() ) {
      setErrorMessage( nameStatus.getMessage() );
      setPageComplete( false );
      return;
    }

    // check whether project already exists
    final IProject handle = getProjectHandle();
    if ( handle.exists() ) {
      setErrorMessage( Messages.getString( "wizard.project.page.project.validator.projectExists" ) );
      setPageComplete( false );
      return;
    }

    final String location = locationComponent.getLocationPath().toOSString();

    // check whether location is empty
    if ( location.length() == 0 ) {
      setErrorMessage( null );
      setMessage( Messages.getString( "wizard.project.page.project.validator.projectLocation" ) );
      setPageComplete( false );
      return;
    }

    // check whether the location is a syntactically correct path
    if ( !Path.EMPTY.isValidPath( location ) ) {
      setErrorMessage( Messages.getString( "wizard.project.page.project.validator.invalidLocation" ) );
      setPageComplete( false );
      return;
    }

    // If we do not place the contents in the workspace validate the location.
    IPath projectPath = Path.fromOSString( location );
    if ( !locationComponent.isInWorkspace() ) {
      final IStatus locationStatus = workspace.validateProjectLocation( handle, projectPath );
      if( !locationStatus.isOK() ) {
        setErrorMessage( locationStatus.getMessage() );
        setPageComplete( false );
        return;
      }
    }

    setPageComplete( true );
    setErrorMessage( null );
    setMessage( null );
  }

}
