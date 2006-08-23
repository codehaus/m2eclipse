
package org.maven.ide.eclipse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.SettingsConfigurationException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidArtifactRTException;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.embedder.ContainerCustomizer;
import org.apache.maven.embedder.DefaultMavenEmbedRequest;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.embedder.MavenEmbedderLogger;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.project.InvalidProjectModelException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.validation.ModelValidationResult;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.events.TransferListener;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.maven.ide.eclipse.container.Maven2ClasspathContainerInitializer;
import org.maven.ide.eclipse.index.MavenRepositoryIndexManager;
import org.maven.ide.eclipse.launch.console.Maven2Console;
import org.maven.ide.eclipse.preferences.Maven2PreferenceConstants;
import org.maven.ide.eclipse.util.ITraceable;
import org.maven.ide.eclipse.util.Tracer;
import org.osgi.framework.BundleContext;


/**
 * Maven2Plugin main plugin class.
 */
public class Maven2Plugin extends AbstractUIPlugin implements ITraceable {
  private static final boolean TRACE_ENABLED = Boolean.getBoolean(Platform.getDebugOption("org.maven.ide.eclipse/plugin"));

  public static final String PLUGIN_ID = Maven2Plugin.class.getPackage().getName();
  public static final String CONTAINER_ID = PLUGIN_ID + ".MAVEN2_CLASSPATH_CONTAINER"; //$NON-NLS-1$
  public static final String NATURE_ID = PLUGIN_ID + ".maven2Nature"; //$NON-NLS-1$
  public static final String BUILDER_ID = PLUGIN_ID + ".maven2Builder"; //$NON-NLS-1$
  public static final String MARKER_ID = PLUGIN_ID + ".maven2Problem"; //$NON-NLS-1$

  public static final String POM_FILE_NAME = "pom.xml"; //$NON-NLS-1$

  /** 
   * Reuse embedder instance or create new one on every operation 
   */
  private static final boolean REUSE_EMBEDDER = false;
  
  // The shared instance.
  private static Maven2Plugin plugin;

  private MavenEmbedder mavenEmbedder;
  private Settings mavenSettings;

  private Maven2Console console;
  private MavenModelManager mavenModelManager;
  private MavenRepositoryIndexManager mavenRepositoryIndexManager;

  
  public Maven2Plugin() {
    plugin = this;
  }

  /**
   * This method is called upon plug-in activation
   */
  public void start(final BundleContext context) throws Exception {
    super.start(context);

    try {
      this.console = new Maven2Console();
    } catch(RuntimeException ex) {
      log(new Status(IStatus.ERROR, PLUGIN_ID, -1, "Unable to start console: " + ex.toString(), ex));
    }

    this.mavenModelManager = new MavenModelManager(this);

    this.mavenRepositoryIndexManager = new MavenRepositoryIndexManager(this);
  }

  /**
   * This method is called when the plug-in is stopped
   */
  public void stop( BundleContext context) throws Exception {
    super.stop( context);
    
    stopEmbedder();
    if (this.console != null) {
      this.console.shutdown();
    }
    plugin = null;
  }

  /**
   * Returns the shared instance.
   */
  public static Maven2Plugin getDefault() {
    return plugin;
  }

  private synchronized MavenEmbedder getMavenEmbedder() {
    if(REUSE_EMBEDDER) {
      if( this.mavenEmbedder==null) {
          this.mavenEmbedder = createEmbedder();
      }
      return this.mavenEmbedder;
    }
    return createEmbedder();
  }

  public void resetMavenEmbedder() {
    stopEmbedder();
  }

  public Object executeInEmbedder(MavenEmbedderCallback template, IProgressMonitor monitor) throws CoreException {
    try {
      return template.run(getMavenEmbedder(), monitor);
    } catch(Exception ex) {
      throw new CoreException(new Status(IStatus.ERROR, Maven2Plugin.PLUGIN_ID, IStatus.ERROR, ex.getMessage(), ex));
    } finally {
      if(!REUSE_EMBEDDER) stopEmbedder();
    }
  }
  
