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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Dependency;

import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.maven.ide.eclipse.Messages;
import org.maven.ide.eclipse.actions.Maven2RepositorySearchDialog;
import org.maven.ide.eclipse.embedder.ResolverConfiguration;
import org.maven.ide.eclipse.index.Indexer;


/**
 * Wizard page responsible for gathering information about the Maven2 dependencies to use. This wizard page allows the
 * user to choose the Maven2 dependencies for the project by browsing the Maven2 repositories.
 */
public class Maven2DependenciesWizardPage extends AbstractMavenWizardPage {

  /** The actual viewer containing the Maven2 dependencies. */
  TableViewer dependencyViewer;

  /** Button which triggers the action of adding a new dependency to the viewer. */
  Button addDependencyButton;

  /** Button which triggers the action of removing a dependency from the viewer. */
  Button removeDependencyButton;

  /**
   * Default constructor. Sets the title and description of this wizard page and marks it as being complete as no user
   * input is required for continuing.
   */
  public Maven2DependenciesWizardPage(ResolverConfiguration resolverConfiguration) {
    super("Maven2DependenciesWizardPage", resolverConfiguration);
    setTitle(Messages.getString("wizard.project.page.dependencies.title"));
    setDescription(Messages.getString("wizard.project.page.dependencies.description"));
    setPageComplete(true);
  }

  public Maven2DependenciesWizardPage() {
    this(null);
  }

  /**
   * {@inheritDoc} This wizard page contains a <code>TableViewer</code> to display the currently included Maven2
   * directories and a button area with buttons to add further dependencies or remove existing ones.
   */
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.numColumns = 2;
    composite.setLayout(layout);

    createDependencyViewerLabel(composite);
    createDependencyViewer(composite);
    createButtonAreaComposite(composite);

    createAdvancedSettings(composite, new GridData(SWT.FILL, SWT.TOP, false, false, 2, 1));

    initialize();

