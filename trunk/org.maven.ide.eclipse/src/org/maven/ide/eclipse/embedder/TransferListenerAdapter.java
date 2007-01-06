
package org.maven.ide.eclipse.embedder;

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

