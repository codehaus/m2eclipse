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
import java.util.Iterator;
import java.util.List;

import org.apache.maven.model.Model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.embedder.MavenModelManager;

/**
 * Maven2ImportWizardPage
 *
 * @author Eugene Kuleshov
 */
public class Maven2ImportWizardPage extends WizardPage {

  Text rootDirectoryText;
  CheckboxTreeViewer projectTreeViewer;

  
  protected Maven2ImportWizardPage() {
    super("Maven2ImportPage");
    setTitle("Import Maven projects");
    setDescription("Import Maven projects to the Workspace.");
    setPageComplete(false);
  }

  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(3, false));
    
    final Label selectRootDirectoryLabel = new Label(composite, SWT.NONE);
    selectRootDirectoryLabel.setText("Root Directory:");

    rootDirectoryText = new Text(composite, SWT.BORDER);
    rootDirectoryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    final Button browseButton = new Button(composite, SWT.NONE);
    browseButton.setText("&Browse...");
    browseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    browseButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.NONE);
        dialog.setText("Select Root Folder");
        dialog.setFilterPath(rootDirectoryText.getText());
        
        String result = dialog.open();
        if(result!=null) {
          rootDirectoryText.setText(result);
          scanProjects();
        }
      }
    });

    final Label projectsLabel = new Label(composite, SWT.NONE);
    projectsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
    projectsLabel.setText("Projects:");

    projectTreeViewer = new CheckboxTreeViewer(composite, SWT.BORDER);
      
    projectTreeViewer.addCheckStateListener(new ICheckStateListener() {
      public void checkStateChanged(CheckStateChangedEvent event) {
        Object[] checkedElements = projectTreeViewer.getCheckedElements();
        setPageComplete(checkedElements!=null && checkedElements.length>0);
      }});
    
    projectTreeViewer.setContentProvider(new ITreeContentProvider() {

      public Object[] getElements(Object element) {
        if(element instanceof List) {
          List projects = (List) element;
          return projects.toArray(new MavenProject[projects.size()]);
        }
        return new Object[0];
      }
      
      public Object[] getChildren(Object parentElement) {
        if(parentElement instanceof List) {
          List projects = (List) parentElement;
          return projects.toArray(new MavenProject[projects.size()]);
        } else if(parentElement instanceof MavenProject) {
          MavenProject mavenProject = (MavenProject) parentElement;
          return mavenProject.projects.toArray(new MavenProject[mavenProject.projects.size()]);
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
        } else if(parentElement instanceof MavenProject) {
          MavenProject mavenProject = (MavenProject) parentElement;
          return !mavenProject.projects.isEmpty();
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
          if(element instanceof MavenProject) {
            MavenProject mavenProject = (MavenProject) element;
            Model model = mavenProject.model;
            String path = mavenProject.pomFile.getAbsolutePath();
            return path.substring(getRootPath().length())  + " - " + model.getId();
          }
          return super.getText(element);
        }
      });
    
    Tree projectTree = projectTreeViewer.getTree();
    projectTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 3));

    final Button selectAllButton = new Button(composite, SWT.NONE);
    selectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    selectAllButton.setText("Select &All");
    selectAllButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        // TODO
      }
      
    });

    final Button deselectAllButton = new Button(composite, SWT.NONE);
    deselectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    deselectAllButton.setText("&Deselect All");
    deselectAllButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        // TODO
      }
    });

    final Button refreshButton = new Button(composite, SWT.NONE);
    refreshButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    refreshButton.setText("&Refresh");
    refreshButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        scanProjects();
      }
    });

    setControl(composite);
  }

  protected void scanProjects() {
    final File folder = new File(getRootPath());
    try {
      getWizard().getContainer().run(true, true, new ProjectScanner(folder, projectTreeViewer, getShell().getDisplay()));
    } catch(InterruptedException ex) {
      // canceled
    } catch(InvocationTargetException ex) {
      Throwable e = ex.getTargetException()==null ? ex : ex.getTargetException();
      Maven2Plugin.getDefault().getConsole().logError("Scanning error " + folder.getAbsolutePath()+"; " + e.toString());
    } catch(Exception ex) {
      Maven2Plugin.getDefault().getConsole().logError("Scanning error " + folder.getAbsolutePath()+"; " + ex.toString());
    }
  }



  String getRootPath() {
    return rootDirectoryText.getText();
  }
  
  public List getPomFiles() {
    return Arrays.asList(projectTreeViewer.getCheckedElements());
  }

  
  private static final class ProjectScanner implements IRunnableWithProgress {
    private final File folder;
    private final Display display;

    final CheckboxTreeViewer viewer;
    
    List projects = new ArrayList();

    ProjectScanner(File folder, CheckboxTreeViewer viewer, Display display) {
      this.folder = folder;
      this.viewer = viewer;
      this.display = display;
    }

    public void run(IProgressMonitor monitor) throws InterruptedException {
      monitor.beginTask("Scanning folders", IProgressMonitor.UNKNOWN);
      try {
        scanFolder(this.folder, new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
        display.syncExec(new Runnable() {
          public void run() {
            viewer.setInput(projects);
          }
        });
        
      } finally {
        monitor.done();
      }
    }

    private void scanFolder(File file, IProgressMonitor monitor) throws InterruptedException {
      if(monitor.isCanceled()) {
        throw new InterruptedException();
      }
      
      monitor.worked(1);
      
      if(!file.exists() || !file.isDirectory()) {
        return;
      }
      
      File pomFile = new File(file, Maven2Plugin.POM_FILE_NAME);
      MavenProject mavenProject = readMavenProject(pomFile);
      if(mavenProject!=null) {
        projects.add(mavenProject);
        return;  // don't scan subfolders of the Maven project
      }
      
      File[] files = file.listFiles();
      for(int i = 0; i < files.length; i++ ) {
        if(files[i].isDirectory()) {
          scanFolder(files[i], monitor);
        }
      }
    }

    private MavenProject readMavenProject(File pomFile) {
      if(!pomFile.exists()) {
        return null;
      }
      
      MavenModelManager modelManager = Maven2Plugin.getDefault().getMavenModelManager();
      try {
        Model model = modelManager.readMavenModel(pomFile);
        MavenProject mavenProject = new MavenProject(pomFile, model);

        for(Iterator it = model.getModules().iterator(); it.hasNext();) {
          String module = (String) it.next();
          File modulePom = new File(pomFile.getParent(), module + "/" + Maven2Plugin.POM_FILE_NAME);
          MavenProject moduleMavenProject = readMavenProject(modulePom);
          if(moduleMavenProject!=null) {
            mavenProject.add(moduleMavenProject);
          }
        }
        
        return mavenProject; 
      } catch(CoreException ex) {
        Maven2Plugin.getDefault().getConsole().logError("Unable to read model " + pomFile.getAbsolutePath());
      }
      return null;
    }
  }


  public static class MavenProject {
    public final File pomFile;
    public final Model model;
    public final ArrayList projects = new ArrayList();

    public MavenProject(File pomFile, Model model) {
      this.pomFile = pomFile;
      this.model = model;
    }

    public void add(MavenProject mavenProject) {
      projects.add(mavenProject);
    }
  }

}
