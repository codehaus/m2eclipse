
package org.maven.ide.eclipse.preferences;

import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.maven.ide.eclipse.Maven2Plugin;


/**
 * Class used to initialize default preference values.
 * 
 * @author Eugene Kuleshov
 */
public class Maven2PreferenceInitializer extends AbstractPreferenceInitializer {

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
   */
  public void initializeDefaultPreferences() {
    Maven2Plugin plugin = Maven2Plugin.getDefault();
    
    IPreferenceStore store = plugin.getPreferenceStore();

    store.setDefault( Maven2PreferenceConstants.P_DEBUG_OUTPUT, false);
    
    store.setDefault( Maven2PreferenceConstants.P_OFFLINE, false);
    store.setDefault( Maven2PreferenceConstants.P_UPDATE_SNAPSHOTS, false);
    store.setDefault( Maven2PreferenceConstants.P_GLOBAL_CHECKSUM_POLICY, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN);
    
    store.setDefault( Maven2PreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION, false);
    store.setDefault( Maven2PreferenceConstants.P_DOWNLOAD_SOURCES, false);
  }

}
