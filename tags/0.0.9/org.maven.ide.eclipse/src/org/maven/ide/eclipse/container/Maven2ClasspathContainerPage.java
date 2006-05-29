
package org.maven.ide.eclipse.container;

import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.maven.ide.eclipse.Maven2Plugin;


public class Maven2ClasspathContainerPage extends WizardPage 
    implements IClasspathContainerPage, IClasspathContainerPageExtension {

  private IJavaProject javaProject;
  private IClasspathEntry containerEntry;

  public Maven2ClasspathContainerPage() {
    super("Maven2 Contener");
  }
  
  public void initialize( IJavaProject javaProject, IClasspathEntry[] currentEntries) {
    this.javaProject = javaProject;
  }
  
  public boolean finish() {
    // TODO
    return true;
  }

  public IClasspathEntry getSelection() {
    // TODO
    // throw new RuntimeException( "Method getSelection not yet implemented");
    return this.containerEntry;
  }

  public void setSelection( IClasspathEntry containerEntry) {
    this.containerEntry = containerEntry==null ? createDefaultEntry() : containerEntry;
    
    // TODO
    // throw new RuntimeException( "Method setSelection not yet implemented");
  }

  private IClasspathEntry createDefaultEntry() {
    return JavaCore.newContainerEntry(new Path(Maven2Plugin.CONTAINER_ID));
  }

  public void createControl( Composite parent) {
    setTitle("Maven2 Managed Libraries");
    setDescription("Set the configuration details.");

    Composite control = new Composite(parent, SWT.NONE);

//    GridLayout layout = new GridLayout();
//    control.setLayout(layout);
//
//    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
//    control.setLayoutData(gd);
//    control.setFont(parent.getFont());
//
//    Composite config = createConfigControl(control);
//    gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
//    config.setLayoutData(gd);
//
//    Composite entries = createEntriesControl(control);
//    gd = new GridData(SWT.FILL, SWT.FILL, true, true);
//    entries.setLayoutData(gd);

    setControl(control);
  }

}

