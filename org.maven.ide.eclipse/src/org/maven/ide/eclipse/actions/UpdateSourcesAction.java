
package org.maven.ide.eclipse.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.wagon.events.TransferListener;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.MavenEmbedderCallback;
import org.maven.ide.eclipse.PluginConsoleEventMonitor;
import org.maven.ide.eclipse.TransferListenerAdapter;


public class UpdateSourcesAction implements IObjectActionDelegate {
  private IAction action;
  private IWorkbenchPart targetPart;
  private ISelection selection;
  

  public void setActivePart( IAction action, IWorkbenchPart targetPart ) {
    this.action = action;
    this.targetPart = targetPart;
  }

  public void selectionChanged( IAction action, ISelection selection ) {
    this.action = action;
    this.selection = selection;
  }

  
  public void run( IAction action ) {
    IStructuredSelection structuredSelection = ( IStructuredSelection ) selection;
    for( Iterator it = structuredSelection.iterator(); it.hasNext(); ) {
      Object element = it.next();
      IProject project = null;
      if( element instanceof IProject ) {
        project = ( IProject ) element;
      } else if( element instanceof IAdaptable ) {
        project = ( IProject ) ( ( IAdaptable ) element ).getAdapter( IProject.class );
      }
      if( project != null ) {
        new UpdateSourcesJob( project).schedule();
      }
    }
  }

  
  private static final class UpdateSourcesJob extends Job {
    private final IProject project;


    private UpdateSourcesJob( IProject project) {
      super( "Updating "+project.getName()+" Sources");
      this.project = project;
    }

    protected IStatus run( IProgressMonitor monitor ) {
      IFile pom = project.getFile( Maven2Plugin.POM_FILE_NAME);
      if( !pom.exists()) {
        return Status.OK_STATUS;
      }
      
      Maven2Plugin plugin = Maven2Plugin.getDefault();
      try {        
        List sourceEntries = new ArrayList(); 
        Map options = new HashMap();
        resolveSourceEntries(sourceEntries, options, project, pom, true, monitor);
        
        Set sources = new HashSet();
        for( Iterator it = sourceEntries.listIterator(); it.hasNext(); ) {
          IClasspathEntry entry = ( IClasspathEntry ) it.next();
          if(!sources.add( entry.getPath().toString()) ) {
              it.remove();
          }
        }
        
        IJavaProject javaProject = JavaCore.create(project);
        
        setOption( javaProject, options, JavaCore.COMPILER_COMPLIANCE );
        setOption( javaProject, options, JavaCore.COMPILER_SOURCE);
        setOption( javaProject, options, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM);
        
        String source = ( String ) options.get( JavaCore.COMPILER_SOURCE );
        if(source==null) {
          sourceEntries.add( JavaRuntime.getDefaultJREContainerEntry() );
        } else {
          sourceEntries.add( getJREContainer( source ) );
        }
        
        IClasspathEntry[] currentClasspath = javaProject.getRawClasspath();
        for( int i = 0; i < currentClasspath.length; i++ ) {
          // Delete all non container (e.g. JRE library) entries. See MNGECLIPSE-9 
          IClasspathEntry entry = currentClasspath[i];
          if( entry.getEntryKind()==IClasspathEntry.CPE_CONTAINER ) {
            if( !JavaRuntime.JRE_CONTAINER.equals( entry.getPath().segment( 0 ) )) {
              sourceEntries.add( entry);
            }
          }
        }
        
        IClasspathEntry[] entries = ( IClasspathEntry[] ) sourceEntries.toArray( new IClasspathEntry[ sourceEntries.size()]);
        javaProject.setRawClasspath(entries, monitor);
        
        plugin.getConsole().logMessage("Updated source folders for project "+project.getName());

      } catch( Exception ex ) {
        String msg = "Unable to update project source folders "+ex.toString();
        plugin.getConsole().logMessage( msg );
        Maven2Plugin.log( msg, ex);
      }
      
      return Status.OK_STATUS;
    }

    private void setOption( IJavaProject javaProject, Map options, String name ) {
      String newValue = ( String ) options.get( name );
      if(newValue==null) {
        return;
      }
      String currentValue = javaProject.getOption( name, false );
      if(!newValue.equals( currentValue )) {
        javaProject.setOption( name, newValue );
      }
    }
    
