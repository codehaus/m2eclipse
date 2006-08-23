
package org.maven.ide.eclipse.index;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.settings.Settings;

import org.maven.ide.eclipse.Maven2Plugin;

/**
 * MavenRepositoryIndexManager
 *
 * @author Eugene Kuleshov
 */
public class MavenRepositoryIndexManager {
  public static final String[] DEFAULT_INDEXES = {"central"};  //$NON-NLS-1$

  private final Maven2Plugin plugin;  
  
  protected Set indexes = Collections.synchronizedSet( new HashSet() );


  
  public MavenRepositoryIndexManager(Maven2Plugin plugin) {
    this.plugin = plugin;
    
    UnpackerJob unpackerJob = new UnpackerJob(plugin.getBundle(), getIndexDir(), DEFAULT_INDEXES, indexes);
    unpackerJob.schedule(2000L);
  }
  
  
  public File[] getIndexes() {
    String[] indexNames = getIndexNames();

    File[] indexes = new File[indexNames.length];
    for(int i = 0; i < indexes.length; i++ ) {
      indexes[i] = new File(getIndexDir(), indexNames[i]);
    }

    return indexes;
  }

  public String getIndexDir() {
    return new File(plugin.getStateLocation().toFile(), "index").getAbsolutePath();
  }
  
  // TODO implement index registry
  private String[] getIndexNames() {
    return (String[]) indexes.toArray(new String[indexes.size()]);
  }

  
  public void reindexLocal() {
    Settings mavenSettings = plugin.getMavenSettings();
    String localRepository = mavenSettings.getLocalRepository();
    if(localRepository==null) {
      plugin.getConsole().logError("Unable to get local repository folder");
      return;
    }
    
    File localRepositoryDir = new File(localRepository);
    if(!localRepositoryDir.exists()) {
      plugin.getConsole().logError("Created local repository folder "+localRepository);
      localRepositoryDir.mkdirs();
    }
    
    if(!localRepositoryDir.isDirectory()) {
      plugin.getConsole().logError("Local repository "+localRepository+" is not a directory");
      return;
    }
  
    IndexerJob indexerJob = new IndexerJob("local", localRepository, getIndexDir(), indexes);
    indexerJob.schedule();
  }


  public void updateIndex(File localFile, String repository, String name, long size, long date) {
    String indexPath = new File(getIndexDir(), "local").getAbsolutePath();
    try {
      Indexer indexer = new Indexer();
      indexer.addDocument(repository, name, size, date, indexer.readNames(localFile), indexPath);
    } catch( IOException ex ) {
      Maven2Plugin.getDefault().getConsole().logError("Unable to index "+name+"; "+ex.getMessage());
    }
  }

}

