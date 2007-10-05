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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
import org.maven.ide.eclipse.embedder.ResolverConfiguration;


/**
 * Maven Import Wizard Page
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
    super("Maven Projects");
    setTitle("Maven Projects");
    setDescription("Select Maven projects");
    setPageComplete(false);
  }

  public void createControl(Composite parent) {
    FormToolkit toolkit = new FormToolkit(Display.getCurrent());

    final Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(3, false));
    setControl(composite);

    if(showLocation()) {
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
    }

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
          List projects = mavenProjectInfo.getProjects();
          return projects.toArray(new MavenProjectInfo[projects.size()]);
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
          return !mavenProjectInfo.getProjects().isEmpty();
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
          return mavenProjectInfo.getLabel() + " - " + mavenProjectInfo.getModel().getId();
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

    ExpandableComposite expandable = toolkit.createExpandableComposite(composite, ExpandableComposite.COMPACT
        | ExpandableComposite.TWISTIE);
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
    AbstractProjectScanner projectScanner = getProjectScanner();
    try {
      getWizard().getContainer().run(true, true, projectScanner);

      projectTreeViewer.setInput(projectScanner.getProjects());
      projectTreeViewer.expandAll();
      projectTreeViewer.setAllChecked(true);
      Object[] checkedElements = projectTreeViewer.getCheckedElements();
      setPageComplete(checkedElements != null && checkedElements.length > 0);

    } catch(InterruptedException ex) {
      // canceled
    } catch(InvocationTargetException ex) {
      Throwable e = ex.getTargetException() == null ? ex : ex.getTargetException();
      Maven2Plugin.getDefault().getConsole().logError(
          "Scanning error " + projectScanner.getDescription() + "; " + e.toString());
    } catch(Exception ex) {
      Maven2Plugin.getDefault().getConsole().logError(
          "Scanning error " + projectScanner.getDescription() + "; " + ex.toString());
    }
  }

  protected AbstractProjectScanner getProjectScanner() {
    return new Maven2ProjectScanner(new File(getRootPath()));
  }

  protected boolean showLocation() {
    return true;
  }

  String getRootPath() {
    return rootDirectoryText.getText();
  }

  public List getProjects() {
    List checkedProjects = Arrays.asList(projectTreeViewer.getCheckedElements());
    
    if(createProjectsForModules()) {
      return checkedProjects;
    }
    
    List mavenProjects = new ArrayList();
    collectProjects(mavenProjects, new HashSet(checkedProjects), (List) projectTreeViewer.getInput());
    return mavenProjects;
  }

  private void collectProjects(List mavenProjects, Set checkedProjects, List childProjects) {
    for(Iterator it = childProjects.iterator(); it.hasNext();) {
      MavenProjectInfo projectInfo = (MavenProjectInfo) it.next();
      if(checkedProjects.contains(projectInfo)) {
        mavenProjects.add(projectInfo);
      } else {
        collectProjects(mavenProjects, checkedProjects, projectInfo.getProjects());
      }
    }
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

  public ResolverConfiguration getResolverConfiguration() {
    boolean includeModules = !createProjectsForModules();
    boolean resolveWorkspaceProjects = resolveWorkspaceProjects();
    return new ResolverConfiguration(includeModules, resolveWorkspaceProjects, getActiveProfiles());
  }

}
