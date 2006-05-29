
package org.maven.ide.eclipse;

import org.apache.maven.monitor.event.EventMonitor;


public class PluginConsoleEventMonitor implements EventMonitor {

  public void startEvent( String eventName, String target, long timestamp ) {
    if( "mojo-execute".equals( eventName ) ) {
      Maven2Plugin.getDefault().getConsole().logMessage( target );
    }
  }

  public void endEvent( String eventName, String target, long timestamp ) {
    if( "project-execute".equals( eventName ) ) {
      Maven2Plugin.getDefault().getConsole().logMessage( "BUILD SUCCESSFUL" );
    }
  }

  public void errorEvent( String eventName, String target, long timestamp, Throwable cause ) {
    Maven2Plugin.getDefault().getConsole().logMessage(
        "ERROR " + eventName + " : " + target + " : " + cause.getMessage() );
  }

}

