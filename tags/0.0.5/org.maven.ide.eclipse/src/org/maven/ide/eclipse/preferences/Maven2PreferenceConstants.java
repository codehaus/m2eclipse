
package org.maven.ide.eclipse.preferences;


/**
 * Constant definitions for plug-in preferences TODO add prefs for MavenEmbedder
 */
public class Maven2PreferenceConstants {
  private static final String PREFIX = "eclipse.m2.";
  
  /** String */
  public static final String P_LOCAL_REPOSITORY_DIR = PREFIX+"localRepositoryDirectory";

  /** true or false */
  public static final String P_CHECK_LATEST_PLUGIN_VERSION = PREFIX+"checkLatestPluginVersion";

  /** String ??? */
  public static final String P_GLOBAL_CHECKSUM_POLICY = PREFIX+"globalChecksumPolicy";

  /** boolean */
  public static final String P_OFFLINE = PREFIX+"offline";

  /** boolean */
  public static final String P_UPDATE_SNAPSHOTS = PREFIX+"updateSnapshots";

  /** boolean */
  public static final String P_DEBUG_OUTPUT = PREFIX+"debugOutput";

  /** boolean */
  public static final String P_DOWNLOAD_SOURCES = PREFIX+"downloadSources";

}

