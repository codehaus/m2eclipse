
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
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.maven.ide.eclipse.Maven2Plugin;


/**
 * IndexerJob
 *
 * @author Eugene Kuleshov
 */
class IndexerJob extends Job {
  private final String repositoryName;
  private final Set indexes;
  private final File indexDir;

  private File repositoryDir;

  public IndexerJob(String repositoryName, Set indexes, File indexDir) {
    super("Indexing " + repositoryName);
    this.repositoryName = repositoryName;
    this.indexes = indexes;
    this.indexDir = indexDir;
    
    setPriority(Job.LONG);
  }

  public void reindex(File repositoryDir, long delay) {
    this.repositoryDir = repositoryDir;
    
    if(getState()==Job.NONE) {
      schedule(delay);
    }
  }

  protected IStatus run(IProgressMonitor monitor) {
    IStatus status = Status.OK_STATUS;
    while(repositoryDir!=null) {
      String repositoryPath = repositoryDir.getAbsolutePath();
      repositoryDir = null;
      try {
        File file = new File(indexDir, repositoryName);
        if(!file.exists()) {
          file.mkdirs();
        }
  
        Indexer indexer = new Indexer();
        indexer.reindex(file.getAbsolutePath(), repositoryPath, repositoryName, monitor);
        indexes.add(repositoryName);
  
      } catch(IOException ex) {
        status = new Status(IStatus.ERROR, Maven2Plugin.PLUGIN_ID, -1, "Indexing error", ex);
  
      }
    }
    return status;
  }


}

