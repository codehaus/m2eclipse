
package org.maven.ide.eclipse.embedder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.MavenExecutionRequest;

import org.maven.ide.eclipse.launch.Maven2LaunchConstants;
import org.maven.ide.eclipse.launch.Maven2LaunchDelegate;
import org.maven.ide.eclipse.preferences.Maven2PreferenceConstants;


/**
 * Maven2Executor
 *
 * @see Maven2LaunchDelegate
 */
public class Maven2Executor implements Maven2LaunchConstants {

  public static void main(String[] args) {
    // System.err.println("Starting in "+System.getProperty("user.dir"));
    if (args.length < 1) {
      System.err.println("POM file name is missing");
    }
    // final long start = System.currentTimeMillis();

    List goals = new ArrayList();
    for (int i = 1; i < args.length; i++) {
      goals.add(args[i]);
    }

    boolean offline = Boolean.getBoolean(Maven2PreferenceConstants.P_OFFLINE);
    boolean debug = Boolean.getBoolean(Maven2PreferenceConstants.P_DEBUG_OUTPUT);

    MavenEmbedder embedder = null;
    try {
      embedder = EmbedderFactory.createMavenEmbedder(EmbedderFactory.createExecutionCustomizer(), 
          new ConsoleMavenEmbeddedLogger(debug));

      String pomFileName = args[0];
      File pomFile = new File( pomFileName );

      MavenExecutionRequest request = EmbedderFactory.createMavenExecutionRequest(embedder, offline, debug);

      request.setBasedir(pomFile.getParentFile());
      request.setPomFile(pomFile.getAbsolutePath());
      request.setGoals(goals);
      request.setProperties(System.getProperties());

      String profiles = System.getProperty(Maven2LaunchConstants.ATTR_PROFILES);
      if(profiles != null) {
        request.addActiveProfiles(Arrays.asList(profiles.split(", ")));
      }
      
      request.addEventMonitor(new ConsoleEventMonitor());
      request.setTransferListener(new ConsoleTransferMonitor());
      
      embedder.execute(request);

    } catch(Throwable e) {
      e.printStackTrace(System.out);
    } finally {
      try {
        if(embedder != null) {
          embedder.stop();
        }
      } catch(MavenEmbedderException e) {
        e.printStackTrace(System.out);
      }
    }
  }

}

