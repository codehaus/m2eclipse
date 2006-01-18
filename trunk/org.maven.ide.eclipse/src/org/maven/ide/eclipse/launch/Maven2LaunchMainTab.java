
package org.maven.ide.eclipse.launch;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.plugin.descriptor.MojoDescriptor;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.MavenEmbedderCallback;
import org.maven.ide.eclipse.Messages;
import org.maven.ide.eclipse.util.ITraceable;
import org.maven.ide.eclipse.util.Tracer;

/**
 * Maven Launch dialog Main tab 
 * 
 * @author Dmitri Maximovich
 */
public class Maven2LaunchMainTab extends AbstractLaunchConfigurationTab implements Maven2LaunchConstants, ITraceable {
  private static final boolean TRACE_ENABLED = Boolean.valueOf(Platform.getDebugOption("org.maven.ide.eclipse/launcher")).booleanValue();

  protected Text fPomDirName;
  protected Text fGoals;
  protected Table tProps;

  public boolean isTraceEnabled() {
    return TRACE_ENABLED;
  }

  public Image getImage() {
    final URL rootURL = Maven2Plugin.getDefault().getRootURL(); 
    try {
      ImageDescriptor descriptor = ImageDescriptor.createFromURL(new URL(rootURL, "icons/main_tab.gif")); //$NON-NLS-1$
      return descriptor.createImage();
    } 
    catch( MalformedURLException ex ) {
      ex.printStackTrace();
    }
    return null;
  }
    