  public Object executeInEmbedder(String name, MavenEmbedderCallback template) throws CoreException {
    try {
      EmbedderJob job = new EmbedderJob( name, template, getMavenEmbedder() );
      job.schedule();
      try {
        job.join();
        
        IStatus status = job.getResult();
        if(status==null) {
          getConsole().logError( "Job " + name + " terminated; "+job);
        } else {
          if(status.isOK()) {
            return job.getCallbackResult(); 
          }
          
          getConsole().logError( "Job " + name + " failed; "+status.getException().toString());
          throw new CoreException(status);
        }
        
      } catch( InterruptedException ex ) {
        getConsole().logError( "Job " + name +" interrupted "+ex.toString());
      }
    } finally {
      if(!REUSE_EMBEDDER) stopEmbedder();
    }
    return null;
  }  
  
  
  /**
   * Returns an image descriptor for the image file at the given plug-in
   * relative path.
   */
  public static ImageDescriptor getImageDescriptor( String path) {
    return AbstractUIPlugin.imageDescriptorFromPlugin( "org.maven.ide.eclipse", path);
  }

  public static void log( IStatus status) {
    getDefault().getLog().log( status);
  }

  public static void log( CoreException ex) {
    log( ex.getStatus());
  }

  public static void log( String msg, Throwable t) {
    log( new Status( IStatus.ERROR, PLUGIN_ID, 0, msg, t));
  }
  
  
  private MavenEmbedder createEmbedder() {
    IPreferenceStore store = this.getPreferenceStore();

    MavenEmbedder embedder = new MavenEmbedder();
    MavenEmbedderLogger logger = new PluginConsoleMavenEmbeddedLogger();
    final boolean debugEnabled = store.getBoolean(Maven2PreferenceConstants.P_DEBUG_OUTPUT);
    logger.setThreshold(debugEnabled ? MavenEmbedderLogger.LEVEL_DEBUG : MavenEmbedderLogger.LEVEL_INFO);
    embedder.setLogger(logger);
    
    // TODO find a better ClassLoader
    // ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    embedder.setClassLoader( getClass().getClassLoader());  
    embedder.setInteractiveMode(false);
    
    // File localRepositoryDirectory = mavenEmbedder.getLocalRepositoryDirectory();
    String localRepositoryDir = store.getString( Maven2PreferenceConstants.P_LOCAL_REPOSITORY_DIR);
    if( localRepositoryDir!=null && localRepositoryDir.length()>0) {
      localRepositoryDir = localRepositoryDir.trim();
      if(new File(localRepositoryDir).exists()) { 
        embedder.setLocalRepositoryDirectory( new File(localRepositoryDir.trim()));
      } else {
        getConsole().logMessage("Local repository folder \""+localRepositoryDir+"\" does not exist");
      }
    }
    
    String globalChecksumPolicy = store.getString( Maven2PreferenceConstants.P_GLOBAL_CHECKSUM_POLICY);
    if( globalChecksumPolicy!=null && globalChecksumPolicy.trim().length()>0) {
      embedder.setGlobalChecksumPolicy(globalChecksumPolicy);
    }

    embedder.setOffline(store.getBoolean( Maven2PreferenceConstants.P_OFFLINE));
//    embedder.setCheckLatestPluginVersion(store.getBoolean( Maven2PreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION));
//    embedder.setUpdateSnapshots(store.getBoolean( Maven2PreferenceConstants.P_UPDATE_SNAPSHOTS));

    try {
      DefaultMavenEmbedRequest embedRequest = new DefaultMavenEmbedRequest();
      embedRequest.setConfigurationCustomizer( new ContainerCustomizer() {
          public void customize( PlexusContainer container ) {
            container.getComponentDescriptor(ArtifactResolver.ROLE).setImplementation(EclipseArtifactResolver.class.getName());
          }
        });
      
      embedder.start(embedRequest);
    } catch( MavenEmbedderException ex ) {
      log( "Unable to start MavenEmbedder", ex );
    }
    
    return embedder;
  }

