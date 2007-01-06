
package org.maven.ide.eclipse.embedder;

import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;


class ConsoleTransferMonitor implements TransferListener {
  private long complete = 0;

  
  public void transferInitiated( TransferEvent e ) {
    this.complete = 0;
  }

  public void transferStarted( TransferEvent e ) {
    // TODO Auto-generated method transferStarted
    System.out.println( "Downloading "+e.getWagon().getRepository()+"/"+e.getResource().getName());
  }

  public void transferProgress( TransferEvent e, byte[] data, int length ) {
    complete += length;
    // System.err.println( "progress "+complete+" "+e.getWagon().getRepository()+"/"+e.getResource().getName());

//    StringBuffer sb = new StringBuffer();
//    long total = e.getResource().getContentLength();
//    if( total>=1024) {
//      sb.append( complete / 1024);
//      if( total!=WagonConstants.UNKNOWN_LENGTH) {
//        sb.append("/").append( total / 1024).append( "K");
//      }
//      
//    } else {
//      sb.append( complete);
//      if( total!=WagonConstants.UNKNOWN_LENGTH) {
//        sb.append("/").append( total).append( "b");
//      }
//    }
//    
//    System.out.print( "\r  "+(int) ( 100d * complete / total)+"%    ");
    System.out.print(".");
  }

  public void transferCompleted( TransferEvent e ) {
    System.out.println();
  }

  public void transferError( TransferEvent e ) {
    System.out.println( e.getException().getMessage());
  }

  public void debug( String msg ) {
    System.out.println( msg);
  }

}

