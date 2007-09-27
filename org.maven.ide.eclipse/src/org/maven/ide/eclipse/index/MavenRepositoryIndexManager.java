
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

import org.apache.lucene.index.IndexReader;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
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

  public final Maven2Console console;

  private final MavenEmbedderManager embedderManager;

  private final IPath stateLocation;

  private IndexerJob localIndexer;

  
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
      IndexReader reader = null;
      try {
        reader = IndexReader.open(localRepositoryIndexDir);        
        indexes.add(LOCAL_INDEX);
      } catch(Exception ex) {
        reindexLocal(5000L);
      } finally {
        try {
          if(reader != null) {
            reader.close();
          }
        } catch(IOException ex) {
          // ignore
        }
      }
      
    } else {
      reindexLocal(5000L);
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

  public void reindexLocal(long delay) {
    if(localIndexer==null || localIndexer.getState()==Job.NONE) {
      localIndexer = new IndexerJob(LOCAL_INDEX, indexes, getIndexDir());
    }
    localIndexer.reindex(embedderManager.getLocalRepositoryDir(), delay);
  }

  public synchronized void updateIndex(File localFile, String repository, String name, long size, long date) {
    String indexPath = new File(getIndexDir(), LOCAL_INDEX).getAbsolutePath();
    try {
      Indexer indexer = new Indexer();
      indexer.addDocument(repository, name, size, date, indexer.readNames(localFile), indexPath);
    } catch( IOException ex ) {
      String msg = "Unable to index "+name;
      console.logError(msg + "; " + ex.getMessage());
      Maven2Plugin.log(msg, ex);
    }
  }
  
  private File getIndexDir() {
    return new File(stateLocation.toFile(), "index");
  }
  
  // TODO implement index registry
  private String[] getIndexNames() {
    return (String[]) indexes.toArray(new String[indexes.size()]);
  }

}

