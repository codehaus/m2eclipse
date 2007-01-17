
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