  private void stopEmbedder() {
    if( mavenEmbedder!=null) {
      try {
        mavenEmbedder.stop();
        mavenEmbedder = null;
      } catch( MavenEmbedderException ex) {
        log( "Unable to stop MavenEmbedder", ex);
      }
    }
  }

  public void addDependency( final IFile pomFile, final Dependency dependency ) {
    addDependencies( pomFile, Collections.singletonList( dependency ) );
  }

  public void addDependencies( final IFile pomFile, final List dependencies ) {
    MavenEmbedderCallback callback = new MavenEmbedderCallback() {
        public Object run( MavenEmbedder mavenEmbedder, IProgressMonitor monitor ) {
          final File pom = pomFile.getLocation().toFile();
          try {
            Model model = mavenEmbedder.readModel( pom);            
            model.getDependencies().addAll(dependencies);
            
            StringWriter w = new StringWriter();
            mavenEmbedder.writeModel( w, model);
            
            pomFile.setContents( new ByteArrayInputStream( w.toString().getBytes( "ASCII")), true, true, null);
            pomFile.refreshLocal( IResource.DEPTH_ONE, null); // TODO ???
          } catch (Exception ex) {
            getConsole().logError( "Unable to update POM: "+pom+"; "+ex.getMessage());
          }
          return null;
        }
      };
          
    try {
      executeInEmbedder( callback, new NullProgressMonitor() ); // TODO
    } catch( Exception e ) {
      // ignore
    }
  }


  public void addMarker( IResource resource, String message, int lineNumber, int severity) {
    try {
      IMarker marker = resource.createMarker( Maven2Plugin.MARKER_ID);
      marker.setAttribute( IMarker.MESSAGE, message);
      marker.setAttribute( IMarker.SEVERITY, severity);
      if( lineNumber == -1) {
        lineNumber = 1;
      }
      marker.setAttribute( IMarker.LINE_NUMBER, lineNumber);
    } catch( CoreException ex) {
      Maven2Plugin.log(ex);
    }
  }

  public void deleteMarkers( IResource resource) {
    try {
      resource.deleteMarkers( Maven2Plugin.MARKER_ID, false, IResource.DEPTH_ZERO);
    } catch( CoreException ex) {
      Maven2Plugin.log(ex);
    }
  }

