
package org.maven.ide.eclipse.preferences;

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

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.Messages;


/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage</samp>,
 * we can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */
public class Maven2PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  public Maven2PreferencePage() {
    super(GRID);
    setPreferenceStore(Maven2Plugin.getDefault().getPreferenceStore());
  }

  /*
   * Creates the field editors. Field editors are abstractions of the common GUI
   * blocks needed to manipulate various types of preferences. Each field editor
   * knows how to save and restore itself.
   */
  public void createFieldEditors() {
//    addField(new DirectoryFieldEditor(Maven2PreferenceConstants.P_LOCAL_REPOSITORY_DIR, 
//        Messages.getString("preferences.localRepositoryFolder"), //$NON-NLS-1$
//        getFieldEditorParent()));

    // addField( new BooleanFieldEditor( Maven2PreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION, 
    //     Messages.getString( "preferences.checkLastPluginVersions" ), //$NON-NLS-1$
    //     getFieldEditorParent() ) );

    addField(new BooleanFieldEditor(Maven2PreferenceConstants.P_OFFLINE, 
        Messages.getString("preferences.offline"), //$NON-NLS-1$
        getFieldEditorParent()));

    // addField( new BooleanFieldEditor( Maven2PreferenceConstants.P_UPDATE_SNAPSHOTS, 
    //     Messages.getString( "preferences.updateSnapshots" ), //$NON-NLS-1$
    //     getFieldEditorParent() ) );

    addField(new BooleanFieldEditor(Maven2PreferenceConstants.P_DOWNLOAD_SOURCES, 
        Messages.getString("preferences.downloadSources"), //$NON-NLS-1$
        getFieldEditorParent()));

    addField(new BooleanFieldEditor(Maven2PreferenceConstants.P_DOWNLOAD_JAVADOC, 
        Messages.getString("preferences.downloadJavadoc"), //$NON-NLS-1$
        getFieldEditorParent()));
    
    /*
     * public static final String CHECKSUM_POLICY_FAIL = "fail"; 
     * public static final String CHECKSUM_POLICY_WARN = "warn"; 
     * public static final String CHECKSUM_POLICY_IGNORE = "ignore";
     */
//    addField(new RadioGroupFieldEditor(Maven2PreferenceConstants.P_GLOBAL_CHECKSUM_POLICY, 
//        Messages.getString("preferences.globalChecksumPolicy"), 1, //$NON-NLS-1$
//        new String[][] {
//            {Messages.getString("preferences.checksumPolicyFail"), ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL}, //$NON-NLS-1$
//            {Messages.getString("preferences.checksumPolicyIgnore"), ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE}, //$NON-NLS-1$
//            {Messages.getString("preferences.checksumPolicyWarn"), ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN}}, //$NON-NLS-1$  // DEFAULT
//        getFieldEditorParent(), true));

    // addField( new StringFieldEditor( Maven2PreferenceConstants.P_OFFLINE,
    // "A &text preference:",
    // getFieldEditorParent()));

    addField(new BooleanFieldEditor(Maven2PreferenceConstants.P_DEBUG_OUTPUT, 
        Messages.getString("preferences.debugOutput"), //$NON-NLS-1$
        getFieldEditorParent()));
    
    GridData buttonsCompositeGridData = new GridData();
    buttonsCompositeGridData.verticalIndent = 15;

    Composite buttonsComposite = new Composite(getFieldEditorParent(), SWT.NONE);
    buttonsComposite.setLayout(new RowLayout());
    buttonsComposite.setLayoutData(buttonsCompositeGridData);

    Button reindexButton = new Button(buttonsComposite, SWT.NONE);
    reindexButton.setText("Re&index Local Repository");
    reindexButton.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          Maven2Plugin.getDefault().getMavenRepositoryIndexManager().reindexLocal();
        } 
      });
    
    Button refreshButton = new Button(buttonsComposite, SWT.NONE);
    refreshButton.setText("Refresh &Settings");
    refreshButton.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          Maven2Plugin.getDefault().invalidateMavenSettings();
        } 
      });
  }

  protected void contributeButtons(Composite parent) {
    super.contributeButtons(parent);
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  public void init( IWorkbench workbench ) {
  }

  public boolean performOk() {
    boolean res = super.performOk();
    if(res) {
      Maven2Plugin.getDefault().invalidateMavenSettings();
    }
    return res;
  }

}
