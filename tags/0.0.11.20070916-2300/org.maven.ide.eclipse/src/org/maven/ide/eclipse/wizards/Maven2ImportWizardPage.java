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

package org.maven.ide.eclipse.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.model.Model;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.maven.ide.eclipse.Maven2Plugin;


/**
 * Maven2ImportWizardPage
 * 
 * @author Eugene Kuleshov
 */
public class Maven2ImportWizardPage extends WizardPage {

  Text rootDirectoryText;

  CheckboxTreeViewer projectTreeViewer;

  private Button projectsForModules;

  private Button resolveWorkspaceProjects;

  private Text activeProfiles;

  protected Maven2ImportWizardPage() {
    super("Maven2ImportPage");
    setTitle("Import Maven projects");
    setDescription("Import Maven projects to the Workspace.");
    setPageComplete(false);
  }

  public void createControl(Composite parent) {
    FormToolkit toolkit = new FormToolkit(Display.getCurrent());

    final Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(3, false));
    setControl(composite);

    final Label selectRootDirectoryLabel = new Label(composite, SWT.NONE);
    selectRootDirectoryLabel.setLayoutData(new GridData());
    selectRootDirectoryLabel.setText("&Root Directory:");

    rootDirectoryText = new Text(composite, SWT.BORDER);
    rootDirectoryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    rootDirectoryText.addSelectionListener(new SelectionAdapter() {
      public void widgetDefaultSelected(SelectionEvent e) {
        if(rootDirectoryText.getText().trim().length() > 0) {
          scanProjects();
        }
      }
    });

