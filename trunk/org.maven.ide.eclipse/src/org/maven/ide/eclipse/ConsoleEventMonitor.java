
package org.maven.ide.eclipse;

import java.util.Date;

import org.apache.maven.monitor.event.EventMonitor;


class ConsoleEventMonitor implements EventMonitor {
  private static final String PREFIX = "[INFO] ";
  private long start;
  
  public void startEvent( String eventName, String target, long timestamp ) {
    if ("mojo-execute".equals(eventName)) {
      System.out.println(PREFIX + target);
    }
    else if ("project-execute".equals(eventName)) {
      this.start = System.currentTimeMillis();
    }
  }

  public void endEvent( String eventName, String target, long timestamp ) {
    if ("project-execute".equals(eventName)) {
      System.out.println(PREFIX+"----------------------------------------------------------------------------");
      System.out.println(PREFIX+"BUILD SUCCESSFUL");
      System.out.println(PREFIX+"----------------------------------------------------------------------------");
      System.out.println(PREFIX+getTotalTime());
      System.out.println(PREFIX+getFinishedAt());
      System.out.println(PREFIX+getMemory());
      System.out.println(PREFIX+"----------------------------------------------------------------------------");
    }
  }

  private String getTotalTime() {
    return "Total time: "+((System.currentTimeMillis()-start)/1000)+" second";
  }
  
  private String getFinishedAt() {
    return "Finished at: "+new Date();
  }
  
  private String getMemory() {
    long freeMem = Runtime.getRuntime().freeMemory();
    long totalMem = Runtime.getRuntime().totalMemory();
    return "Memory "+(freeMem/(1024*1024))+"M/"+(totalMem/(1024*1024))+"M";
  }
  
  public void errorEvent( String eventName, String target, long timestamp, Throwable cause ) {
    System.out.println("[ERROR] " + eventName + " : " + target );
    if(cause!=null) {
      System.out.println("Diagnosis: "+cause.getMessage());
    }
    System.out.println("FATAL ERROR: Error executing Maven for a project");
  }

}

