
package org.maven.ide.eclipse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidArtifactRTException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.embedder.MavenEmbedderLogger;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.maven.ide.eclipse.index.Indexer;
import org.maven.ide.eclipse.launch.console.Maven2Console;
import org.maven.ide.eclipse.preferences.Maven2PreferenceConstants;
import org.maven.ide.eclipse.util.ITraceable;
import org.maven.ide.eclipse.util.Tracer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;


/**
 * Maven2Plugin main plugin class.
 */
public class Maven2Plugin extends AbstractUIPlugin implements ITraceable {
  private static final boolean TRACE_ENABLED = Boolean.valueOf(Platform.getDebugOption("org.maven.ide.eclipse/plugin")).booleanValue();

  public static final String PLUGIN_ID = Maven2Plugin.class.getPackage().getName();
  public static final String CONTAINER_ID = PLUGIN_ID + ".MAVEN2_CLASSPATH_CONTAINER"; //$NON-NLS-1$
  public static final String NATURE_ID = PLUGIN_ID + ".maven2Nature"; //$NON-NLS-1$
  public static final String BUILDER_ID = PLUGIN_ID + ".maven2Builder"; //$NON-NLS-1$
  public static final String MARKER_ID = PLUGIN_ID + ".maven2Problem"; //$NON-NLS-1$

  public static final String POM_FILE_NAME = "pom.xml"; //$NON-NLS-1$

  public static final String[] DEFAULT_INDEXES = { "central"};  // default indexes //$NON-NLS-1$

  /** reise embedder instance or create new one on every operation */
  private static final boolean REUSE_EMBEDDER = false;
  
  // The shared instance.
  private static Maven2Plugin plugin;

  private MavenEmbedder mavenEmbedder;

  protected List indexes = Collections.synchronizedList( new ArrayList() );  

  /** console */
  private Maven2Console console;
  
  /**
   * The constructor.
   */
  public Maven2Plugin() {
    plugin = this;
  }

