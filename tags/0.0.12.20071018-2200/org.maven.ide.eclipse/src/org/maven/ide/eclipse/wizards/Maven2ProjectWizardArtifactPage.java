package org.maven.ide.eclipse.wizards;

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

import java.util.ArrayList;

import org.apache.maven.model.Model;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.maven.ide.eclipse.Messages;
import org.maven.ide.eclipse.embedder.ResolverConfiguration;


/**
 * Wizard page responsible for gathering information about the Maven2 artifact and the directories to create. This
 * wizard page gathers Maven2 specific information. The user must specify the necessary information about the Maven2
 * artifact and she can also decide which directories of the default Maven2 directory structure should be created. Input
 * validation is performed in order to make sure that all the provided information is valid before letting the wizard
 * continue.
 */
public class Maven2ProjectWizardArtifactPage extends AbstractMavenWizardPage {

  /** The default Maven2 output path. */
  private static final String DEFAULT_MAVEN2_OUTPUT_LOCATION = "target/classes";

  private final Maven2ProjectWizardLocationPage locationPage;

  /** Component to gather information about the Maven2 artifact. */
  protected Maven2ArtifactComponent artifactComponent;

  /** Component which allows to choose which Maven2 directories to create. */
  private Maven2DirectoriesComponent directoriesComponent;

  /**
   * Sets the title and description of this wizard page and marks it as not being complete as user input is required for
   * continuing.
   */
  public Maven2ProjectWizardArtifactPage(ResolverConfiguration resolverConfiguration,
      Maven2ProjectWizardLocationPage locationPage) {
    super("MavenProjectWizardArtifactPage", resolverConfiguration);
    this.locationPage = locationPage;
    setTitle(Messages.getString("wizard.project.page.maven2.title"));
    setDescription(Messages.getString("wizard.project.page.maven2.description"));
    setPageComplete(false);
  }

