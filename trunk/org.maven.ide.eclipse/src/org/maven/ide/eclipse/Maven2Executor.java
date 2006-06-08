
package org.maven.ide.eclipse;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.embedder.MavenEmbedderLogger;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.settings.Settings;
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
    
    List goals = new ArrayList();
    for (int i = 1; i < args.length; i++) {
      goals.add(args[i]);
    }
    
    MavenEmbedder embedder = new MavenEmbedder();
    try {
      
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      
      embedder.setClassLoader(classLoader);
      
      ConsoleMavenEmbeddedLogger logger = new ConsoleMavenEmbeddedLogger();
      if (Boolean.getBoolean(Maven2PreferenceConstants.P_DEBUG_OUTPUT)) {
        logger.setThreshold(MavenEmbedderLogger.LEVEL_DEBUG);
      }
      else {
        logger.setThreshold(MavenEmbedderLogger.LEVEL_INFO);
      }
      embedder.setLogger(logger);
      embedder.start();

      String pomFileName = args[0];
      //System.out.println( "POM: " + pomFileName );

      File pomFile = new File( pomFileName );

      // MavenProject mavenProject = embedder.readProjectWithDependencies( pomFile );
      // mavenProject.getProperties().setProperty("compilerId", "javac");

      Properties properties = System.getProperties();
      //System.err.println(properties);
      
      ConsoleEventMonitor consoleEventMonitor = new ConsoleEventMonitor();
      TransferListener transferListener = new ConsoleTransferMonitor();

      // embedder.execute(mavenProject, goals, consoleEventMonitor, transferListener, properties, pomFile.getParentFile() );

      File userSettingsPath = embedder.getUserSettingsPath( null );
      File globalSettingsFile = embedder.getGlobalSettingsPath();
      
      Settings settings = embedder.buildSettings( 
          userSettingsPath,
          globalSettingsFile,
          false,  // interactive
          false,  // offline,
          false,  // usePluginRegistry,
          Boolean.FALSE);  // pluginUpdateOverride );

      String localRepositoryPath = System.getProperty(Maven2PreferenceConstants.P_LOCAL_REPOSITORY_DIR);
      if(localRepositoryPath==null || localRepositoryPath.trim().length()==0) {
        localRepositoryPath = embedder.getLocalRepositoryPath( settings );
      }
      
      MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest()
          .setPomFile( pomFile.getAbsolutePath() )
          .setBasedir( pomFile.getParentFile() )
          .setGoals( goals )
          .setProperties( properties )
          .setSettings( settings )
          .setLocalRepositoryPath( localRepositoryPath )
          .setReactorActive( false )
          .setRecursive( true )
          .setInteractive( false )
          .setTransferListener( transferListener )
          .addEventMonitor( consoleEventMonitor )
          // .activateDefaultEventMonitor()
          .setShowErrors( true )  // TODO make configurable
          .setLoggingLevel( MavenExecutionRequest.LOGGING_LEVEL_INFO )  // TODO make configurable
          .setFailureBehavior( MavenExecutionRequest.REACTOR_FAIL_AT_END ) // TODO make configurable
          // .addActiveProfiles( activeProfiles )  // TODO make configurable
          // .addInactiveProfiles( inactiveProfiles )  // TODO make configurable
          .setOffline(Boolean.getBoolean(Maven2PreferenceConstants.P_OFFLINE))
          .setGlobalChecksumPolicy(System.getProperty(Maven2PreferenceConstants.P_GLOBAL_CHECKSUM_POLICY));
          // .setUpdateSnapshots( updateSnapshots )  // TODO make configurable
          ;
      
      embedder.execute( executionRequest );
      
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

}

