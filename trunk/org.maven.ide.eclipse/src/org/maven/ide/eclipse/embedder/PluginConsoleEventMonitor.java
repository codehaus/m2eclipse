
package org.maven.ide.eclipse.embedder;

import org.apache.maven.monitor.event.EventMonitor;

import org.maven.ide.eclipse.launch.console.Maven2Console;


public class PluginConsoleEventMonitor implements EventMonitor {

  private Maven2Console console;

  public PluginConsoleEventMonitor(Maven2Console console) {
    this.console = console;
  }

  public void startEvent( String eventName, String target, long timestamp ) {
    if( "mojo-execute".equals( eventName ) ) {
      console.logMessage( target );
    }
  }

  public void endEvent( String eventName, String target, long timestamp ) {
    if( "project-execute".equals( eventName ) ) {
      console.logMessage( "BUILD SUCCESSFUL" );
    }
  }

  public void errorEvent( String eventName, String target, long timestamp, Throwable cause ) {
    console.logMessage("ERROR " + eventName + " : " + target + (cause == null ? "" : " : " + cause.getMessage()));
  }

}

