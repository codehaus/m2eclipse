/* $Id$ */

package org.maven.ide.eclipse;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.embedder.MavenEmbedderLogger;
import org.apache.maven.project.MavenProject;
import org.apache.maven.wagon.events.TransferListener;

import org.maven.ide.eclipse.launch.Maven2LaunchConstants;
import org.maven.ide.eclipse.preferences.Maven2PreferenceConstants;


public class Maven2Executor implements Maven2LaunchConstants {
  public static void main(String[] args) {
    //System.err.println("Starting in "+System.getProperty("user.dir"));
    if (args.length < 1) {
      System.err.println("Pom file name is missing");
    }
    //final long start = System.currentTimeMillis();
    
    MavenEmbedder embedder = new MavenEmbedder();
    try {
      
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      
      embedder.setClassLoader(classLoader);
      embedder.setInteractiveMode( false );
      
      ConsoleMavenEmbeddedLogger logger = new ConsoleMavenEmbeddedLogger();
      if (enableDebug()) {
        logger.setThreshold(MavenEmbedderLogger.LEVEL_DEBUG);
      }
      else {
        logger.setThreshold(MavenEmbedderLogger.LEVEL_INFO);
      }
      embedder.setLogger(logger);

      setPreferences(embedder);
      embedder.start();

      String pomFileName = args[0];
      //System.out.println( "POM: " + pomFileName );

      File pomFile = new File( pomFileName );

      MavenProject mavenProject = embedder.readProjectWithDependencies( pomFile );
      // mavenProject.getProperties().setProperty("compilerId", "javac");

      List goals = new ArrayList();
      for (int i = 1; i < args.length; i++) {
        goals.add(args[i]);
      }

      
      Properties properties = System.getProperties();
      //System.err.println(properties);
      
      ConsoleEventMonitor consoleEventMonitor = new ConsoleEventMonitor();
      TransferListener transferListener = new ConsoleTransferMonitor();

      embedder.execute(mavenProject, goals, consoleEventMonitor, transferListener, properties, pomFile.getParentFile() );
    } catch (Throwable e) {
      e.printStackTrace(System.out);
    }
    finally {
        try {
          embedder.stop();
        } 
        catch(MavenEmbedderException e) {
          e.printStackTrace(System.out);
        }
    }
  }

  private static boolean enableDebug() {
    return Boolean.valueOf(System.getProperty(Maven2PreferenceConstants.P_DEBUG_OUTPUT)).booleanValue();
  }
  
  private static void setPreferences( MavenEmbedder embedder ) {
    String s = System.getProperty(Maven2PreferenceConstants.P_LOCAL_REPOSITORY_DIR);
    if(s!=null && s.trim().length()>0) {
      embedder.setLocalRepositoryDirectory(new File(s));
    }
    File f = new File("C:\\Documents and Settings\\maxim\\.m2\\repository");
    boolean yes = f.exists();
    embedder.setLocalRepositoryDirectory(f);
    
    s = System.getProperty(Maven2PreferenceConstants.P_GLOBAL_CHECKSUM_POLICY);
    if(s!=null && s.trim().length()>0) {
      embedder.setGlobalChecksumPolicy(s);
    }
 
    embedder.setCheckLatestPluginVersion(
        Boolean.valueOf(System.getProperty(Maven2PreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION)).booleanValue());
    embedder.setOffline(
        Boolean.valueOf(System.getProperty(Maven2PreferenceConstants.P_OFFLINE)).booleanValue());
    embedder.setPluginUpdateOverride(
        Boolean.valueOf(System.getProperty(Maven2PreferenceConstants.P_PLUGIN_UPDATE_OVERRIDE)).booleanValue());
    embedder.setUpdateSnapshots(
        Boolean.valueOf(System.getProperty(Maven2PreferenceConstants.P_UPDATE_SNAPSHOTS)).booleanValue());
    embedder.setUsePluginRegistry(
        Boolean.valueOf(System.getProperty(Maven2PreferenceConstants.P_USE_PLUGIN_REGISTRY)).booleanValue());
  }
  
}
