package org.maven.ide.eclipse.wizards;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.maven.ide.eclipse.Messages;


/**
 * Simple GUI component which allows the user to choose between a workspace
 * location and a user specified external location.
 *
 * This component is mainly used for choosing the location at which to create
 * a new project.
 */
public class Maven2LocationComponent extends Composite {

  /** Radio button indicating whether the workspace location has been chosen. */
  private Button inWorkspaceButton;

  /** Radio button indicating whether an external location has been chosen. */
  private Button inExternalLocationButton;

  /** Text field for defining a user specified external location. */
  private Text locationText;

  /** Button allowing to choose a directory on the file system as the external location. */
  private Button locationBrowseButton;

  private ModifyListener modifyingListener;

  private Label locationLabel;

  /**
   * Constructor.
   *
   * Constructs all the GUI components contained in this <code>Composite</code>.
   * These components allow the user to choose between a workspace location and
   * a user specified external location.
   *
   * @param parent             The widget which will be the parent of this component.
   * @param styles             The widget style for this component.
   * @param modifyingListener  Listener which is notified when the contents of
   *                           this component change due to user input.
   */
  public Maven2LocationComponent( final Composite parent, int styles ) {
    super( parent, styles );

    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 3;
    gridLayout.marginLeft = 0;
    setLayout( gridLayout );

    Group locationGroup = new Group( this, SWT.NONE );
    locationGroup.setText( Messages.getString( "locationComponent.location" ) );
    locationGroup.setLayoutData( new GridData( GridData.FILL, GridData.FILL, true, true, 3, 1 ) );
    locationGroup.setLayout( gridLayout );

    GridData gridData = new GridData();
    gridData.horizontalSpan = 3;

    // first radio button
    inWorkspaceButton = new Button( locationGroup, SWT.RADIO );
    inWorkspaceButton.setText( Messages.getString( "locationComponent.inWorkspace" ) );
    inWorkspaceButton.setLayoutData( gridData );
    inWorkspaceButton.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        boolean isEnabled = !inWorkspaceButton.getSelection();
        locationLabel.setEnabled( isEnabled );
        locationText.setEnabled( isEnabled );
        locationBrowseButton.setEnabled( isEnabled );
        if(modifyingListener!=null) {
          modifyingListener.modifyText( null );
        }
      }
    } );

    // second radio button
    inExternalLocationButton = new Button( locationGroup, SWT.RADIO );
    inExternalLocationButton.setText( Messages.getString( "locationComponent.atExternal" ) );
    inExternalLocationButton.setLayoutData( gridData );

    // choose directory
    locationLabel = new Label( locationGroup, SWT.NONE );
    locationLabel.setText( Messages.getString( "locationComponent.directory" ) );

    locationText = new Text( locationGroup, SWT.BORDER );
    locationText.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );

    locationBrowseButton = new Button( locationGroup, SWT.PUSH );
    locationBrowseButton.setText( Messages.getString( "locationComponent.browse" ) );

    gridData = new GridData( SWT.FILL, SWT.DEFAULT, false, false );
    locationBrowseButton.setLayoutData( gridData );

    locationBrowseButton.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        String directory = new DirectoryDialog( getShell(), SWT.OPEN ).open();
        if ( directory != null ) {
          locationText.setText( directory.trim() );
        }
      }
    } );

    inWorkspaceButton.setSelection( true );
    
    locationLabel.setEnabled( false );
    locationText.setEnabled( false );
    locationBrowseButton.setEnabled( false );
  }

  /**
   * Returns the path of the location chosen by the user.
   *
   * According to the user input, the path either points to the workspace or
   * to a valid user specified location on the filesystem.
   *
   * @return  The path of the location chosen by the user.
   *          Is never <code>null</code>.
   */
  public IPath getLocationPath() {
    if ( isInWorkspace() ) {
      return Platform.getLocation();
    }
    return Path.fromOSString( locationText.getText().trim() );
  }

  /**
   * Returns whether the workspace has been chosen as the location to use.
   *
   * @return  <code>true</code> if the workspace is chosen as the location to use,
   *          <code>false</code> if the specified external location is to be used.
   */
  public boolean isInWorkspace() {
    return inWorkspaceButton.getSelection();
  }
  
  public void setModifyingListener( ModifyListener modifyingListener ) {
    this.modifyingListener = modifyingListener;
    locationText.addModifyListener( modifyingListener );
  }
  
  public void dispose() {
    super.dispose();
    if(modifyingListener!=null) {
      locationText.removeModifyListener( modifyingListener );
    }
  }
}
