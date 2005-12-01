/* $Id$ */

package org.maven.ide.eclipse;

import org.apache.maven.embedder.MavenEmbedderLogger;


class ConsoleMavenEmbeddedLogger implements MavenEmbedderLogger {
  private int treshold = LEVEL_DEBUG;

  public void debug( String msg ) {
    if (isDebugEnabled()) {
      System.out.println( "[DEBUG] "+msg);
    }
  }

  public void debug( String msg, Throwable t) {
    if (isDebugEnabled()) {
      System.out.println( "[DEBUG] "+msg+" "+t.getMessage());
    }
  }

  public void info( String msg ) {
    if (isInfoEnabled()) {
      System.out.println( "[INFO] "+msg);
    }
  }

  public void info( String msg, Throwable t ) {
    if (isInfoEnabled()) {
      System.out.println( "[INFO] "+msg+" "+t.getMessage());
    }
  }

  public void warn( String msg ) {
    if (isWarnEnabled()) {
      System.out.println("[WARN] "+msg);
    }
  }
  
  public void warn( String msg, Throwable t ) {
    if (isWarnEnabled()) {
      System.out.println( "[WARN] "+msg+" "+t.getMessage());
    }
  }
  
  public void fatalError( String msg ) {
    if (isFatalErrorEnabled()) {
      System.out.println( "[FATAL ERROR] "+msg);
    }
  }
  
  public void fatalError( String msg, Throwable t ) {
    if (isFatalErrorEnabled()) {
      System.out.println( "[FATAL ERROR] "+msg+" "+t.getMessage());
    }
  }
  
  public void error( String msg ) {
    if (isErrorEnabled()) {
      System.out.println( "[ERROR] "+msg);
    }
  }
  
  public void error( String msg, Throwable t ) {
    if (isErrorEnabled()) {
      System.out.println( "[ERROR] "+msg+" "+t.getMessage());
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

