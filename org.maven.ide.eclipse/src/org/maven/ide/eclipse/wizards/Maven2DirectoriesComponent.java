package org.maven.ide.eclipse.wizards;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.maven.ide.eclipse.Messages;


/**
 * Simple GUI component which allows the user to choose a set of directories
 * from the default Maven2 directory structure.
 *
 * This component is mainly used for choosing which of the directories of the
 * default Maven2 directory structure to create along with a newly created
 * project.
 */
public class Maven2DirectoriesComponent extends Composite {

  /** All the directories constituing the default Maven2 directory structure. */
  private static Maven2Directory[] defaultMaven2Directories = {
    new Maven2Directory( "src/main/java", "target/classes", true ),
    new Maven2Directory( "src/main/resources", "target/classes", true ),
    new Maven2Directory( "src/main/filters", null, false ),
    new Maven2Directory( "src/main/assembly", null, false ),
    new Maven2Directory( "src/main/config", null, false ),
    new Maven2Directory( "src/main/webapp", null, false ),
    new Maven2Directory( "src/test/java", "target/test-classes", true ),
    new Maven2Directory( "src/test/resources", "target/test-classes", true ),
    new Maven2Directory( "src/test/filters", null, false ),
    new Maven2Directory( "src/site", null, false ),
  };

  /** The set of directories currently selected by the user. */
  Set directories = new LinkedHashSet();

  /**
   * Constructor.
   *
   * Constructs all the GUI components contained in this <code>Composite</code>.
   * These components allow the user to choose a subset of all the directories
   * of the default Maven2 directory structure.
   *
   * @param parent  The widget which will be the parent of this component.
   * @param styles  The widget style for this component.
   */
  public Maven2DirectoriesComponent( Composite parent, int styles ) {
    super( parent, styles );

    setLayout( new GridLayout() );

    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 3;
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;

    GridData gridData = new GridData( GridData.FILL, GridData.FILL, true, true, 1, 1 );

    Group group = new Group( this, SWT.NONE );
    group.setText( Messages.getString( "directoriesComponent.projectLayout" ) );
    group.setLayout( gridLayout );
    group.setLayoutData( gridData );

    DirectorySelectionListener directorySelectionListener = new DirectorySelectionListener();

    // Add checkboxes for all the directories.
    for ( int i = 0; i < defaultMaven2Directories.length; i++ ) {
      Maven2Directory directory = defaultMaven2Directories[i];

      Button directoryButton = new Button( group, SWT.CHECK );
      directoryButton.setText( directory.getPath() );
      directoryButton.addSelectionListener( directorySelectionListener );
      directoryButton.setData( directory );
      if(directory.isDefault()) {
        directoryButton.setSelection( true );
        directories.add( directory );
      }
    }
  }

  /**
   * Returns all the Maven2 directories currently selected by the user.
   *
   * @return  All the Maven2 directories currently selected by the user.
   *          Neither the array nor any of its elements is <code>null</code>.
   */
  public Maven2Directory[] getDirectories() {
    return ( Maven2Directory[] ) directories.toArray( new Maven2Directory[directories.size()] );
  }


  class DirectorySelectionListener extends SelectionAdapter {

    public void widgetSelected( SelectionEvent e ) {
      Button button = ((Button) e.getSource());
      if ( button.getSelection() ) {
        directories.add( button.getData() );
      } else {
        directories.remove( button.getData() );
      }
    }
  }


}