  /**
   * This method is called upon plug-in activation
   */
  public void start( final BundleContext context) throws Exception {
    super.start( context);

    try {
      this.console = new Maven2Console();
      
    } catch (RuntimeException ex) {
      log( new Status( IStatus.ERROR, PLUGIN_ID, -1, "Unable to start console: "+ex.toString(), ex));
    }
    
    executeInEmbedder(new MavenEmbedderCallback() {
        public Object run( MavenEmbedder mavenEmbedder, IProgressMonitor monitor ) {
          ArtifactRepository localRepository = mavenEmbedder.getLocalRepository();
          String baseDir = localRepository.getBasedir();
          
          IndexerJob indexerJob = new IndexerJob("local", baseDir, getIndexDir(), indexes);
          indexerJob.setPriority( Job.LONG );
          indexerJob.schedule(1000L);
          
          return null;
        }
      });

    UnpackerJob unpackerJob = new UnpackerJob(context.getBundle(), getIndexDir(), indexes);
    unpackerJob.setPriority( Job.LONG );
    unpackerJob.schedule(2000L);
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
    } else {
      return createEmbedder();
    }
  }

  public void resetMavenEmbedder() {
    stopEmbedder();
  }

  public Object executeInEmbedder(MavenEmbedderCallback template) {
    try {
      return template.run(getMavenEmbedder(), new NullProgressMonitor());
    } finally {
      if(!REUSE_EMBEDDER) stopEmbedder();
    }
  }
  
  public Object executeInEmbedder(String name, MavenEmbedderCallback template) {
    try {
      EmbedderJob job = new EmbedderJob( name, template, getMavenEmbedder() );
      job.schedule();
      try {
        job.join();
        // TODO check job.getResult()
        return job.getCallbackResult();        
      } catch( InterruptedException ex ) {
        getConsole().logError( "Interrupted "+ex.toString());
        return null;
      }
    } finally {
      if(!REUSE_EMBEDDER) stopEmbedder();
    }
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
  
  public Indexer getIndexer() {
    String[] indexNames = getIndexNames();
    
    File[] indexes = new File[ indexNames.length];
    for( int i = 0; i < indexes.length; i++ ) {
      indexes[ i] = new File( getIndexDir(), indexNames[ i]);
    }
    
    return new Indexer( indexes);
  }

  public String getIndexDir() {
    return new File( getStateLocation().toFile(), "index").getAbsolutePath();
  }
  
  // TODO implement index registry
  private String[] getIndexNames() {
    return ( String[] ) indexes.toArray( new String[indexes.size()] );
  }

  // TODO verify if index had been updated

  
  private MavenEmbedder createEmbedder() {
    IPreferenceStore store = this.getPreferenceStore();

    MavenEmbedder embedder = new MavenEmbedder();
    MavenEmbedderLogger logger = new ConsoleMavenEmbeddedLogger(getConsole());
    final boolean debugEnabled = store.getBoolean(Maven2PreferenceConstants.P_DEBUG_OUTPUT);
    logger.setThreshold(debugEnabled ? MavenEmbedderLogger.LEVEL_DEBUG : MavenEmbedderLogger.LEVEL_INFO);
    embedder.setLogger(logger);
    
    // TODO find a better ClassLoader
    // ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    embedder.setClassLoader( getClass().getClassLoader());  
    embedder.setInteractiveMode(false);
    
    // File localRepositoryDirectory = mavenEmbedder.getLocalRepositoryDirectory();
    String localRepositoryDir = store.getString( Maven2PreferenceConstants.P_LOCAL_REPOSITORY_DIR);
    if( localRepositoryDir!=null) {
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
 
    embedder.setCheckLatestPluginVersion(store.getBoolean( Maven2PreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION));
    embedder.setOffline(store.getBoolean( Maven2PreferenceConstants.P_OFFLINE));
    embedder.setUpdateSnapshots(store.getBoolean( Maven2PreferenceConstants.P_UPDATE_SNAPSHOTS));

    try {
      embedder.start();
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

  public void addDependency(final IFile file, final Dependency dependency) {
    // IFile file = project.getFile( new Path( Maven2Plugin.POM_FILE_NAME));

    executeInEmbedder(new MavenEmbedderCallback() {
        public Object run( MavenEmbedder mavenEmbedder, IProgressMonitor monitor ) {
          final File pom = file.getLocation().toFile();
          try {
            Model model = mavenEmbedder.readModel( pom);
            
            List dependencies = model.getDependencies();
            dependencies.add(dependency);
            
            StringWriter w = new StringWriter();
            mavenEmbedder.writeModel( w, model);
            
            file.setContents( new ByteArrayInputStream( w.toString().getBytes( "ASCII")), true, true, null);
            file.refreshLocal( IResource.DEPTH_ONE, null); // TODO ???
          } 
          catch (Exception ex) {
            log( "Unable to update POM: "+pom+"; "+ex.getMessage(), ex);
          }
          return null;
        }
      });
  }


  public void addMarker( IResource file, String message, int lineNumber, int severity) {
    try {
      deleteMarkers(file);
      
      IMarker marker = file.createMarker( Maven2Plugin.MARKER_ID);
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

  public void deleteMarkers( IResource file) {
    try {
      file.deleteMarkers( Maven2Plugin.MARKER_ID, false, IResource.DEPTH_ZERO);
    } catch( CoreException ex) {
      Maven2Plugin.log(ex);
    }
  }

  public void resolveClasspathEntries(Set libraryEntries, Set moduleArtifacts, 
      IFile pomFile, boolean recursive, IProgressMonitor monitor) {
    Tracer.trace(this, "resolveClasspathEntries from pom:"+pomFile);
      
    if(monitor.isCanceled()) return;
    
    String msg = "Reading "+pomFile.getFullPath();
    getConsole().logMessage( msg);
    
    final MavenProject mavenProject = (MavenProject) executeInEmbedder("Reading Project", new ReadProjectTask( pomFile ));
    if (mavenProject == null) {
      return;
    }

    try {
      deleteMarkers(pomFile);
      // TODO use version?
      moduleArtifacts.add( mavenProject.getGroupId()+":"+mavenProject.getArtifactId() );
      
      boolean downloadSources = this.getPreferenceStore().getBoolean( Maven2PreferenceConstants.P_DOWNLOAD_SOURCES );
      
      Set artifacts = mavenProject.getArtifacts();
      for( Iterator it = artifacts.iterator(); it.hasNext();) {
        final Artifact a = ( Artifact) it.next();
        // TODO use version?
        if(!moduleArtifacts.contains(a.getGroupId()+":"+a.getArtifactId()) &&
            // TODO verify if there is an Eclipse API to check that archive is acceptable
           ("jar".equals(a.getType()) || "zip".equals( a.getType() ))) {
          String artifactLocation = a.getFile().getAbsolutePath();
          
          // TODO add a lookup through workspace projects
          
          Path srcPath = null;

          File srcFile = new File(artifactLocation.substring( 0, artifactLocation.length()-4 )+"-sources.jar");
          if(srcFile.exists()) {
            // XXX ugly hack to do not download any sources
            srcPath = new Path(srcFile.getAbsolutePath());
          } else if (downloadSources) {
            srcPath = ( Path ) executeInEmbedder( new MavenEmbedderCallback() {
                  public Object run(MavenEmbedder mavenEmbedder, IProgressMonitor monitor) {
                      Artifact src = mavenEmbedder.createArtifactWithClassifier(a.getGroupId(), a.getArtifactId(), a.getVersion(), 
                          "java-source", "sources");
                      if (src != null) {
                        try {
                          mavenEmbedder.resolve(src, mavenProject.getRemoteArtifactRepositories(), mavenEmbedder.getLocalRepository());
                          return new Path(src.getFile().getAbsolutePath());
                        } catch( AbstractArtifactResolutionException ex ) {
                          String name = ex.getGroupId()+":"+ex.getArtifactId()+"-"+ex.getVersion()+"."+ex.getType();
                          getConsole().logError( ex.getOriginalMessage()+" "+name );
                        }
                      }
                      return null;
                  }
                }); 
          }
              
          libraryEntries.add( JavaCore.newLibraryEntry( new Path(artifactLocation), srcPath, null));
        }
      }
      
      if(recursive) {
        IContainer parent = pomFile.getParent();
        
        List modules = mavenProject.getModules();
        for( Iterator it = modules.iterator(); it.hasNext() && !monitor.isCanceled(); ) {
          String module = ( String ) it.next();
          IResource memberPom = parent.findMember( module+"/"+POM_FILE_NAME); //$NON-NLS-1$
          if(memberPom!=null && memberPom.getType() == IResource.FILE) {
            resolveClasspathEntries(libraryEntries, moduleArtifacts, (IFile)memberPom, true, monitor);
          }
        }    
      }

    } catch( InvalidArtifactRTException ex) {
      addMarker(pomFile, ex.getBaseMessage(), 1, IMarker.SEVERITY_ERROR); //$NON-NLS-1$
      
    } catch( Throwable ex) {
      addMarker(pomFile, ex.toString(), 1, IMarker.SEVERITY_ERROR); //$NON-NLS-1$
      
    }
    
    monitor.done();
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
      monitor.beginTask( "", IProgressMonitor.UNKNOWN );
      
      try {
        TransferListenerAdapter listener = new TransferListenerAdapter( monitor );
        return mavenEmbedder.readProjectWithDependencies(this.file.getLocation().toFile(), listener);
        
      } catch( ProjectBuildingException ex ) {
        handleProjectBuildingException( ex );
        return null;
      
      } catch( AbstractArtifactResolutionException ex ) {
        String name = ex.getGroupId()+":"+ex.getArtifactId()+"-"+ex.getVersion()+"."+ex.getType();
        String msg = ex.getOriginalMessage()+" "+name;
        Maven2Plugin.getDefault().addMarker(this.file, msg, 1, IMarker.SEVERITY_ERROR); //$NON-NLS-1$
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
        Maven2Plugin.getDefault().addMarker(this.file, msg, pex.getLineNumber(), IMarker.SEVERITY_ERROR); //$NON-NLS-1$
        Maven2Plugin.getDefault().getConsole().logError( msg +" at line "+ pex.getLineNumber());
      } else {
        String msg = Messages.getString("plugin.markerBuildError") + ex.getMessage();
        Maven2Plugin.getDefault().addMarker(this.file, msg, 1, IMarker.SEVERITY_ERROR); //$NON-NLS-1$
        Maven2Plugin.getDefault().getConsole().logError( msg);
      }
    }
  }


  private static final class EmbedderJob extends Job {
    private final MavenEmbedderCallback template;
    private final MavenEmbedder embedder;

    private Object callbackResult;

    private EmbedderJob( String name, MavenEmbedderCallback template, MavenEmbedder embedder ) {
      super( name );
      this.template = template;
      this.embedder = embedder;
    }

    protected IStatus run( IProgressMonitor monitor ) {
      callbackResult = this.template.run(this.embedder, monitor);
      return Status.OK_STATUS;
    }
    
    public Object getCallbackResult() {
      return this.callbackResult;
    }
  }


  private static class IndexerJob extends Job {
    private String repositoryName;
    private String repositoryDir;
    private String indexDir;
    private List indexes;
    
    public IndexerJob( String repositoryName, String repositoryDir, String indexDir, List indexes ) {
      super("Indexing "+repositoryName);
      this.repositoryName = repositoryName;
      this.repositoryDir = repositoryDir;
      this.indexDir = indexDir;
      this.indexes = indexes;
    }

    protected IStatus run( IProgressMonitor monitor ) {
      try {
        File file = new File(indexDir, repositoryName);
        if(!file.exists()) {
          file.mkdirs();
        }
      
        Indexer.reindex( file.getAbsolutePath(), repositoryDir, repositoryName , monitor);
        indexes.add( repositoryName );
        return Status.OK_STATUS;
        
      } catch( IOException ex ) {
        return new Status(IStatus.ERROR, PLUGIN_ID, -1, "Indexing error", ex);
      
      }
    }
    
  }

  
  private static class UnpackerJob extends Job {
    private final Bundle bundle;
    private final String indexDir;
    private final List indexes;

    public UnpackerJob( Bundle bundle, String indexDir, List indexes ) {
      super("Initializing indexes");
      this.bundle = bundle;
      this.indexDir = indexDir;
      this.indexes = indexes;
    }

    protected IStatus run( IProgressMonitor monitor ) {
      String[] indexNames = DEFAULT_INDEXES;
      for( int i = 0; i < indexNames.length; i++ ) {
        String name = indexNames[ i];
        
        File index = new File( indexDir, name);
        if(!index.exists()) {
          index.mkdirs();
        }
        
        monitor.subTask( name );
        URL indexArchive = bundle.getEntry( name+".zip");
        InputStream is = null;
        ZipInputStream zis = null;
        try {
          is = indexArchive.openStream();
          zis = new ZipInputStream( is );
          ZipEntry entry;
          byte[] buf = new byte[4096];
          while( ( entry = zis.getNextEntry() ) != null ) {
            File indexFile = new File( index, entry.getName() );
            FileOutputStream fos = null;
            try {
              fos = new FileOutputStream( indexFile );
              int n = 0;
              while( ( n = zis.read( buf ) ) != -1 ) {
                fos.write( buf, 0, n );
              }
            } finally {
              close( fos );
            }
          }
          indexes.add(name);
        } catch( Exception ex ) {
          log( new Status( IStatus.ERROR, PLUGIN_ID, -1, "Unable to initialize indexes", ex));

        } finally {
          close(zis);
          close(is);
        }
      }
      return Status.OK_STATUS;
    }
  
    private void close(InputStream is) {
      try {
        if(is!=null) is.close();
      } catch(IOException ex) {
      }
    }
    
    private void close(OutputStream os) {
      try {
        if(os!=null) os.close();
      } catch(IOException ex) {
      }
    }
  
  }
  
}

