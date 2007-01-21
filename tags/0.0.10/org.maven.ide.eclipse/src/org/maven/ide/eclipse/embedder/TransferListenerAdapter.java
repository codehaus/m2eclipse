
package org.maven.ide.eclipse.embedder;

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

import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.resource.Resource;

import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.index.MavenRepositoryIndexManager;
import org.maven.ide.eclipse.launch.console.Maven2Console;


/**
 * TransferListenerAdapter
 *
 * @author Eugene Kuleshov
 */
public final class TransferListenerAdapter implements TransferListener {
  private final IProgressMonitor monitor;
  private final Maven2Console console;
  private final MavenRepositoryIndexManager indexManager;

  private long complete = 0;


  public TransferListenerAdapter(IProgressMonitor monitor, Maven2Console console, MavenRepositoryIndexManager indexManager) {
    this.monitor = monitor;
    this.console = console;
    this.indexManager = indexManager;
  }

  public void transferInitiated(TransferEvent e) {
    // System.err.println( "init "+e.getWagon().getRepository()+"/"+e.getResource().getName());
    this.complete = 0;
  }

  public void transferStarted(TransferEvent e) {
    console.logMessage("Downloading " + e.getWagon().getRepository() + "/" + e.getResource().getName());
    // monitor.beginTask("0% "+e.getWagon().getRepository()+"/"+e.getResource().getName(), IProgressMonitor.UNKNOWN);
    monitor.subTask("0% " + e.getWagon().getRepository() + "/" + e.getResource().getName());
  }

  public void transferProgress(TransferEvent e, byte[] buffer, int length) {
    complete += length;

    long total = e.getResource().getContentLength();

    StringBuffer sb = new StringBuffer();
    if(total >= 1024) {
      sb.append(complete / 1024);
      if(total != WagonConstants.UNKNOWN_LENGTH) {
        sb.append("/").append(total / 1024).append("K");
      }

    } else {
      sb.append(complete);
      if(total != WagonConstants.UNKNOWN_LENGTH) {
        sb.append("/").append(total).append("b");
      }
    }

    monitor.subTask((int) (100d * complete / total) + "% " + e.getWagon().getRepository() + "/" + e.getResource().getName());
  }

  public void transferCompleted( TransferEvent e) {
    console.logMessage("Downloaded "+e.getWagon().getRepository()+"/"+e.getResource().getName());
    
    // monitor.subTask("100% "+e.getWagon().getRepository()+"/"+e.getResource().getName());
    monitor.subTask("");

    // updating local index
    String repository = e.getWagon().getRepository().getName();
    Resource resource = e.getResource();
    
    indexManager.updateIndex(e.getLocalFile(), repository, resource.getName(), resource.getContentLength(), resource.getLastModified());
  }

  public void transferError(TransferEvent e) {
    console.logMessage("Unable to download " + e.getWagon().getRepository() + "/" + e.getResource().getName() + ": " + e.getException());
    monitor.subTask("error " + e.getWagon().getRepository() + "/" + e.getResource().getName());
  }

  public void debug(String message) {
    // System.err.println( "debug "+message);
  }
}