    private IClasspathEntry getJREContainer( String version ) {
      int n = VERSIONS.indexOf( version );
      if(n>-1) {
        Map jreContainers = getJREContainers();
        for( int i = n; i < VERSIONS.size(); i++ ) {
          IClasspathEntry entry = ( IClasspathEntry ) jreContainers.get( version );
          if(entry!=null) {
            Maven2Plugin.getDefault().getConsole().logMessage( "JRE compliant to "+version+". "+entry );
            return entry;
          }
        }
      }
      IClasspathEntry entry = JavaRuntime.getDefaultJREContainerEntry();
      Maven2Plugin.getDefault().getConsole().logMessage( "No JRE compliant to "+version+". Using default JRE container "+entry );
      return entry;
    }
    
    private Map getJREContainers() {
      Map jreContainers = new HashMap();
      
      jreContainers.put( getJREVersion( JavaRuntime.getDefaultVMInstall() ), JavaRuntime.getDefaultJREContainerEntry());
      
      IVMInstallType[] installTypes = JavaRuntime.getVMInstallTypes();
      for( int i = 0; i < installTypes.length; i++ ) {
        IVMInstall[] installs = installTypes[i].getVMInstalls();
        for( int j = 0; j < installs.length; j++ ) {
          IVMInstall install = installs[j];
          String version = getJREVersion(install);
          if(!jreContainers.containsKey( version )) {
            jreContainers.put(version, JavaCore.newContainerEntry( JavaRuntime.newJREContainerPath( install ) ));
          }
        }
      }
      
      return jreContainers;
    }

    private String getJREVersion( IVMInstall install ) {
      LibraryLocation[] libraryLocations = JavaRuntime.getLibraryLocations( install );
      if( libraryLocations != null ) {
        for( int k = 0; k < libraryLocations.length; k++ ) {
          IPath path = libraryLocations[k].getSystemLibraryPath();
          String jarName = path.lastSegment();
          if( "rt.jar".equals( jarName ) ) {
            try {
              JarFile jarFile = new JarFile( path.toFile() );
              Manifest manifest = jarFile.getManifest();
              Attributes attributes = manifest.getMainAttributes();
              return attributes.getValue( Attributes.Name.SPECIFICATION_VERSION );
            } catch( Exception ex ) {
              Maven2Plugin.getDefault().getConsole().logError( "Unable to read "+path+" "+ex.getMessage() );
            }
          }
        }
      }
      return null;
    }
    
    
    public void resolveSourceEntries(final List sourceEntries, final Map options, final IProject project, final IResource pomFile, final boolean recursive, IProgressMonitor monitor) {
      // Tracer.trace(this, "resolveSourceEntries in project:"+project+" for pom:"+pomFile);
        
      if(monitor.isCanceled()) return;
    
      final Maven2Plugin plugin = Maven2Plugin.getDefault();
      
      MavenProject mavenProject = ( MavenProject ) plugin.executeInEmbedder("Reading Project", new MavenEmbedderCallback() {
          public Object run( MavenEmbedder mavenEmbedder, IProgressMonitor monitor ) {
            File f = pomFile.getLocation().toFile();
            
            MavenProject mavenProject;
            try {
              String msg = "Reading "+pomFile.getFullPath();
              monitor.beginTask( msg, IProgressMonitor.UNKNOWN);
              plugin.getConsole().logMessage( msg);
              mavenProject = mavenEmbedder.readProject(f);
            } catch( Exception ex) {
              String msg = "Unable to read project "+pomFile.getFullPath();
              plugin.getConsole().logError(msg);
              return null;
            } finally {
              monitor.done();
            }
    
            String source = getBuildOption( mavenProject, "maven-compiler-plugin", "source" );
            if(source!=null) {
              plugin.getConsole().logMessage( "Setting source compatibility: "+source );
              setVersion( options, JavaCore.COMPILER_SOURCE, source);
              setVersion( options, JavaCore.COMPILER_COMPLIANCE, source );
            }
            
            String target = getBuildOption( mavenProject, "maven-compiler-plugin", "target" );
            if(target!=null) {
              plugin.getConsole().logMessage( "Setting target compatibility: "+source );
              setVersion( options, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, target );
            }
            
            List goals = Arrays.asList( "generate-sources,generate-resources".split(","));
            // TODO hook up console view
            EventMonitor eventMonitor = new PluginConsoleEventMonitor();
            Properties properties = new Properties();
              
            try {
              String msg = "Generating sources for "+pomFile.getFullPath();
              plugin.getConsole().logMessage( msg);
              monitor.beginTask( "", IProgressMonitor.UNKNOWN);
              TransferListener transferListener = new TransferListenerAdapter( monitor );
              mavenEmbedder.execute(mavenProject, goals, eventMonitor, transferListener, properties, f.getParentFile());
            } catch( Exception ex ) {
              String msg = "Failed to run generate source goals "+pomFile.getFullPath()+" "+ex.getMessage();
              plugin.getConsole().logError(msg);
            } finally {
              monitor.done();
            }
            
            IContainer parent = pomFile.getParent();
            try {
              parent.refreshLocal( IResource.DEPTH_INFINITE, monitor );
            } catch( CoreException ex ) {
              plugin.getConsole().logError("Unable to refresh folder "+parent.getFullPath()+"; "+ex.getMessage());
            }
            
            File basedir = pomFile.getLocation().toFile().getParentFile();
            File projectBaseDir = project.getLocation().toFile();
            
            extractSourceDirs(sourceEntries, project, mavenProject.getCompileSourceRoots(), basedir, projectBaseDir);
            extractSourceDirs(sourceEntries, project, mavenProject.getTestCompileSourceRoots(), basedir, projectBaseDir);
            
            extractResourceDirs(sourceEntries, project, mavenProject.getBuild().getResources(), basedir, projectBaseDir);
            extractResourceDirs(sourceEntries, project, mavenProject.getBuild().getTestResources(), basedir, projectBaseDir);
            
            return mavenProject;
          }

        });
      
      if( mavenProject!=null && recursive) {
        IContainer parent = pomFile.getParent();      
        List modules = mavenProject.getModules();
        for( Iterator it = modules.iterator(); it.hasNext() && !monitor.isCanceled(); ) {
          String module = ( String ) it.next();
          IResource memberPom = parent.findMember( module+"/"+Maven2Plugin.POM_FILE_NAME); //$NON-NLS-1$
          if(memberPom!=null) {
            resolveSourceEntries(sourceEntries, options, project, memberPom, true, monitor);
          }
        }
      }
    }
      