  /**
   * {@inheritDoc} This wizard page contains a <code>Maven2ArtifactComponent</code> to gather information about the
   * Maven2 artifact and a <code>Maven2DirectoriesComponent</code> which allows to choose which directories of the
   * default Maven2 directory structure to create.
   */
  public void createControl(Composite parent) {
    GridLayout layout = new GridLayout();

    Composite container = new Composite(parent, SWT.NULL);
    container.setLayout(layout);

    ModifyListener modifyingListener = new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        validate();
      }
    };

    // artifact group
    GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    artifactComponent = new Maven2ArtifactComponent(container, SWT.NONE);
    artifactComponent.setLayoutData(gridData);
    artifactComponent.setModifyingListener(modifyingListener);

    // directory structure
    gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
    directoriesComponent = new Maven2DirectoriesComponent(container, SWT.NONE);
    directoriesComponent.setLayoutData(gridData);

    artifactComponent.setPackaging(Maven2ArtifactComponent.DEFAULT_PACKAGING);
    artifactComponent.setVersion(Maven2ArtifactComponent.DEFAULT_VERSION);
    validate();

    createAdvancedSettings(container, new GridData(SWT.FILL, SWT.TOP, false, false, 2, 1));

    setControl(container);
  }

  /**
   * Returns the Maven2 model containing the artifact information provided by the user.
   * 
   * @return The Maven2 model containing the provided artifact information. Is never <code>null</code>.
   */
  public Model getModel() {
    return artifactComponent.getModel();
  }

  public Maven2ArtifactComponent getArtifactComponent() {
    return this.artifactComponent;
  }

  /**
   * Returns the directories of the default Maven2 directory structure selected by the user. These directories should be
   * created along with the new project.
   * 
   * @return The Maven2 directories selected by the user. Neither the array nor any of its elements is <code>null</code>.
   */
  public String[] getDirectories() {
    Maven2Directory[] mavenDirectories = directoriesComponent.getDirectories();
    String[] directories = new String[mavenDirectories.length];
    for(int i = 0; i < directories.length; i++ ) {
      directories[i] = mavenDirectories[i].getPath();
    }

    return directories;
  }

  /**
   * Returns all the required source classpath entries for the Maven2 directories currently selected by the user.
   * <p>
   * Note that only source and source related resource directories require such a classpath entry since the contents of
   * other directories are not relevant to Eclipse. More concretely, only the following Maven2 directories require a
   * classpath entry to be created:
   * <ul>
   * <li>src/main/java</li>
   * <li>src/main/resources</li>
   * <li>src/test/java</li>
   * <li>src/test/resources</li>
   * </ul>
   * </p>
   * <p>
   * The output folders of the classpath entries are thereby set up to match the output folders used by Maven2.
   * </p>
   * <p>
   * Note that the output folder for some classpath entry is only explicitly set if it does not correspond to the
   * default output path as returned by <code>getDefaultOutputLocationPath( IPath projectPath )</code> as this is
   * implicitly inferred.
   * </p>
   * 
   * @param projectPath The path to the project for which the classpath entries are built.
   * @return All the required source classpath entries for the Maven2 directories currently selected by the user.
   *         Neither the array nor any of its elements is <code>null</code> and all the classpath entries are of type
   *         <code>JavaCore.CPE_SOURCE</code>.
   */
  public IClasspathEntry[] getClasspathEntries(IPath projectPath) {
    Maven2Directory[] mavenDirectories = directoriesComponent.getDirectories();
    ArrayList entries = new ArrayList();
    for(int i = 0; i < mavenDirectories.length; i++ ) {
      if(mavenDirectories[i].isSourceEntry()) {
        entries.add(getClasspathEntry(projectPath, mavenDirectories[i]));
      }
    }

    return (IClasspathEntry[]) entries.toArray(new IClasspathEntry[entries.size()]);
  }

  /**
   * Returns a single source classpath entry for the given Maven2 <code>directory</code>. The source classpath entry
   * will have the appropriate output folder set as defined by Maven2.
   * 
   * @param projectPath The path to the project for which the classpath entries are built.
   * @param directory The Maven2 directory for which to create the source classpath entry.
   * @return The source classpath entry of type <code>JavaCore.CPE_SOURCE</code>. Is never <code>null</code>.
   */
  private static IClasspathEntry getClasspathEntry(IPath projectPath, Maven2Directory directory) {
    // If the output path is undefined or it corresponds to the default output
    // directory set in the classpath, we do not explicitely include it in
    // the source classpath entry as it is automatically implied.
    IPath outputPath = null;
    if(directory.getOutputPath() != null && !DEFAULT_MAVEN2_OUTPUT_LOCATION.equals(directory.getOutputPath())) {
      outputPath = projectPath.append(directory.getOutputPath());
    }

    return JavaCore.newSourceEntry(projectPath.append(directory.getPath()), new IPath[0], outputPath);
  }

  /**
   * Returns the default output location path of the project. This path is intended as the default output location path
   * in the Java .classpath file.
   * 
   * @param projectPath The path to the project being created.
   * @return The project relative path to the default ouput location. Is never <code>null</code>.
   */
  public IPath getDefaultOutputLocationPath(IPath projectPath) {
    return projectPath.append(DEFAULT_MAVEN2_OUTPUT_LOCATION);
  }

  /** {@inheritDoc} */
  public void setVisible(boolean visible) {
    super.setVisible(visible);

    if(visible) {
      artifactComponent.setFocus();
      
      String projectName = locationPage.getProjectName();
      artifactComponent.setGroupId(projectName);
      artifactComponent.setArtifactId(projectName);
    }
  }

  /**
   * Validates the contents of this wizard page.
   * <p>
   * Feedback about the validation is given to the user by displaying error messages or informative messages on the
   * wizard page. Depending on the provided user input, the wizard page is marked as being complete or not.
   * <p>
   * If some error or missing input is detected in the user input, an error message or informative message,
   * respectively, is displayed to the user. If the user input is complete and correct, the wizard page is marked as
   * begin complete to allow the wizard to proceed. To that end, the following conditions must be met:
   * <ul>
   * <li>The user must have provided a group ID.</li>
   * <li>The user must have provided an artifact ID.</li>
   * <li>The user must have provided a version for the artifact.</li>
   * <li>The user must have provided the packaging type for the artifact.</li>
   * </ul>
   * </p>
   * 
   * @see org.eclipse.jface.dialogs.DialogPage#setMessage(java.lang.String)
   * @see org.eclipse.jface.wizard.WizardPage#setErrorMessage(java.lang.String)
   * @see org.eclipse.jface.wizard.WizardPage#setPageComplete(boolean)
   */
  void validate() {
    if(artifactComponent.getGroupId().trim().length() == 0) {
      setErrorMessage(Messages.getString("wizard.project.page.maven2.validator.groupID"));
      setPageComplete(false);
      return;
    }

    if(artifactComponent.getArtifactId().trim().length() == 0) {
      setErrorMessage(Messages.getString("wizard.project.page.maven2.validator.artifactID"));
      setPageComplete(false);
      return;
    }

    if(artifactComponent.getVersion().trim().length() == 0) {
      setErrorMessage(Messages.getString("wizard.project.page.maven2.validator.version"));
      setPageComplete(false);
      return;
    }

    if(artifactComponent.getPackaging().trim().length() == 0) {
      setErrorMessage(Messages.getString("wizard.project.page.maven2.validator.packaging"));
      setPageComplete(false);
      return;
    }

    setPageComplete(true);

    setErrorMessage(null);
    setMessage(null);
  }

}
