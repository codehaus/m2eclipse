
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

import org.apache.maven.embedder.MavenEmbedderLogger;


class ConsoleMavenEmbeddedLogger implements MavenEmbedderLogger {
  private int treshold = LEVEL_DEBUG;

  public ConsoleMavenEmbeddedLogger(boolean debug) {
    this.treshold = debug ? LEVEL_DEBUG : LEVEL_INFO;
  }

  private void out(String s) {
    System.out.println(s);
  }

  private void outError(String s) {
    System.out.println(s);
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
  
  public void close() {
  }

}