    public static final List VERSIONS = Arrays.asList( "1.1,1.2,1.3,1.4,1.5,1.6,1.7".split( "," ) ); 
    
    static void setVersion( Map options, String name, String value ) {
      if(value==null) {
        return;
      }
      String current = ( String ) options.get( name );
      if(current==null) {
        options.put( name, value );
      } else {
        int oldIndex = VERSIONS.indexOf( current );
        int newIndex = VERSIONS.indexOf( value.trim() );
        if(newIndex>oldIndex) {
          options.put( name, value );
        }
      }
    }

    void extractSourceDirs( List entries, IProject project, List sourceRoots, File basedir, File projectBaseDir ) {
      for( Iterator it = sourceRoots.iterator(); it.hasNext(); ) {
        String sourceRoot = ( String ) it.next();
        if( new File( sourceRoot ).isDirectory() ) {
          IResource r = project.findMember(toRelativeAndFixSeparator( projectBaseDir, sourceRoot ));
          if(r!=null) {
            entries.add( JavaCore.newSourceEntry( r.getFullPath() /*, new IPath[] { new Path( "**"+"/.svn/"+"**")} */) );
            Maven2Plugin.getDefault().getConsole().logMessage( "Adding source folder " + r.getFullPath() );
          }
        }
      }
    }

    void extractResourceDirs( List entries, IProject project, List resources, File basedir, File projectBaseDir ) {
      for( Iterator it = resources.iterator(); it.hasNext(); ) {
        Resource resource = ( Resource ) it.next();
        File resourceDirectory = new File( resource.getDirectory() );
        if( resourceDirectory.exists() && resourceDirectory.isDirectory() ) {
          IResource r = project.findMember(toRelativeAndFixSeparator( projectBaseDir, resource.getDirectory() ));
          if(r!=null) {
            entries.add( JavaCore.newSourceEntry( r.getFullPath(), new IPath[] {}, r.getFullPath()));  //, new IPath[] { new Path( "**"+"/.svn/"+"**")} ) );
            Maven2Plugin.getDefault().getConsole().logMessage( "Adding resource folder " + r.getFullPath() );
          }
        }
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
      return relative.replace( '\\', '/' ); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static String getBuildOption( MavenProject project, String artifactId, String optionName ) {
      for( Iterator it = project.getModel().getBuild().getPlugins().iterator(); it.hasNext(); ) {
        Plugin plugin = ( Plugin ) it.next();
        if( artifactId.equals( plugin.getArtifactId() ) ) {
          Xpp3Dom o = ( Xpp3Dom ) plugin.getConfiguration();
          if( o != null && o.getChild( optionName ) != null ) {
            return o.getChild( optionName ).getValue();
          }
        }
      }
      return null;
    }
    
  }

}

