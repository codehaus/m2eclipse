
package org.maven.ide.eclipse.preferences;

import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;

import org.eclipse.jface.preference.*;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;

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
 * 
 * TODO move labels into the NLS properties
 * 
 * TODO update/restart MavenEmbedder on properties change
 */
public class Maven2PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  public Maven2PreferencePage() {
    super( GRID);
    setPreferenceStore( Maven2Plugin.getDefault().getPreferenceStore());
    // setDescription( "Maven2 Preferences");
  }

  /*
   * Creates the field editors. Field editors are abstractions of the common GUI
   * blocks needed to manipulate various types of preferences. Each field editor
   * knows how to save and restore itself.
   */
  public void createFieldEditors() {
    addField( new DirectoryFieldEditor( Maven2PreferenceConstants.P_LOCAL_REPOSITORY_DIR, 
        Messages.getString("preferences.localRepositoryFolder"), //$NON-NLS-1$
        getFieldEditorParent()));
    
    addField( new BooleanFieldEditor( Maven2PreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION, 
        Messages.getString("preferences.checkLastPluginVersions"), //$NON-NLS-1$
        getFieldEditorParent()));

    addField( new BooleanFieldEditor( Maven2PreferenceConstants.P_OFFLINE, 
        Messages.getString("preferences.offline"), //$NON-NLS-1$
        getFieldEditorParent()));

    addField( new BooleanFieldEditor( Maven2PreferenceConstants.P_PLUGIN_UPDATE_OVERRIDE, 
        Messages.getString("preferences.pluginUpdateOverride"), //$NON-NLS-1$
        getFieldEditorParent()));

    addField( new BooleanFieldEditor( Maven2PreferenceConstants.P_UPDATE_SNAPSHOTS, 
        Messages.getString("preferences.updateSnapshots"), //$NON-NLS-1$
        getFieldEditorParent()));

    addField( new BooleanFieldEditor( Maven2PreferenceConstants.P_USE_PLUGIN_REGISTRY, 
        Messages.getString("preferences.usePluginRegistry"), //$NON-NLS-1$
        getFieldEditorParent()));

    /*
    public static final String UPDATE_POLICY_NEVER = "never";
    public static final String UPDATE_POLICY_ALWAYS = "always";
    public static final String UPDATE_POLICY_DAILY = "daily";
    public static final String UPDATE_POLICY_INTERVAL = "interval";
     */
    addField( new RadioGroupFieldEditor( Maven2PreferenceConstants.P_GLOBAL_CHECKSUM_POLICY,
        Messages.getString("preferences.globalChecksumPolicy"), 1,  //$NON-NLS-1$
        new String[][] { 
            { Messages.getString("preferences.policyAlways"),   ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS },  //$NON-NLS-1$
            { Messages.getString("preferences.policyDaily"),    ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY },  //$NON-NLS-1$
            { Messages.getString("preferences.policyInterval"), ArtifactRepositoryPolicy.UPDATE_POLICY_INTERVAL },  //$NON-NLS-1$
            { Messages.getString("preferences.policyNever"),    ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER }},  //$NON-NLS-1$
        getFieldEditorParent(), true));
    
//    addField( new StringFieldEditor( Maven2PreferenceConstants.P_OFFLINE, 
//        "A &text preference:", 
//        getFieldEditorParent()));

    addField( new BooleanFieldEditor( Maven2PreferenceConstants.P_DEBUG_OUTPUT, 
        Messages.getString("preferences.debugOutput"), //$NON-NLS-1$
        getFieldEditorParent()));

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  public void init( IWorkbench workbench) {
  }

  public boolean performOk() {
    boolean res = super.performOk();
    if(res) {
      Maven2Plugin.getDefault().resetMavenEmbedder();
    }
    return res;
  }
  
}

