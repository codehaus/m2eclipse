
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;
import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.wagon.events.TransferListener;

import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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

  
  
  // The shared instance.
  private static Maven2Plugin plugin;

  private MavenEmbedder mavenEmbedder;

  private List indexes = Collections.synchronizedList( new ArrayList() );  

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
  public void start( BundleContext context) throws Exception {
    super.start( context);
    
    IndexerJob indexerJob = new IndexerJob("local", getMavenEmbedder().getLocalRepository().getBasedir(), getIndexDir(), indexes);
    indexerJob.setPriority( Job.LONG );
    indexerJob.schedule(1000L);
      
    UnpackerJob unpackerJob = new UnpackerJob(context.getBundle(), getIndexDir(), indexes);
    unpackerJob.setPriority( Job.LONG );
    unpackerJob.schedule(2000L);
    
    try {
      this.console = new Maven2Console();
    
    } catch (RuntimeException ex) {
      log( new Status( IStatus.ERROR, PLUGIN_ID, -1, "Unable to start console", ex));
    }
  }

  /**
   * This method is called when the plug-in is stopped
   */
  public void stop( BundleContext context) throws Exception {
    super.stop( context);
    
    stopEmbedder();
    
    plugin = null;
  }

  /**
   * Returns the shared instance.
   */
  public static Maven2Plugin getDefault() {
    return plugin;
  }

  public synchronized MavenEmbedder getMavenEmbedder() {
    if( this.mavenEmbedder==null) {
      try {
        this.mavenEmbedder = createEmbedder();
      } catch( MavenEmbedderException e) {
        log( "Unable to start MavenEmbedder", e);
      }
    }
    return this.mavenEmbedder;
  }

  public void resetMavenEmbedder() {
    stopEmbedder();
  }
  
 
  public MavenProject getMavenProject( final IFile file, boolean withDependencies ) {
    MavenEmbedder mavenEmbedder = getMavenEmbedder();
    if( withDependencies ) {
      ProjectLoadingJob projectLoadingJob = new ProjectLoadingJob( "Reading project", file, mavenEmbedder );
      projectLoadingJob.schedule();
      try {
        projectLoadingJob.join();
      } catch( InterruptedException ex ) {
      }
      return projectLoadingJob.getMavenProject();
    } else {
      try {
        return mavenEmbedder.readProject( file.getLocation().toFile() );
      } catch( ProjectBuildingException e ) {
        return null;
      }
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

  
  private MavenEmbedder createEmbedder() throws MavenEmbedderException {
    MavenEmbedder embedder = new MavenEmbedder();
 
    // TODO find a better ClassLoader
    // ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    embedder.setClassLoader( getClass().getClassLoader());  
    embedder.setInteractiveMode(false);
    
    IPreferenceStore store = this.getPreferenceStore();
    
    // File localRepositoryDirectory = mavenEmbedder.getLocalRepositoryDirectory();
    String localRepositoryDir = store.getString( Maven2PreferenceConstants.P_LOCAL_REPOSITORY_DIR);
    if( localRepositoryDir!=null && localRepositoryDir.trim().length()>0) {
      embedder.setLocalRepositoryDirectory( new File(localRepositoryDir));
    }
    
    String globalChecksumPolicy = store.getString( Maven2PreferenceConstants.P_GLOBAL_CHECKSUM_POLICY);
    if( globalChecksumPolicy!=null && globalChecksumPolicy.trim().length()>0) {
      embedder.setGlobalChecksumPolicy(globalChecksumPolicy);
    }
 
    embedder.setCheckLatestPluginVersion(store.getBoolean( Maven2PreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION));
    embedder.setOffline(store.getBoolean( Maven2PreferenceConstants.P_OFFLINE));
    embedder.setPluginUpdateOverride(store.getBoolean( Maven2PreferenceConstants.P_PLUGIN_UPDATE_OVERRIDE));
    embedder.setUpdateSnapshots(store.getBoolean( Maven2PreferenceConstants.P_UPDATE_SNAPSHOTS));
    embedder.setUsePluginRegistry(store.getBoolean( Maven2PreferenceConstants.P_USE_PLUGIN_REGISTRY));

    embedder.start();
    
    return embedder;
  }

  private void stopEmbedder() {
    if( mavenEmbedder!=null) {
      try {
        mavenEmbedder.stop();
        mavenEmbedder = null;
      } catch( MavenEmbedderException e) {
        log( new Status( IStatus.ERROR, PLUGIN_ID, 0, 
            "Unable to stop MavenEmbedder", e.getCause()));
      }
    }
  }

  public void addDependency(IFile file, Dependency dependency) {
    // IFile file = project.getFile( new Path( Maven2Plugin.POM_FILE_NAME));
    File pom = file.getLocation().toFile();
    try {
      MavenEmbedder mavenEmbedder = getMavenEmbedder();
      Model model = mavenEmbedder.readModel( pom);
      
      List dependencies = model.getDependencies();
      dependencies.add(dependency);
      
      StringWriter w = new StringWriter();
      mavenEmbedder.writeModel( w, model);
      
      file.setContents( new ByteArrayInputStream( w.toString().getBytes( "ASCII")), true, true, null);
      file.refreshLocal( IResource.DEPTH_ONE, null); // TODO ???
      
    } catch (Exception ex) {
      log( "Unable to update POM "+pom+"; "+ex.getMessage(), ex);
      
    }
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

  public void resolveClasspathEntries(Set set, IResource pomFile, boolean recursive, IProgressMonitor monitor) {
    Tracer.trace(this, "resolveClasspathEntries from pom:"+pomFile);
      
    if(monitor.isCanceled()) return;
    
    monitor.subTask( "Reading "+pomFile.getFullPath());
    
    TransferListener transferListener = new TransferListenerAdapter( monitor );
    try {
      File f = pomFile.getLocation().toFile();
      
      MavenProject mavenProject = getMavenEmbedder().readProjectWithDependencies( f, transferListener);
      deleteMarkers(pomFile);
      
      Set artifacts = mavenProject.getArtifacts();
      for( Iterator it = artifacts.iterator(); it.hasNext();) {
        Artifact a = ( Artifact) it.next();
        set.add( JavaCore.newLibraryEntry( new Path( a.getFile().getAbsolutePath()), null, null));
      }
      
      if(recursive) {
        IContainer parent = pomFile.getParent();
        
        List modules = mavenProject.getModules();
        for( Iterator it = modules.iterator(); it.hasNext() && !monitor.isCanceled(); ) {
          String module = ( String ) it.next();
          IResource memberPom = parent.findMember( module+"/"+POM_FILE_NAME); //$NON-NLS-1$
          if(memberPom!=null) {
            resolveClasspathEntries(set, memberPom, true, monitor);
          }
        }    
      }

    } catch( ProjectBuildingException ex) {
      Throwable cause = ex.getCause();
      if( cause instanceof XmlPullParserException) {
        XmlPullParserException pex = ( XmlPullParserException ) cause;
        addMarker(pomFile, Messages.getString("plugin.markerParsingError") + pex.getMessage(), pex.getLineNumber(), IMarker.SEVERITY_ERROR); //$NON-NLS-1$
      } else {
        addMarker(pomFile, Messages.getString("plugin.markerBuildError") + ex.getMessage(), 1, IMarker.SEVERITY_ERROR); //$NON-NLS-1$
      }
      
    } catch( ArtifactResolutionException ex) {
      // log( "Artifact resolution error " + ex.getMessage(), ex);
      // addMarker(pomFile, Messages.getString("plugin.markerArtifactResolutionError") + ex.getMessage(), -1, IMarker.SEVERITY_ERROR); //$NON-NLS-1$
      String name = ex.getGroupId()+":"+ex.getArtifactId()+"-"+ex.getVersion()+"."+ex.getType();
      addMarker(pomFile, ex.getOriginalMessage()+" "+name, 1, IMarker.SEVERITY_ERROR); //$NON-NLS-1$
      
    }
  }

  
  public void resolveSourceEntries(Set sources, IProject project, IResource pomFile, boolean recursive, IProgressMonitor monitor) {
    Tracer.trace(this, "resolveSourceEntries in project:"+project+" for pom:"+pomFile);
      
    if(monitor.isCanceled()) return;
  
    MavenEmbedder mavenEmbedder = getMavenEmbedder();
    
    TransferListener transferListener = new TransferListenerAdapter( monitor );
    File f = pomFile.getLocation().toFile();
    
    MavenProject mavenProject;
    try {
      monitor.subTask( "Reading "+pomFile.getFullPath());
      mavenProject = mavenEmbedder.readProjectWithDependencies( f, transferListener);
    } catch( Exception ex) {
      log( "Unable to read project "+f, ex);
      return;
    }    

    List goals = Arrays.asList( "generate-sources,generate-resources".split(","));
    EventMonitor eventMonitor = new ConsoleEventMonitor();
    Properties properties = new Properties();
      
    try {
      monitor.subTask( "Executing "+pomFile.getFullPath());
      mavenEmbedder.execute(mavenProject, goals, eventMonitor, transferListener, properties, f.getParentFile());
    } catch( Exception ex ) {
      Tracer.trace(this, "Failed to run generate source goals "+f);
      log( "Failed to run generate source goals "+f, ex);
    }
    
    File basedir = pomFile.getLocation().toFile().getParentFile();
    File projectBaseDir = project.getLocation().toFile();
    
    extractSourceDirs(sources, mavenProject.getCompileSourceRoots(), basedir, projectBaseDir);
    extractSourceDirs(sources, mavenProject.getTestCompileSourceRoots(), basedir, projectBaseDir);
    
    extractResourceDirs(sources, mavenProject.getBuild().getResources(), basedir, projectBaseDir);
    extractResourceDirs(sources, mavenProject.getBuild().getTestResources(), basedir, projectBaseDir);
    
    if(recursive) {
      IContainer parent = pomFile.getParent();
      
      List modules = mavenProject.getModules();
      for( Iterator it = modules.iterator(); it.hasNext() && !monitor.isCanceled(); ) {
        String module = ( String ) it.next();
        IResource memberPom = parent.findMember( module+"/"+POM_FILE_NAME); //$NON-NLS-1$
        if(memberPom!=null) {
          resolveSourceEntries(sources, project, memberPom, true, monitor);
        }
      }    
    }
  }
    
  private void extractSourceDirs( Set directories, List sourceRoots, File basedir, File projectBaseDir ) {
    for( Iterator it = sourceRoots.iterator(); it.hasNext(); ) {
      String sourceRoot = ( String ) it.next();

      if( new File( sourceRoot ).isDirectory() ) {
        directories.add( toRelativeAndFixSeparator( projectBaseDir, sourceRoot ) );
      }
    }
  }

  private void extractResourceDirs( Set directories, List resources, File basedir, File projectBaseDir ) {
    for( Iterator it = resources.iterator(); it.hasNext(); ) {
      Resource resource = ( Resource ) it.next();

      File resourceDirectory = new File( resource.getDirectory() );

      if( !resourceDirectory.exists() || !resourceDirectory.isDirectory() ) {
        continue;
      }

      directories.add( toRelativeAndFixSeparator( projectBaseDir, resource.getDirectory() ) );
    }
  }

  private String toRelativeAndFixSeparator( File basedir, String absolutePath ) {
    String relative;
    if( absolutePath.equals( basedir.getAbsolutePath() ) ) {
      relative = ".";
    } else if( absolutePath.startsWith( basedir.getAbsolutePath() ) ) {
      relative = absolutePath.substring( basedir.getAbsolutePath().length() + 1 );
    } else {
      relative = absolutePath;
    }

    return StringUtils.replace( relative, "\\", "/" ); //$NON-NLS-1$ //$NON-NLS-2$
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
  

  private static final class ProjectLoadingJob extends Job {
    private final IFile file;
    private final MavenEmbedder embedder;
    private MavenProject mavenProject;

    private ProjectLoadingJob( String name, IFile file, MavenEmbedder embedder ) {
      super( name );
      this.file = file;
      this.embedder = embedder;
    }

    protected IStatus run( IProgressMonitor monitor ) {
      monitor.subTask( "Reading " + this.file.getFullPath() );
      try {
        TransferListenerAdapter listener = new TransferListenerAdapter( monitor );
        this.mavenProject = embedder.readProjectWithDependencies( this.file.getLocation().toFile(), listener );
      } catch( Exception ex ) {
        // ignore
      }
      return Status.OK_STATUS;
    }

    public MavenProject getMavenProject() {
      return mavenProject;
    }

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