  public void resolveClasspathEntries(Set libraryEntries, Set moduleArtifacts, 
      IFile pomFile, boolean recursive, IProgressMonitor monitor) {
    Tracer.trace(this, "resolveClasspathEntries from pom:"+pomFile);
    String msg = "Reading "+pomFile.getFullPath();
    getConsole().logMessage( msg);

    IProject currentProject = pomFile.getProject();
    
    monitor.beginTask( "Reading "+pomFile.getFullPath(), IProgressMonitor.UNKNOWN );
    try {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }
      
      final MavenProject mavenProject = (MavenProject) executeInEmbedder(
          new ReadProjectTask( pomFile ), new SubProgressMonitor(monitor, 1));
      if (mavenProject == null) {
        return;
      }
    
      // deleteMarkers(pomFile);
      // TODO use version?
      moduleArtifacts.add( mavenProject.getGroupId()+":"+mavenProject.getArtifactId() );
      
      final IPreferenceStore prefs = this.getPreferenceStore();
      boolean offline = prefs.getBoolean( Maven2PreferenceConstants.P_OFFLINE );
      boolean downloadSources = !offline & prefs.getBoolean( Maven2PreferenceConstants.P_DOWNLOAD_SOURCES );
      boolean downloadJavadoc = !offline & prefs.getBoolean( Maven2PreferenceConstants.P_DOWNLOAD_JAVADOC );

      Set artifacts = mavenProject.getArtifacts();
      for( Iterator it = artifacts.iterator(); it.hasNext();) {
        if(monitor.isCanceled()) {
          throw new OperationCanceledException();
        }

        final Artifact a = ( Artifact) it.next();

        monitor.subTask( "Processing " + a.getId() );
        String artifactLocation = a.getFile().getAbsolutePath();
        
        // The artifact filename cannot be used here to determine
        // the type because eclipse project artifacts don't have jar or zip file names.
        // TODO use version?
        if(!moduleArtifacts.contains(a.getGroupId()+":"+a.getArtifactId()) &&
           ("jar".equals(a.getType()) || "zip".equals(a.getType()))) {
          
          moduleArtifacts.add(a.getGroupId()+":"+a.getArtifactId());
          
          IFile artifactFile = getMavenModelManager().getArtifactFile( a );
          if (artifactFile != null) {
            IProject artifactProject = artifactFile.getProject();
            if(artifactProject.getFullPath().equals(currentProject.getFullPath())) {
              // This is another artifact in our current project so we should not
              // add our own project to ourself
              continue;
            }
                        
            libraryEntries.add(JavaCore.newProjectEntry(artifactProject.getFullPath(), false));
            continue;
          }
            
          Path srcPath = materializeArtifactPath(mavenProject, a, "java-source", "sources", downloadSources, monitor);
          
          IClasspathAttribute[] attributes = new IClasspathAttribute[0];
          if(srcPath==null) {  // no need to search for javadoc if we have source code
            Path javadocPath = materializeArtifactPath(mavenProject, a, "java-doc", "javadoc", downloadJavadoc, monitor);
            String javaDocUrl = null;
            if(javadocPath != null) {
              javaDocUrl = Maven2ClasspathContainerInitializer.getJavaDocUrl(javadocPath.toString());
            } else {
              javaDocUrl = getJavaDocUrl(artifactLocation, monitor);
            }
            if(javaDocUrl!=null) {
              attributes = new IClasspathAttribute[] {
                  JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, javaDocUrl)};
            }
          }
          
          libraryEntries.add(JavaCore.newLibraryEntry(new Path(artifactLocation), srcPath, null, 
              new IAccessRule[0], attributes, false /*not exported*/));
        }
      }

      
      if(recursive) {
        IContainer parent = pomFile.getParent();
        
        List modules = mavenProject.getModules();
        for( Iterator it = modules.iterator(); it.hasNext() && !monitor.isCanceled(); ) {
          if(monitor.isCanceled()) {
            throw new OperationCanceledException();
          }

          String module = ( String ) it.next();
          IResource memberPom = parent.findMember( module+"/"+POM_FILE_NAME); //$NON-NLS-1$
          if(memberPom!=null && memberPom.getType() == IResource.FILE) {
            resolveClasspathEntries(libraryEntries, moduleArtifacts, (IFile) memberPom, true, 
                new SubProgressMonitor(monitor, 1));
          }
        }    
      }

    } catch(OperationCanceledException ex) {
      throw ex;
      
    } catch(InvalidArtifactRTException ex) {
      deleteMarkers(pomFile);
      addMarker(pomFile, ex.getBaseMessage(), 1, IMarker.SEVERITY_ERROR);
      
    } catch(Throwable ex) {
      deleteMarkers(pomFile);
      addMarker(pomFile, ex.toString(), 1, IMarker.SEVERITY_ERROR);
      
    } finally {
      monitor.done();
      
    }
  }

  private String getJavaDocUrl(String artifactLocation, IProgressMonitor monitor) throws CoreException {
    // guess the javadoc url from the project url in the artifact's pom.xml
    File file = new File(artifactLocation.substring(0, artifactLocation.length()-4) + ".pom");
    if(file.exists()) {
      Model model = getMavenModelManager().readMavenModel(file, monitor);
      String url = model.getUrl();
      if(url!=null) {
        url = url.trim();
        if(url.length()>0) {
          if(!url.endsWith("/")) url += "/";
          return url + "apidocs/";  // assuming project is used maven-generated site
        }
      }              
    }
    return null;
  }

  // type = "java-source"
  private Path materializeArtifactPath(
        final MavenProject mavenProject, final Artifact a, 
        final String type, final String suffix, boolean download,
        IProgressMonitor monitor) throws Exception {
    String artifactLocation = a.getFile().getAbsolutePath();
    // artifactLocation ends on '.jar' or '.zip'
    File file = new File(artifactLocation.substring(0, artifactLocation.length()-4) + "-" + suffix + ".jar");
    Path path = null;
    if(file.exists()) {
      // XXX ugly hack to do not download any artifacts
      path = new Path(file.getAbsolutePath());
    } else if (download) {
      path = ( Path ) executeInEmbedder( new MavenEmbedderCallback() {
            public Object run(MavenEmbedder mavenEmbedder, IProgressMonitor monitor) {
              monitor.beginTask( "Resolve " + type + " " + a.getId(), IProgressMonitor.UNKNOWN );
              try {
                Artifact f = 
                  mavenEmbedder.createArtifactWithClassifier(
                      a.getGroupId(), a.getArtifactId(), a.getVersion(), 
                      type, suffix);
                if (f != null) {
                  mavenEmbedder.resolve(f, mavenProject.getRemoteArtifactRepositories(), mavenEmbedder.getLocalRepository());
                  return new Path(f.getFile().getAbsolutePath());
                }
              } catch( AbstractArtifactResolutionException ex ) {
                String name = ex.getGroupId()+":"+ex.getArtifactId()+"-"+ex.getVersion()+"."+ex.getType();
                getConsole().logError( ex.getOriginalMessage()+" "+name );
              } finally {
                monitor.done();
              }
              return null;
            }
          }, new SubProgressMonitor(monitor, 1)); 
    }
    return path;
  }

  
  /**
   * Substitute any variable
   */
  public static String substituteVar(String s) {
    if (s == null) {
      return s;
    }
    try {
    return VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(s);
  } 
    catch (CoreException e) {
      return null;
    }
  }

  public URL getRootURL() {
    return getBundle().getEntry("/"); 
  }

  public boolean isTraceEnabled() {
    return TRACE_ENABLED;
  }
  
  /**
   * Returns the standard display to be used. The method first checks, if
   * the thread calling this method has an associated display. If so, this
   * display is returned. Otherwise the method returns the default display.
   */
  public static Display getStandardDisplay() {
      Display display= Display.getCurrent();
      if (display == null) {
          display= Display.getDefault();
      }
      return display;     
  }
  
  public Maven2Console getConsole() {
    return this.console;
  }
  
  
  public static final class ReadProjectTask implements MavenEmbedderCallback {
    private final IFile file;

    public ReadProjectTask( IFile file ) {
      this.file = file;
    }

    public Object run( MavenEmbedder mavenEmbedder, IProgressMonitor monitor ) {
      monitor.beginTask( "Reading "+file.getFullPath(), IProgressMonitor.UNKNOWN );
      try {
        monitor.subTask( "Reading "+file.getFullPath() );
        TransferListenerAdapter listener = new TransferListenerAdapter( monitor );
        return mavenEmbedder.readProjectWithDependencies(this.file.getLocation().toFile(), listener);
        
      } catch( ProjectBuildingException ex ) {
        handleProjectBuildingException( ex );
        return null;
      
      } catch( AbstractArtifactResolutionException ex ) {
        // String name = ex.getGroupId()+":"+ex.getArtifactId()+"-"+ex.getVersion()+"."+ex.getType();
        String msg = ex.getMessage()
            .replaceAll("----------", "")
            .replaceAll("\r\n\r\n", "\n")
            .replaceAll("\n\n", "\n");
        Maven2Plugin.getDefault().deleteMarkers(this.file);
        Maven2Plugin.getDefault().addMarker(this.file, msg, 1, IMarker.SEVERITY_ERROR);
        Maven2Plugin.getDefault().getConsole().logError(msg);
        
        try {
          return mavenEmbedder.readProject( this.file.getLocation().toFile() );
        } catch( ProjectBuildingException ex2 ) {
          handleProjectBuildingException( ex2 );
        }

        return null;
        
      } finally {
        monitor.done();
      }
    }

    private void handleProjectBuildingException( ProjectBuildingException ex ) {
      Throwable cause = ex.getCause();
      if( cause instanceof XmlPullParserException) {
        XmlPullParserException pex = ( XmlPullParserException ) cause;
        String msg = Messages.getString("plugin.markerParsingError") + pex.getMessage();
        Maven2Plugin.getDefault().deleteMarkers(this.file);
        Maven2Plugin.getDefault().addMarker(this.file, msg, pex.getLineNumber(), IMarker.SEVERITY_ERROR); //$NON-NLS-1$
        Maven2Plugin.getDefault().getConsole().logError( msg +" at line "+ pex.getLineNumber());
      } else if( ex instanceof InvalidProjectModelException) {
        InvalidProjectModelException mex = ( InvalidProjectModelException ) ex;
        ModelValidationResult validationResult = mex.getValidationResult();
        String msg = Messages.getString("plugin.markerBuildError") + mex.getMessage();
        Maven2Plugin.getDefault().getConsole().logError( msg);        
        Maven2Plugin.getDefault().deleteMarkers(this.file);
        if(validationResult==null) {
          Maven2Plugin.getDefault().addMarker(this.file, msg, 1, IMarker.SEVERITY_ERROR); //$NON-NLS-1$
        } else {
          for( Iterator it = validationResult.getMessages().iterator(); it.hasNext(); ) {
            String message = ( String ) it.next();
            Maven2Plugin.getDefault().addMarker(this.file, message, 1, IMarker.SEVERITY_ERROR); //$NON-NLS-1$
            Maven2Plugin.getDefault().getConsole().logError("  "+message);        
          }
        }
      } else {
        String msg = Messages.getString("plugin.markerBuildError") + ex.getMessage();
        Maven2Plugin.getDefault().deleteMarkers(this.file);
        Maven2Plugin.getDefault().addMarker(this.file, msg, 1, IMarker.SEVERITY_ERROR); //$NON-NLS-1$
        Maven2Plugin.getDefault().getConsole().logError( msg);
      }
    }
  }


  private static final class EmbedderJob extends Job {
    private final MavenEmbedderCallback template;
    private final MavenEmbedder embedder;

    private Object callbackResult;

    EmbedderJob( String name, MavenEmbedderCallback template, MavenEmbedder embedder ) {
      super( name );
      this.template = template;
      this.embedder = embedder;
    }

    protected IStatus run( IProgressMonitor monitor ) {
      try {
        callbackResult = this.template.run(this.embedder, monitor);
        return Status.OK_STATUS;
      } catch( Throwable t ) {
        return new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, t.getMessage(), t);
      }
    }
    
    public Object getCallbackResult() {
      return this.callbackResult;
    }
    
  }


  /**
   * Provides a clean mechanism to allow the construction of a
   * MavenExectionRequest for the given embedder, pom and with goals and
   * properties. Note that the rest of the settings can be modified by the
   * preferences of the plugin, this allows for good re-use with some
   * consistency
   * 
   * @param embedder
   * @param pomFile
   * @param properties
   * @param goals
   * @throws SettingsConfigurationException
   */
  public MavenExecutionRequest createMavenExecutionRequest() {
    PluginConsoleEventMonitor consoleEventMonitor = new PluginConsoleEventMonitor();
    TransferListener transferListener = new ConsoleTransferMonitor();
    
    MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest()
        .setSettings(getMavenSettings())
        // .setPomFile(pomFile.getAbsolutePath())
        // .setBasedir(pomFile.getParentFile())
        // .setProperties(properties==null ? System.getProperties() : properties)
        // .setLocalRepositoryPath(localRepositoryPath)  // ?????????????
        .setReactorActive(false)
        .setRecursive(true)
        .setInteractive(false)
        .setShowErrors(true) // TODO make configurable
        .setLoggingLevel(MavenExecutionRequest.LOGGING_LEVEL_INFO) // TODO make configurable
        .setFailureBehavior(MavenExecutionRequest.REACTOR_FAIL_AT_END) // TODO make configurable
        // .setOffline(offline)  // TODO make configurable
        // .setUpdateSnapshots(updateSnapshots) // TODO make configurable
        // .setGlobalChecksumPolicy(globalChecksumPolicy)  // TODO
        // .addActiveProfiles(activeProfiles ) // TODO make configurable
        // .addInactiveProfiles(inactiveProfiles ) // TODO make configurable
        .setTransferListener(transferListener)
        .addEventMonitor(consoleEventMonitor)
        ;
    return executionRequest;
  }
  
  public Settings getMavenSettings() {
    if(mavenSettings==null) {
      loadMavenSettings();
    }
    return mavenSettings;
  }

  public void invalidateMavenSettings() {
    mavenSettings = null;
  }
  
  private void loadMavenSettings() {
    try {
      mavenSettings = (Settings) executeInEmbedder(new MavenEmbedderCallback() {
          public Object run(MavenEmbedder embedder, IProgressMonitor monitor) throws Exception {
            File userSettingsPath = embedder.getUserSettingsPath( null );  // TODO
            File globalSettingsFile = embedder.getGlobalSettingsPath();

            Settings settings = embedder.buildSettings( 
                            userSettingsPath, 
                            globalSettingsFile, 
                            false, // interactive
                            false, // offline,
                            false, // usePluginRegistry,
                            Boolean.FALSE );  // pluginUpdateOverride );
            
            if(settings.getLocalRepository()==null) {
              settings.setLocalRepository(embedder.getLocalRepositoryPath(settings));
            }
            
            return settings;
          }
        }, new NullProgressMonitor());
    } catch(CoreException ex) {
      getConsole().logError("Unable to load Maven settings; "+ex.getMessage()); 
    }
  }

  public MavenModelManager getMavenModelManager() {
    return mavenModelManager;
  }

  public MavenRepositoryIndexManager getMavenRepositoryIndexManager() {
    // TODO Auto-generated method getIndexManager
    return mavenRepositoryIndexManager;
  }
  
  /**
   * Helper method that can be used to add the Maven2 natures (to enable the
   * tooling) to a given eclipse project, this is useful as it allows other
   * plug-in or wizards outside of the plugin to add the requires natures to
   * enable the tooling automatically
   * 
   * @param project
   */
  /*
  // TODO this is not ready to be a public API. 
     It should apply changes only if they are required, to avoid project rebuild on buildpath change
        
  public void addNatures( IProject project ) throws CoreException {
    ArrayList newNatures = new ArrayList();
    newNatures.add( JavaCore.NATURE_ID );
    newNatures.add( Maven2Plugin.NATURE_ID );

    IProjectDescription description = project.getDescription();
    String[] natures = description.getNatureIds();
    for( int i = 0; i < natures.length; ++i ) {
      String id = natures[i];
      if( !Maven2Plugin.NATURE_ID.equals( id ) && !JavaCore.NATURE_ID.equals( natures[i] ) ) {
        newNatures.add( natures[i] );
      }
    }
    description.setNatureIds( ( String[] ) newNatures.toArray( new String[newNatures.size()] ) );
    project.setDescription( description, null );

    IJavaProject javaProject = JavaCore.create( project );
    if( javaProject != null ) {
      // remove classpatch container from JavaProject
      IClasspathEntry[] entries = javaProject.getRawClasspath();
      ArrayList newEntries = new ArrayList();
      for( int i = 0; i < entries.length; i++ ) {
        if( !Maven2ClasspathContainer.isMaven2ClasspathContainer( entries[i].getPath() ) ) {
          newEntries.add( entries[i] );
        }
      }
      newEntries.add( JavaCore.newContainerEntry( new Path( Maven2Plugin.CONTAINER_ID ) ) );
      javaProject.setRawClasspath( ( IClasspathEntry[] ) newEntries.toArray( new IClasspathEntry[newEntries.size()] ), null );
    }
  }
  */

  /**
   * Provides the ability to update the source folders on a given project based upon the information in the 
   * Maven Project Object Model (POM)
   * 
   * @param project
   */
  /*
  // TODO this is not ready to be a public API.
     This method should actually have code from UpdateSourcesJob and that job should be 
     using it to avoid cyclic dependencies between classes. See resolveClasspathEntries() method.
     
  public void updateSourceFolders( IProject project ) {
    UpdateSourcesAction.UpdateSourcesJob job = new UpdateSourcesAction.UpdateSourcesJob(project);
    try {
      job.join();
    } catch (Exception e) {
      throw new RuntimeException("Unable to update source folders on project "+project.getName(),e);
    }
  }
  */
  
}

