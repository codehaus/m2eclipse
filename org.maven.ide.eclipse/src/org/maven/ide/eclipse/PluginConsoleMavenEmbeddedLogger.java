/* $Id: ConsoleMavenEmbeddedLogger.java 30 2006-01-14 22:22:52 -0500 (Sat, 14 Jan 2006) maxim $ */

package org.maven.ide.eclipse;

import org.apache.maven.embedder.MavenEmbedderLogger;


class PluginConsoleMavenEmbeddedLogger implements MavenEmbedderLogger {
  private int treshold = LEVEL_DEBUG;
  
  private void out(String s) {
    Maven2Plugin.getDefault().getConsole().logMessage(s);
  }

  private void outError(String s) {
    Maven2Plugin.getDefault().getConsole().logError(s);
  }
  
  public void debug( String msg ) {
    if (isDebugEnabled()) {
      out("[DEBUG] "+msg);
    }
  }

  public void debug( String msg, Throwable t) {
    if (isDebugEnabled()) {
      out( "[DEBUG] "+msg+" "+t.getMessage());
    }
  }

  public void info( String msg ) {
    if (isInfoEnabled()) {
      out( "[INFO] "+msg);
    }
  }

  public void info( String msg, Throwable t ) {
    if (isInfoEnabled()) {
      out( "[INFO] "+msg+" "+t.getMessage());
    }
  }

  public void warn( String msg ) {
    if (isWarnEnabled()) {
      out("[WARN] "+msg);
    }
  }
  
  public void warn( String msg, Throwable t ) {
    if (isWarnEnabled()) {
      out( "[WARN] "+msg+" "+t.getMessage());
    }
  }
  
  public void fatalError( String msg ) {
    if (isFatalErrorEnabled()) {
      outError( "[FATAL ERROR] "+msg);
    }
  }
  
  public void fatalError( String msg, Throwable t ) {
    if (isFatalErrorEnabled()) {
      outError( "[FATAL ERROR] "+msg+" "+t.getMessage());
    }
  }
  
  public void error( String msg ) {
    if (isErrorEnabled()) {
      outError( "[ERROR] "+msg);
    }
  }
  
  public void error( String msg, Throwable t ) {
    if (isErrorEnabled()) {
      outError( "[ERROR] "+msg+" "+t.getMessage());
    }
  }
  
  public boolean isDebugEnabled() {
    return this.treshold <= LEVEL_DEBUG;
  }
  
  public boolean isInfoEnabled() {
    return this.treshold <= LEVEL_INFO;
  }

  public boolean isWarnEnabled() {
    return this.treshold <= LEVEL_WARN;
  }

  public boolean isErrorEnabled() {
    return this.treshold <= LEVEL_ERROR;
  }

  public boolean isFatalErrorEnabled() {
    return this.treshold <= LEVEL_FATAL;
  }

  public void setThreshold( int treshold ) {
    this.treshold = treshold;
  }

  public int getThreshold() {
    return treshold;
  }

}
