
package org.maven.ide.eclipse;

import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;

import org.eclipse.core.runtime.IProgressMonitor;

public final class TransferListenerAdapter implements TransferListener {
  private final IProgressMonitor monitor;

  private long complete = 0;
  
  
  public TransferListenerAdapter( IProgressMonitor monitor) {
    this.monitor = monitor;
  }

  public void transferInitiated( TransferEvent e) {
    // System.err.println( "init "+e.getWagon().getRepository()+"/"+e.getResource().getName());
    this.complete = 0;
  }

  public void transferStarted( TransferEvent e) {
    // System.err.println( "start "+e.getWagon().getRepository()+"/"+e.getResource().getName());
  }

  public void transferProgress( TransferEvent e, byte[] buffer, int length) {
    complete += length;
    // System.err.println( "progress "+complete+" "+e.getWagon().getRepository()+"/"+e.getResource().getName());

    StringBuffer sb = new StringBuffer();
    long total = e.getResource().getContentLength();
    if( total>=1024) {
      sb.append( complete / 1024);
      if( total!=WagonConstants.UNKNOWN_LENGTH) {
        sb.append("/").append( total / 1024).append( "K");
      }
      
    } else {
      sb.append( complete);
      if( total!=WagonConstants.UNKNOWN_LENGTH) {
        sb.append("/").append( total).append( "b");
      }
    }
    
    monitor.subTask((int) ( 100d * complete / total)+"% "+e.getWagon().getRepository()+"/"+e.getResource().getName());
  }

  public void transferCompleted( TransferEvent e) {
    // System.err.println( "completed "+e.getWagon().getRepository()+"/"+e.getResource().getName());
    monitor.subTask("downloading");
  }

  public void transferError( TransferEvent e) {
    // System.err.println( "error "+e.getWagon().getRepository()+"/"+e.getResource().getName());
    monitor.subTask("downloading");
  }

  public void debug( String message) {
    // System.err.println( "debug "+message);
  }
}

