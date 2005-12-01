
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
    store.setDefault( Maven2PreferenceConstants.P_UPDATE_SNAPSHOTS, true);
    store.setDefault( Maven2PreferenceConstants.P_GLOBAL_CHECKSUM_POLICY, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS);
    
    store.setDefault( Maven2PreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION, false);
    store.setDefault( Maven2PreferenceConstants.P_PLUGIN_UPDATE_OVERRIDE, false);
    store.setDefault( Maven2PreferenceConstants.P_USE_PLUGIN_REGISTRY, true);
    
//    MavenEmbedder embedder = plugin.getMavenEmbedder();
//    File localRepository = embedder.getLocalRepositoryDirectory();
//    if(localRepository!=null) {
//      store.setDefault( Maven2PreferenceConstants.P_LOCAL_REPOSITORY_DIR, localRepository.getAbsolutePath());
//    }
//    
//    store.setDefault( Maven2PreferenceConstants.P_OFFLINE, embedder.isOffline());
//    store.setDefault( Maven2PreferenceConstants.P_UPDATE_SNAPSHOTS, embedder.isUpdateSnapshots());
//    
//    String globalChecksumPolicy = embedder.getGlobalChecksumPolicy();
//    if( globalChecksumPolicy!=null) {
//      store.setDefault( Maven2PreferenceConstants.P_GLOBAL_CHECKSUM_POLICY, 
//          globalChecksumPolicy==null ? ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER : globalChecksumPolicy);
//    }
//    
//    store.setDefault( Maven2PreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION, embedder.isCheckLatestPluginVersion() );
//    store.setDefault( Maven2PreferenceConstants.P_PLUGIN_UPDATE_OVERRIDE, embedder.isPluginUpdateOverride() );
//    store.setDefault( Maven2PreferenceConstants.P_USE_PLUGIN_REGISTRY, embedder.isUsePluginRegistry() );
  }

}