    final Button browseButton = new Button(composite, SWT.NONE);
    browseButton.setText("&Browse...");
    browseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    browseButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.NONE);
        dialog.setText("Select Root Folder");
        dialog.setFilterPath(rootDirectoryText.getText());

        String result = dialog.open();
        if(result != null) {
          rootDirectoryText.setText(result);
          scanProjects();
        }
      }
    });

    final Label projectsLabel = new Label(composite, SWT.NONE);
    projectsLabel.setLayoutData(new GridData());
    projectsLabel.setText("&Projects:");
    new Label(composite, SWT.NONE);

    projectTreeViewer = new CheckboxTreeViewer(composite, SWT.BORDER);

    projectTreeViewer.addCheckStateListener(new ICheckStateListener() {
      public void checkStateChanged(CheckStateChangedEvent event) {
        projectTreeViewer.setSubtreeChecked(event.getElement(), event.getChecked());

        Object[] checkedElements = projectTreeViewer.getCheckedElements();
        setPageComplete(checkedElements != null && checkedElements.length > 0);
      }
    });

    projectTreeViewer.setContentProvider(new ITreeContentProvider() {

      public Object[] getElements(Object element) {
        if(element instanceof List) {
          List projects = (List) element;
          return projects.toArray(new MavenProjectInfo[projects.size()]);
        }
        return new Object[0];
      }

      public Object[] getChildren(Object parentElement) {
        if(parentElement instanceof List) {
          List projects = (List) parentElement;
          return projects.toArray(new MavenProjectInfo[projects.size()]);
        } else if(parentElement instanceof MavenProjectInfo) {
          MavenProjectInfo mavenProjectInfo = (MavenProjectInfo) parentElement;
          return mavenProjectInfo.projects.toArray(new MavenProjectInfo[mavenProjectInfo.projects.size()]);
        }
        return new Object[0];
      }

      public Object getParent(Object element) {
        return null;
      }

      public boolean hasChildren(Object parentElement) {
        if(parentElement instanceof List) {
          List projects = (List) parentElement;
          return !projects.isEmpty();
        } else if(parentElement instanceof MavenProjectInfo) {
          MavenProjectInfo mavenProjectInfo = (MavenProjectInfo) parentElement;
          return !mavenProjectInfo.projects.isEmpty();
        }
        return false;
      }

      public void dispose() {
      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }
    });

    projectTreeViewer.setLabelProvider(new LabelProvider() {
      public String getText(Object element) {
        if(element instanceof MavenProjectInfo) {
          MavenProjectInfo mavenProjectInfo = (MavenProjectInfo) element;
          Model model = mavenProjectInfo.model;
          String path = mavenProjectInfo.pomFile.getAbsolutePath();
          return path.substring(getRootPath().length()) + " - " + model.getId();
        }
        return super.getText(element);
      }
    });

    final Tree projectTree = projectTreeViewer.getTree();
    GridData projectTreeData = new GridData(SWT.FILL, SWT.FILL, false, true, 2, 3);
    projectTreeData.heightHint = 300;
    projectTreeData.widthHint = 500;
    projectTree.setLayoutData(projectTreeData);

    final Button selectAllButton = new Button(composite, SWT.NONE);
    selectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    selectAllButton.setText("Select &All");
    selectAllButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        projectTreeViewer.expandAll();
        projectTreeViewer.setAllChecked(true);
        setPageComplete(projectTreeViewer.getCheckedElements().length > 0);
      }
    });

    final Button deselectAllButton = new Button(composite, SWT.NONE);
    deselectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    deselectAllButton.setText("&Deselect All");
    deselectAllButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        projectTreeViewer.setAllChecked(false);
        setPageComplete(false);
      }
    });

    final Button refreshButton = new Button(composite, SWT.NONE);
    refreshButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, true));
    refreshButton.setText("&Refresh");
    refreshButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        scanProjects();
      }
    });

    ExpandableComposite expandable = toolkit.createExpandableComposite(composite, ExpandableComposite.COMPACT | ExpandableComposite.TWISTIE);
    expandable.clientVerticalSpacing = 1;
    expandable.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));
    expandable.setText("Ad&vanced");
    expandable.setFont(composite.getFont());
    expandable.setBackground(composite.getBackground());
    expandable.addExpansionListener(new ExpansionAdapter() {
      public void expansionStateChanged(ExpansionEvent e) {
        getControl().getShell().pack();
      }
    });
    
    toolkit.paintBordersFor(expandable);

    Composite advancedComposite = toolkit.createComposite(expandable, SWT.NONE);
    expandable.setClient(advancedComposite);
    
    final GridLayout gridLayout = new GridLayout();
    gridLayout.marginLeft = 10;
    gridLayout.numColumns = 2;
    advancedComposite.setLayout(gridLayout);
    advancedComposite.setBackground(composite.getBackground());
    // toolkit.paintBordersFor(advancedComposite);

    resolveWorkspaceProjects = new Button(advancedComposite, SWT.CHECK);
    resolveWorkspaceProjects.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    resolveWorkspaceProjects.setText("&Resolve workspace projects");
    resolveWorkspaceProjects.setSelection(true);

    projectsForModules = new Button(advancedComposite, SWT.CHECK);
    projectsForModules.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    projectsForModules.setText("&Separate projects for modules");
    projectsForModules.setSelection(true);

    final Label profilesLabel = new Label(advancedComposite, SWT.NONE);
    profilesLabel.setLayoutData(new GridData());
    profilesLabel.setText("Pr&ofiles:");

    activeProfiles = new Text(advancedComposite, SWT.BORDER);
    activeProfiles.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  }

  protected void scanProjects() {
    final File folder = new File(getRootPath());
    try {
      List projects = new ArrayList();
      getWizard().getContainer().run(true, true, new Maven2ProjectScanner(folder, projects));
      
      projectTreeViewer.setInput(projects);
      projectTreeViewer.expandAll();
      projectTreeViewer.setAllChecked(true);
      Object[] checkedElements = projectTreeViewer.getCheckedElements();
      setPageComplete(checkedElements != null && checkedElements.length > 0);
      
    } catch(InterruptedException ex) {
      // canceled
    } catch(InvocationTargetException ex) {
      Throwable e = ex.getTargetException() == null ? ex : ex.getTargetException();
      Maven2Plugin.getDefault().getConsole().logError(
          "Scanning error " + folder.getAbsolutePath() + "; " + e.toString());
    } catch(Exception ex) {
      Maven2Plugin.getDefault().getConsole().logError(
          "Scanning error " + folder.getAbsolutePath() + "; " + ex.toString());
    }
  }

  String getRootPath() {
    return rootDirectoryText.getText();
  }

  public List getProjects() {
    return (List) projectTreeViewer.getInput();
  }
  public List getCheckedProjects() {
    return Arrays.asList(projectTreeViewer.getCheckedElements());
  }

  public boolean createProjectsForModules() {
    return this.projectsForModules.getSelection();
  }

  public boolean resolveWorkspaceProjects() {
    return this.resolveWorkspaceProjects.getSelection();
  }

  public String getActiveProfiles() {
    return this.activeProfiles.getText();
  }

}
