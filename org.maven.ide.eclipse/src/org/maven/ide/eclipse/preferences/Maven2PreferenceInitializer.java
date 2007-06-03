
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
    
    store.setDefault( Maven2PreferenceConstants.P_DOWNLOAD_SOURCES, false);
    store.setDefault( Maven2PreferenceConstants.P_DOWNLOAD_JAVADOC, false);
    
    // store.setDefault( Maven2PreferenceConstants.P_GLOBAL_CHECKSUM_POLICY, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN);
    // store.setDefault( Maven2PreferenceConstants.P_UPDATE_SNAPSHOTS, false);
    // store.setDefault( Maven2PreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION, false);
  }

}