    setControl(composite);
  }

  /**
   * Creates the label describing the content nature of the dependency viewer.
   * 
   * @param parent The parent of the label component to create.
   */
  private void createDependencyViewerLabel(Composite parent) {
    GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);

    Label dependencyViewerLabel = new Label(parent, SWT.NONE);
    dependencyViewerLabel.setText(Messages.getString("wizard.project.page.dependencies.dependencies"));
    dependencyViewerLabel.setLayoutData(gridData);
  }

  /**
   * Creates the dependency viewer which will hold the chosen Maven2 dependencies.
   * 
   * @param parent The parent of the dependency viewer component to create.
   */
  private void createDependencyViewer(Composite parent) {
    GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);

    dependencyViewer = new TableViewer(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
    dependencyViewer.getControl().setLayoutData(gridData);
    dependencyViewer.setUseHashlookup(true);
    dependencyViewer.setLabelProvider(new DependencyLabelProvider());
    dependencyViewer.setSorter(new DependencySorter());

    dependencyViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        removeDependencyButton.setEnabled(selection.size() > 0);
      }
    });
  }

  /**
   * Creates the button area containg buttons to add further dependencies or remove existing ones.
   * 
   * @param parent The parent of the button area composite to create.
   */
  private void createButtonAreaComposite(Composite parent) {
    Composite buttonAreaComposite = new Composite(parent, SWT.NONE);
    GridLayout envButtonLayout = new GridLayout();
    envButtonLayout.marginHeight = 0;
    envButtonLayout.marginWidth = 5;
    buttonAreaComposite.setLayout(envButtonLayout);

    GridData gridData = new GridData(SWT.FILL, SWT.BEGINNING, false, true);
    buttonAreaComposite.setLayoutData(gridData);

    createAddDependencyButton(buttonAreaComposite);
    createRemoveDependencyButton(buttonAreaComposite);
  }

  /**
   * Creates the button for adding dependencies to the project. An appropriate listener is attached to the button in
   * order to trigger the action of adding a Maven2 dependency to the project.
   * 
   * @param parent The parent of the button control to create.
   * @see #handleAddDependency()
   */
  private void createAddDependencyButton(Composite parent) {
    addDependencyButton = new Button(parent, SWT.PUSH);
    addDependencyButton.setText(Messages.getString("wizard.project.page.dependencies.add"));

    GridData gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
    addDependencyButton.setLayoutData(gridData);

    addDependencyButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        Maven2RepositorySearchDialog dialog = new Maven2RepositorySearchDialog(getShell(), Collections.EMPTY_SET,
            Indexer.JAR_NAME);
        if(dialog.open() == Window.OK) {
          Object result = dialog.getFirstResult();
          if(result instanceof Indexer.FileInfo) {
            dependencyViewer.add(((Indexer.FileInfo) result).getDependency());
          } else if(result instanceof Indexer.ArtifactInfo) {
            // If we have an ArtifactInfo, we add the first FileInfo it contains
            // which corresponds to the latest version of the artifact.
            Set files = ((Indexer.ArtifactInfo) result).files;
            if((files != null) && (files.size() > 0)) {
              Object file = files.iterator().next();
              if(file instanceof Indexer.FileInfo) {
                dependencyViewer.add(((Indexer.FileInfo) file).getDependency());
              }
            }
          }
        }
      }
    });
  }

  /**
   * Creates the button for removing dependencies from the project. An appropriate listener is attached to the button in
   * order to trigger the action of removing the dependencies currently selected in the dependency viewer.
   * 
   * @param parent The parent of the button control to create.
   * @see #handleRemoveDependency()
   */
  private void createRemoveDependencyButton(Composite parent) {
    removeDependencyButton = new Button(parent, SWT.PUSH);
    removeDependencyButton.setText(Messages.getString("wizard.project.page.dependencies.remove"));

    GridData gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
    removeDependencyButton.setLayoutData(gridData);

    removeDependencyButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        IStructuredSelection selection = (IStructuredSelection) dependencyViewer.getSelection();
        if(selection != null) {
          dependencyViewer.remove(selection.toArray());
        }
      }
    });
  }

  /**
   * Initializes the GUI components.
   */
  private void initialize() {
    removeDependencyButton.setEnabled(false);
  }

  /**
   * Returns the Maven2 dependencies currently chosen by the user as displayed in the dependency viewer.
   * 
   * @return The Maven2 dependencies currently chosen by the user. Neither the array nor any of its elements is
   *         <code>null</code>.
   */
  public Dependency[] getDependencies() {
    List dependencies = new ArrayList();
    for(int i = 0; i < dependencyViewer.getTable().getItemCount(); i++ ) {
      Object element = dependencyViewer.getElementAt(i);
      if(element instanceof Dependency) {
        dependencies.add(element);
      }
    }
    return (Dependency[]) dependencies.toArray(new Dependency[dependencies.size()]);
  }

  /**
   * Simple <code>LabelProvider</code> attached to the dependency viewer.
   * <p>
   * The information displayed for objects of type <code>Dependency</code> inside the dependency viewer is the
   * following:
   * </p>
   * <p>
   * {groupId} - {artifactId} - {version} - {type}
   * </p>
   */
  public static class DependencyLabelProvider extends LabelProvider {

    /** The image to show for all objects of type <code>Dependency</code>. */
    private static final Image DEPENDENCY_IMAGE = JavaUI.getSharedImages().getImage(
        ISharedImages.IMG_OBJS_EXTERNAL_ARCHIVE);

    /**
     * {@inheritDoc}
     * <p>
     * The text returned for objects of type <code>Dependency</code> contains the following information about the
     * dependency:
     * </p>
     * <p>
     * {groupId} - {artifactId} - {version} - {type}
     * </p>
     */
    public String getText(Object element) {
      if(element instanceof Dependency) {
        Dependency dependency = (Dependency) element;
        return dependency.getGroupId() + " - " + dependency.getArtifactId() + " - " + dependency.getVersion() + " - "
            + dependency.getType();
      }
      return super.getText(element);
    }

    public Image getImage(Object element) {
      if(element instanceof Dependency) {
        return DEPENDENCY_IMAGE;
      }
      return super.getImage(element);
    }
  }

  /**
   * Simple <code>ViewerSorter</code> attached to the dependency viewer. Objects of type <code>Dependency</code> are
   * sorted by (1) their groupId and (2) their artifactId.
   */
  public static class DependencySorter extends ViewerSorter {

    /**
     * Two objects of type <code>Dependency</code> are sorted by (1) their groupId and (2) their artifactId.
     */
    public int compare(Viewer viewer, Object e1, Object e2) {
      if(!(e1 instanceof Dependency) || !(e2 instanceof Dependency)) {
        return super.compare(viewer, e1, e2);
      }

      // First of all, compare the group IDs of the two dependencies.
      String group1 = ((Dependency) e1).getGroupId();
      String group2 = ((Dependency) e2).getGroupId();

      int result = (group1 == null) ? -1 : group1.compareToIgnoreCase(group2);

      // If the group IDs match, we sort by the artifact IDs.
      if(result == 0) {
        String artifact1 = ((Dependency) e1).getArtifactId();
        String artifact2 = ((Dependency) e2).getArtifactId();
        result = artifact1 == null ? -1 : artifact1.compareToIgnoreCase(artifact2);
      }

      return result;
    }
  }
}