  public void createControl(Composite parent) {
        Composite mainComposite = new Composite(parent, SWT.NONE);
        setControl(mainComposite);
        //PlatformUI.getWorkbench().getHelpSystem().setHelp(mainComposite, IAntUIHelpContextIds.ANT_MAIN_TAB);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        mainComposite.setLayout(layout);
        mainComposite.setLayoutData(gridData);
        mainComposite.setFont(parent.getFont());

    // pom file 
		final Group pomGroup = new Group(mainComposite, SWT.NONE);
		pomGroup.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
		pomGroup.setText(Messages.getString("launch.pomGroup")); //$NON-NLS-1$
		pomGroup.setLayout(new GridLayout());

		this.fPomDirName = new Text(pomGroup, SWT.BORDER);
		this.fPomDirName.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
    this.fPomDirName.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        entriesChanged();
      }
    });
        

		final Composite pomDirButtonsComposite = new Composite(pomGroup, SWT.NONE);
		pomDirButtonsComposite.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
		final GridLayout pomDirButtonsGridLayout = new GridLayout();
		pomDirButtonsGridLayout.numColumns = 3;
		pomDirButtonsComposite.setLayout(pomDirButtonsGridLayout);

		final Button browseWorkspaceButton = new Button(pomDirButtonsComposite, SWT.NONE);
		browseWorkspaceButton.setText(Messages.getString("launch.browseWorkspace")); //$NON-NLS-1$
		browseWorkspaceButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), ResourcesPlugin.getWorkspace().getRoot(), false, Messages.getString("launch.choosePomDir")); //$NON-NLS-1$
        int buttonId = dialog.open();
        if (buttonId == IDialogConstants.OK_ID) {
          Object[] resource = dialog.getResult();
          if (resource != null && resource.length > 0) {
            String fileLoc = VariablesPlugin.getDefault().getStringVariableManager().generateVariableExpression("workspace_loc", ((IPath)resource[0]).toString()); //$NON-NLS-1$
            fPomDirName.setText(fileLoc);
            entriesChanged();
          }
        }
      }
		});

		final Button browseFilesystemButton = new Button(pomDirButtonsComposite, SWT.NONE);
		browseFilesystemButton.setText(Messages.getString("launch.browseFs")); //$NON-NLS-1$
		browseFilesystemButton.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.NONE);
                dialog.setFilterPath(fPomDirName.getText());
                String text= dialog.open();
                if (text != null) {
                    fPomDirName.setText(text);
                    entriesChanged();
                }
            }
		});

		final Button browseVariablesButton = new Button(pomDirButtonsComposite, SWT.NONE);
		browseVariablesButton.setText(Messages.getString("launch.browseVariables")); //$NON-NLS-1$
		browseVariablesButton.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e) {
                StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
                dialog.open();
                String variable =  dialog.getVariableExpression();
                if (variable != null) {
                    fPomDirName.insert(variable);
                }
            }
		});

		// goals
		final Group goalsGroup = new Group(mainComposite, SWT.NONE);
		goalsGroup.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
		final GridLayout targetsGridLayout = new GridLayout();
		goalsGroup.setLayout(targetsGridLayout);
		goalsGroup.setText(Messages.getString("launch.goalsGroup")); //$NON-NLS-1$

		this.fGoals = new Text(goalsGroup, SWT.BORDER);
		this.fGoals.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
      this.fGoals.addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
          // TODO ignore caret movement keys which doesn't actually change data
          entriesChanged();
        }
    });
		
    // need to remember if focus was ever set to that field to use it while inserting new goals
    this.fGoals.addFocusListener(new FocusAdapter() {
      public void focusGained( FocusEvent e ) {
        super.focusGained(e);
        fGoals.setData("focus");
      }
    });  
      
		final Button browseGoalsButton = new Button(goalsGroup, SWT.NONE);
		browseGoalsButton.setLayoutData(new GridData());
		browseGoalsButton.setText(Messages.getString("launch.goals")); //$NON-NLS-1$
		browseGoalsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String fileName = Maven2Plugin.substituteVar(fPomDirName.getText());
				if (!isDirectoryExist(fileName)) {
					MessageDialog.openError(getShell(), Messages.getString("launch.errorPomMissing"), 
              Messages.getString("launch.errorSelectPom")); //$NON-NLS-1$ //$NON-NLS-2$
					return;
				}
        final Maven2GoalSelectionDialog dialog = new Maven2GoalSelectionDialog(getShell(), Messages.getString("launch.goalsDialog.title"));  //$NON-NLS-1$
        dialog.setAllowMultiple(false);
        
        Object res = Maven2Plugin.getDefault().executeInEmbedder("Launch Configuration", new MavenEmbedderCallback() {
            public Object doInEmbedder( MavenEmbedder mavenEmbedder, IProgressMonitor monitor ) {
              dialog.setInput(mavenEmbedder);
              return new Integer(dialog.open());
            }
          });
        int rc = ((Integer) res).intValue();
        if (rc == IDialogConstants.OK_ID) {
            Object[] o = dialog.getResult();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < o.length; i++) {
              if (o[i] instanceof Maven2GoalSelectionDialog.LifecyclePhase) {
                sb.append(((Maven2GoalSelectionDialog.LifecyclePhase)o[i]).getName()).append(' ');
              }
              else if (o[i] instanceof MojoDescriptor) {
                MojoDescriptor mojoDescriptor = (MojoDescriptor)o[i];
                sb.append(mojoDescriptor.getFullGoalName()).append(' ');
              }
            }
            if (sb.charAt(sb.length()-1) == ' ') {
              sb.deleteCharAt(sb.length()-1);
            }
            //setNewGoals(fGoals, sb.toString());
            fGoals.insert(sb.toString());
            entriesChanged();
        }
      }

      // fancy insert into fGoals field
      private void setNewGoals(Text field, String newText) {
        final boolean hadFocus =  fGoals.getData() != null;
        final int currPos = field.getCaretPosition();
        final String oldText = field.getText();

        if (!hadFocus) {
          // never had focus - add to the end
          if (oldText.length() > 0 && !oldText.endsWith(" ")) {
            newText = " " + newText;
          }
          field.setText(oldText+newText);
          return;
        }
        
        // had focus before
        
        // caret at the first position
        if (currPos == 0) {
          if (oldText.charAt(0) != ' ') {
            field.insert(newText+" ");
            return;
          }
          else {
            field.insert(newText);
            return;
          }
        }
      
        // caret at the last position
        if (currPos == oldText.length()) {
          if (!oldText.endsWith(" ")) {
            field.insert(" "+newText);
            return;
          }
          else {
            field.insert(newText);
            return;
          }
        }
        
        // caret somewhere in the middle
        if (oldText.charAt(currPos) != ' ') {
          newText = newText + " ";
        }
        if (oldText.charAt(currPos-1) != ' ') {
          newText = " " + newText;
        }
        field.insert(newText);
    }
      
		});
    
		// properties
		final Composite propsComposite = new Composite(mainComposite, SWT.NONE);
		propsComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		final GridLayout propsGridLayout = new GridLayout();
		propsGridLayout.marginWidth = 0;
		propsGridLayout.horizontalSpacing = 0;
		propsGridLayout.numColumns = 2;
		propsComposite.setLayout(propsGridLayout);

		this.tProps = new Table(propsComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		//this.tProps.setItemCount(10);
		this.tProps.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		this.tProps.setLinesVisible(true);
		this.tProps.setHeaderVisible(true);
    TableViewer tableViewer= new TableViewer(this.tProps);
    tableViewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        TableItem[] selection = tProps.getSelection();
        if (selection.length == 1) {
          editProperty(selection[0].getText(0), selection[0].getText(1));
        }
      }
    });

		final TableColumn propColumn = new TableColumn(this.tProps, SWT.NONE, 0);
		propColumn.setWidth(100);
		propColumn.setText(Messages.getString("launch.propName")); //$NON-NLS-1$

		final TableColumn valueColumn = new TableColumn(this.tProps, SWT.NONE, 1);
		valueColumn.setWidth(200);
		valueColumn.setText(Messages.getString("launch.propValue")); //$NON-NLS-1$

		final Composite propsButtonsComposite = new Composite(propsComposite, SWT.NONE);
		propsButtonsComposite.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false));
		final GridLayout propsButtonsGridLayout = new GridLayout();
		propsButtonsGridLayout.marginHeight = 0;
		propsButtonsGridLayout.marginLeft = 5;
		propsButtonsGridLayout.marginWidth = 0;
		propsButtonsComposite.setLayout(propsButtonsGridLayout);

		final Button addPropButton = new Button(propsButtonsComposite, SWT.NONE);
		addPropButton.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
		addPropButton.setText(Messages.getString("launch.propAddButton")); //$NON-NLS-1$
    addPropButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        addProperty();
      }
    });

		final Button removePropButton = new Button(propsButtonsComposite, SWT.NONE);
		removePropButton.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
		removePropButton.setText(Messages.getString("launch.propRemoveButton")); //$NON-NLS-1$
    removePropButton.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected( SelectionEvent e ) {
          if (tProps.getSelectionCount() > 0) {
            tProps.remove(tProps.getSelectionIndices());
            entriesChanged();
          }
        }
      });
    }

    void addProperty() {
      Maven2PropertyAddDialog dialog = new Maven2PropertyAddDialog(getShell(), Messages.getString("launch.propAddDialogTitle"), new String[]{}); //$NON-NLS-1$
      int buttonId = dialog.open();
      
      if (buttonId == IDialogConstants.OK_ID) {
        String[] result = dialog.getNameValuePair();
        TableItem item = new TableItem(tProps, SWT.NONE);
        item.setText(0, result[0]);
        item.setText(1, result[1]);
        entriesChanged();
      }
    }

    void editProperty(String name, String value) {
      Maven2PropertyAddDialog dialog = new Maven2PropertyAddDialog(getShell(), Messages.getString("launch.propEditDialogTitle"), new String[]{name, value}); //$NON-NLS-1$
      int buttonId = dialog.open();
      
      if (buttonId == IDialogConstants.OK_ID) {
        String[] result = dialog.getNameValuePair();
        TableItem[] item = tProps.getSelection();
        // we expect only one row selected
        item[0].setText(0, result[0]);
        item[0].setText(1, result[1]);
        entriesChanged();
      }
    }
    
    public void initializeFrom(ILaunchConfiguration configuration) {
        try {
            this.fPomDirName.setText(configuration.getAttribute(ATTR_POM_DIR, "")); //$NON-NLS-1$
        } 
        catch (CoreException e) {
        }
        try {
            this.fGoals.setText(configuration.getAttribute(ATTR_GOALS, "")); //$NON-NLS-1$
        } 
        catch (CoreException e) {
        }
        try {
          tProps.removeAll();
          List properties = configuration.getAttribute(ATTR_PROPERTIES, Collections.EMPTY_LIST);
          for( Iterator iter = properties.iterator(); iter.hasNext(); ) {
            String s = (String)iter.next();
            try {
              String[] ss = s.split("="); //$NON-NLS-1$
              TableItem item = new TableItem(tProps, SWT.NONE);
              item.setText(0, ss[0]);
              if (ss.length > 1) {
                item.setText(1, ss[1]);
              }
            }
            catch (Exception e) {
              String msg = "Error parsing argument: "+s; //$NON-NLS-1$
              Maven2Plugin.log(msg, e);
            }
          }
        } 
        catch( CoreException ex ) {
        }
        setDirty(false);
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    }

    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
      configuration.setAttribute(ATTR_POM_DIR, this.fPomDirName.getText());
      configuration.setAttribute(ATTR_GOALS, this.fGoals.getText());
      Tracer.trace(this, "pomDirName", this.fPomDirName.getText());
      Tracer.trace(this, "goals", this.fGoals.getText());
        
      TableItem[] items = this.tProps.getItems();
      // store as String in "param=value" format 
      List properties = new ArrayList(); 
      for( int i = 0; i < items.length; i++ ) {
        String p = items[i].getText(0);
        String v = items[i].getText(1);
        if (p != null && p.trim().length() > 0) {
          String prop = p.trim()+"="+(v == null ? "" : v);  //$NON-NLS-1$ //$NON-NLS-2$
          properties.add(prop);
          Tracer.trace(this, "property", prop);
        }
      }
      configuration.setAttribute(ATTR_PROPERTIES, properties);
    }

    public String getName() {
        return Messages.getString("launch.mainTabName"); //$NON-NLS-1$
    }

    public boolean isValid(ILaunchConfiguration launchConfig) {
        setErrorMessage(null);
        
        String pomFileName = this.fPomDirName.getText();
        if (pomFileName == null || pomFileName.trim().length() == 0) {
            setErrorMessage(Messages.getString("launch.pomDirectoryEmpty"));
            return false;
        }
        if (!isDirectoryExist(pomFileName)) {
            setErrorMessage(Messages.getString("launch.pomDirectoryDoesntExist"));
            return false;
        }
        return true;
    }

    protected boolean isDirectoryExist(String name) {
    	if (name == null || name.trim().length() == 0) {
    		return false;
    	}
		  File pomDir = new File(Maven2Plugin.substituteVar(name));
		  if (!pomDir.exists()) {
  			return false;
		  }
      if (!pomDir.isDirectory()) {
        return false;
      }
      	return true;
    }

    void entriesChanged() {
        setDirty(true);
        updateLaunchConfigurationDialog();
    }

}
