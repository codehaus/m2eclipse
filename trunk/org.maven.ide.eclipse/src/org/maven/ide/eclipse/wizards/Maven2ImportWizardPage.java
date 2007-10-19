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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.maven.ide.eclipse.Maven2Plugin;


/**
 * Maven Import Wizard Page
 * 
 * @author Eugene Kuleshov
 */
public class Maven2ImportWizardPage extends AbstractMavenWizardPage {

  protected Text rootDirectoryText;

  protected CheckboxTreeViewer projectTreeViewer;

  protected Maven2ImportWizardPage() {
    super("MavenProjectImportWizardPage");
    setTitle("Maven Projects");
    setDescription("Select Maven projects");
    setPageComplete(false);
  }

  public void createControl(Composite parent) {
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
    projectsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
    projectsLabel.setText("&Projects:");

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
    GridData projectTreeData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 3);
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

    createAdvancedSettings(composite, new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));
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
      setErrorMessage(null);

    } catch(InterruptedException ex) {
      // canceled

    } catch(InvocationTargetException ex) {
      Throwable e = ex.getTargetException() == null ? ex : ex.getTargetException();
      String msg;
      if(e instanceof CoreException) {
        msg = e.getMessage();
      } else {
        msg = "Scanning error " + projectScanner.getDescription() + "; " + e.toString();
        Maven2Plugin.getDefault().getConsole().logError(msg);
      }
      setErrorMessage(msg);

    }
  }

  protected AbstractProjectScanner getProjectScanner() {
    return new Maven2ProjectScanner(new File(rootDirectoryText.getText()));
  }

  protected boolean showLocation() {
    return true;
  }

  public List getProjects() {
    List checkedProjects = Arrays.asList(projectTreeViewer.getCheckedElements());

    if(!resolverConfiguration.shouldIncludeModules()) {
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

}
