
package org.maven.ide.eclipse.actions;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.apache.maven.embedder.MavenEmbedder;

import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.MavenEmbedderCallback;


/**
 * Simple Goal Callback that is designed to work in conjunction with the Maven2
 * Plugin embedder
 */
class GoalExecutionCallBack implements MavenEmbedderCallback {
  private File pomFile;
  private List goals;
  private Properties properties;


  public GoalExecutionCallBack( File pomFile, List goals, Properties properties ) {
    this.pomFile = pomFile;
    this.goals = goals;
    this.properties = properties;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.maven.ide.eclipse.MavenEmbedderCallback#run(org.apache.maven.embedder.MavenEmbedder,
   *      org.eclipse.core.runtime.IProgressMonitor)
   */
  public Object run( MavenEmbedder mavenEmbedder, IProgressMonitor monitor ) {
    try {
      mavenEmbedder.execute( Maven2Plugin.getDefault().getMavenExecutionRequest( mavenEmbedder, pomFile, properties, goals ) );
    } catch( Exception ex ) {
      Maven2Plugin.getDefault().getConsole().logError( ex.toString() );
    }
    return null;
  }

}

