
package org.maven.ide.eclipse.index;

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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;
import org.maven.ide.eclipse.launch.console.Maven2Console;
import org.osgi.framework.Bundle;


/**
 * MavenRepositoryIndexManager
 *
 * @author Eugene Kuleshov
 */
public class MavenRepositoryIndexManager {
  private static final String LOCAL_INDEX = "local";

  private static final String[] DEFAULT_INDEXES = {"central"};  //$NON-NLS-1$

  private Set indexes = Collections.synchronizedSet(new HashSet());

  private final Maven2Console console;

  private final MavenEmbedderManager embedderManager;

  private final IPath stateLocation;

  
  public MavenRepositoryIndexManager(MavenEmbedderManager embedderManager, Maven2Console console, 
      Bundle pluginBundle, IPath stateLocation) {
    this.embedderManager = embedderManager;
    this.console = console;
    this.stateLocation = stateLocation;

    File indexDir = getIndexDir();
    
    UnpackerJob unpackerJob = new UnpackerJob(pluginBundle, indexDir, DEFAULT_INDEXES, indexes);
    unpackerJob.schedule(2000L);
    
    File localRepositoryIndexDir = new File(indexDir, LOCAL_INDEX);
    if(localRepositoryIndexDir.exists()) {
      indexes.add(LOCAL_INDEX);
    } else {
      reindexLocal();
    }
  }
  
  public File[] getIndexes() {
    String[] indexNames = getIndexNames();

    File[] indexes = new File[indexNames.length];
    for(int i = 0; i < indexes.length; i++ ) {
      indexes[i] = new File(getIndexDir(), indexNames[i]);
    }

    return indexes;
  }

  public void reindexLocal() {
    IndexerJob indexerJob = new IndexerJob(LOCAL_INDEX, getLocalRepositoryDir(), getIndexDir(), indexes);
    indexerJob.schedule();
  }


  public void updateIndex(File localFile, String repository, String name, long size, long date) {
    String indexPath = new File(getIndexDir(), LOCAL_INDEX).getAbsolutePath();
    try {
      Indexer indexer = new Indexer();
      indexer.addDocument(repository, name, size, date, indexer.readNames(localFile), indexPath);
    } catch( IOException ex ) {
      Maven2Plugin.getDefault().getConsole().logError("Unable to index "+name+"; "+ex.getMessage());
    }
  }
  
  private File getIndexDir() {
    return new File(stateLocation.toFile(), "index");
  }
  
  private File getLocalRepositoryDir() {
    String localRepository = embedderManager.getProjectEmbedder().getLocalRepository().getBasedir();
    
    File localRepositoryDir = new File(localRepository);
    
//    if(!localRepositoryDir.exists()) {
//      console.logError("Created local repository folder "+localRepository);
//      localRepositoryDir.mkdirs();
//    }
    
    if(!localRepositoryDir.isDirectory()) {
      console.logError("Local repository "+localRepository+" is not a directory");
    }
    
    return localRepositoryDir;
  }
  
  // TODO implement index registry
  private String[] getIndexNames() {
    return (String[]) indexes.toArray(new String[indexes.size()]);
  }

}

