/* $Id$ */

package org.maven.ide.eclipse.util;

public class Tracer {
  private Tracer() {}
  
  public static void trace(ITraceable target, String message, Object param) {
    if (target.isTraceEnabled()) {
      System.out.println(target.getClass().getName()+": "+message+(param != null ? ": ["+param+"]" : ""));
    }
  }

  public static void trace(ITraceable target, String message, Object param, Throwable e) {
    trace(target, message, param);
    if (target.isTraceEnabled() && e != null) {
      e.printStackTrace(System.out);
    }
  }
  
  public static void trace(ITraceable target, String message) {
    trace(target, message, null);
  }

}
