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

package org.maven.ide.eclipse.preferences;

import java.io.File;

import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.ConfigurationValidationResult;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
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
import org.maven.ide.eclipse.embedder.PluginConsoleMavenEmbeddedLogger;


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
  final File localRepositoryDir;

  final Maven2Plugin plugin;

  final String globalSettings;

  private FileFieldEditor globalSettingsEditor;

  public Maven2PreferencePage() {
    super(GRID);
    setPreferenceStore(Maven2Plugin.getDefault().getPreferenceStore());

    plugin = Maven2Plugin.getDefault();
    localRepositoryDir = plugin.getMavenEmbedderManager().getLocalRepositoryDir();
    globalSettings = getPreferenceStore().getString(Maven2PreferenceConstants.P_GLOBAL_SETTINGS_FILE);
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

    addField(new BooleanFieldEditor(Maven2PreferenceConstants.P_OFFLINE, Messages.getString("preferences.offline"), //$NON-NLS-1$
        getFieldEditorParent()));

    // addField( new BooleanFieldEditor( Maven2PreferenceConstants.P_UPDATE_SNAPSHOTS, 
    //     Messages.getString( "preferences.updateSnapshots" ), //$NON-NLS-1$
    //     getFieldEditorParent() ) );

    addField(new BooleanFieldEditor(Maven2PreferenceConstants.P_DOWNLOAD_SOURCES, Messages
        .getString("preferences.downloadSources"), //$NON-NLS-1$
        getFieldEditorParent()));

    addField(new BooleanFieldEditor(Maven2PreferenceConstants.P_DOWNLOAD_JAVADOC, Messages
        .getString("preferences.downloadJavadoc"), //$NON-NLS-1$
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
    addField(new BooleanFieldEditor(Maven2PreferenceConstants.P_DEBUG_OUTPUT, //
        Messages.getString("preferences.debugOutput"), //$NON-NLS-1$
        getFieldEditorParent()));

    globalSettingsEditor = new FileFieldEditor(Maven2PreferenceConstants.P_GLOBAL_SETTINGS_FILE, //
        Messages.getString("preferences.globalSettingsFile"), getFieldEditorParent()) {{  //$NON-NLS-1$
          setValidateStrategy(VALIDATE_ON_KEY_STROKE);
        }
        protected boolean doCheckState() {
          return checkSettings(getStringValue());
        }
    };  
    
    addField(new StringFieldEditor("", Messages.getString("preferences.userSettingsFile"), getFieldEditorParent()) { //$NON-NLS-1$
      protected void doLoad() {
        getTextControl().setEditable(false);
        getTextControl().setText(MavenEmbedder.DEFAULT_USER_SETTINGS_FILE.getAbsolutePath());
      }
      
      protected void doLoadDefault() {
        getTextControl().setEditable(false);
        getTextControl().setText(MavenEmbedder.DEFAULT_USER_SETTINGS_FILE.getAbsolutePath());
      }
      
      protected void doStore() {
      }
      
      protected boolean doCheckState() {
        return true;
      }
    });
    
    final StringFieldEditor localRepositoryEditor = new StringFieldEditor("", Messages.getString("preferences.localRepository"), getFieldEditorParent()) { //$NON-NLS-1$
      protected void doLoad() {
        getTextControl().setEditable(false);
        getTextControl().setText(Maven2Plugin.getDefault().getMavenEmbedderManager().getLocalRepositoryDir().getAbsolutePath());
      }

      protected void doLoadDefault() {
        getTextControl().setEditable(false);
        getTextControl().setText(Maven2Plugin.getDefault().getMavenEmbedderManager().getLocalRepositoryDir().getAbsolutePath());
      }

      protected void doStore() {
      }

      protected boolean doCheckState() {
        return true;
      }
    };
    addField(localRepositoryEditor);

    addField(globalSettingsEditor);

    GridData buttonsCompositeGridData = new GridData();
    buttonsCompositeGridData.verticalIndent = 15;
    buttonsCompositeGridData.horizontalSpan = 2;

    Composite buttonsComposite = new Composite(getFieldEditorParent(), SWT.NONE);
    buttonsComposite.setLayout(new RowLayout());
    buttonsComposite.setLayoutData(buttonsCompositeGridData);

    Button reindexButton = new Button(buttonsComposite, SWT.NONE);
    reindexButton.setText(Messages.getString("preferences.reindexButton"));
    reindexButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        plugin.getMavenEmbedderManager().invalidateMavenSettings();
        plugin.getMavenRepositoryIndexManager().reindexLocal(0L);
      }
    });

    Button refreshButton = new Button(buttonsComposite, SWT.NONE);
    refreshButton.setText(Messages.getString("preferences.refreshButton"));
    refreshButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        plugin.getMavenEmbedderManager().invalidateMavenSettings();
        localRepositoryEditor.load();
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
  public void init(IWorkbench workbench) {
  }

  public boolean performOk() {
    String settingsFileName = globalSettingsEditor.getStringValue();
    if(settingsFileName != null && settingsFileName.length() > 0) {
      if(!checkSettings(settingsFileName)) {
        return false;
      }
    }
    
    setErrorMessage(null);

    Configuration userConfiguration = new DefaultConfiguration()
      .setUserSettingsFile(MavenEmbedder.DEFAULT_USER_SETTINGS_FILE)
      .setClassLoader(Thread.currentThread().getContextClassLoader())
      .setMavenEmbedderLogger(new PluginConsoleMavenEmbeddedLogger(plugin.getConsole(), false));
    
    ConfigurationValidationResult result = MavenEmbedder.validateConfiguration(userConfiguration);
    if(!result.isValid()) {
      Exception uex = result.getUserSettingsException();
      Exception gex = result.getGlobalSettingsException();
      if(uex!=null) {
        setMessage("Unable to parse user settings file; " + uex.toString(), IMessageProvider.WARNING);
      } else if(gex!=null) {
        setMessage("Unable to parse global settings file; " + gex.toString(), IMessageProvider.WARNING);
      } else {
        setMessage("User configuration is invalid", IMessageProvider.WARNING);
      }
    }

    boolean res = super.performOk();
    if(res) {
      String newGlobalSettings = getPreferenceStore().getString(Maven2PreferenceConstants.P_GLOBAL_SETTINGS_FILE);

      if(newGlobalSettings == null ? globalSettings == null : !newGlobalSettings.equals(globalSettings)) {
        plugin.getMavenEmbedderManager().invalidateMavenSettings();
      }

      File newRepositoryDir = plugin.getMavenEmbedderManager().getLocalRepositoryDir();
      if(!newRepositoryDir.equals(localRepositoryDir)) {
        plugin.getMavenRepositoryIndexManager().reindexLocal(0L);
      }
    }
    return res;
  }

  boolean checkSettings(String name) {
    File settingsFile = new File(name);
    if(!settingsFile.exists()) {
      setErrorMessage("Global settings file don't exists");
      return false;
    }
    
    Configuration globalConfiguration = new DefaultConfiguration()
      .setGlobalSettingsFile(settingsFile)
      .setClassLoader(Thread.currentThread().getContextClassLoader())
      .setMavenEmbedderLogger(new PluginConsoleMavenEmbeddedLogger(plugin.getConsole(), false));
    
    ConfigurationValidationResult result = MavenEmbedder.validateConfiguration(globalConfiguration);
    if(!result.isValid()) {
      if(result.getGlobalSettingsException()!=null) {
        setErrorMessage("Unable to parse settings file; " + result.getGlobalSettingsException().toString());
        return false;
      }
      setErrorMessage("Global settings file is invalid");
      return false;
    } 
    return true;
  }

}
