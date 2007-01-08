
package org.maven.ide.eclipse.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.embedder.EmbedderFactory;
import org.maven.ide.eclipse.embedder.PluginConsoleEventMonitor;
import org.maven.ide.eclipse.embedder.PluginConsoleMavenEmbeddedLogger;
import org.maven.ide.eclipse.embedder.TransferListenerAdapter;
import org.maven.ide.eclipse.index.MavenRepositoryIndexManager;
import org.maven.ide.eclipse.launch.console.Maven2Console;
import org.maven.ide.eclipse.preferences.Maven2PreferenceConstants;


public class UpdateSourcesAction implements IObjectActionDelegate {
  private ISelection selection;

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

  public void selectionChanged(IAction action, ISelection selection) {
    this.selection = selection;
  }

  public void run(IAction action) {
    IStructuredSelection structuredSelection = (IStructuredSelection) selection;
    for(Iterator it = structuredSelection.iterator(); it.hasNext();) {
      Object element = it.next();
      IProject project = null;
      if(element instanceof IProject) {
        project = (IProject) element;
      } else if(element instanceof IAdaptable) {
        project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
      }
      if(project != null) {
        new UpdateSourcesJob(project).schedule();
      }
    }
  }

  
  private static final class UpdateSourcesJob extends Job {
    private final IProject project;
    private final Maven2Console console;
    private final MavenRepositoryIndexManager indexManager;

    private List sourceEntries = new ArrayList(); 
    private Set sources = new HashSet();
    private Map options = new HashMap();

    UpdateSourcesJob(IProject project) {
      super("Updating " + project.getName() + " Sources");
      this.project = project;
      
      Maven2Plugin plugin = Maven2Plugin.getDefault();
      this.console = plugin.getConsole();
      this.indexManager = plugin.getMavenRepositoryIndexManager();
    }

    protected IStatus run(IProgressMonitor monitor) {
      IFile pom = project.getFile(Maven2Plugin.POM_FILE_NAME);
      if(!pom.exists()) {
        return Status.OK_STATUS;
      }

      monitor.beginTask("Updating sources " + project.getName(), IProgressMonitor.UNKNOWN);
      try {
        collectSourceEntries(monitor);

        // TODO optimize project refresh
        monitor.subTask("Refreshing");
        project.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1));

        monitor.subTask("Configuring Build Path");
        IJavaProject javaProject = JavaCore.create(project);

        setOption(javaProject, options, JavaCore.COMPILER_COMPLIANCE);
        setOption(javaProject, options, JavaCore.COMPILER_SOURCE);
        setOption(javaProject, options, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM);

        String source = (String) options.get(JavaCore.COMPILER_SOURCE);
        if(source == null) {
          sourceEntries.add(JavaRuntime.getDefaultJREContainerEntry());
        } else {
          sourceEntries.add(getJREContainer(source));
        }

        IClasspathEntry[] currentClasspath = javaProject.getRawClasspath();
        for(int i = 0; i < currentClasspath.length; i++ ) {
          // Delete all non container (e.g. JRE library) entries. See MNGECLIPSE-9 
          IClasspathEntry entry = currentClasspath[i];
          if(entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
            if(!JavaRuntime.JRE_CONTAINER.equals(entry.getPath().segment(0))) {
              sourceEntries.add(entry);
            }
          }
        }

        IClasspathEntry[] entries = (IClasspathEntry[]) sourceEntries.toArray(new IClasspathEntry[sourceEntries.size()]);
        javaProject.setRawClasspath(entries, monitor);

        console.logMessage("Updated source folders for project " + project.getName());

      } catch(Exception ex) {
        console.logMessage("Unable to update source folders " + project.getName() + "; " + ex.toString());
      } finally {
        monitor.done();
      }

      return Status.OK_STATUS;
    }

    private void setOption(IJavaProject javaProject, Map options, String name) {
      String newValue = (String) options.get(name);
      if(newValue == null) {
        return;
      }
      String currentValue = javaProject.getOption(name, false);
      if(!newValue.equals(currentValue)) {
        javaProject.setOption(name, newValue);
      }
    }

    private IClasspathEntry getJREContainer(String version) {
      int n = VERSIONS.indexOf(version);
      if(n > -1) {
        Map jreContainers = getJREContainers();
        for(int i = n; i < VERSIONS.size(); i++ ) {
          IClasspathEntry entry = (IClasspathEntry) jreContainers.get(version);
          if(entry != null) {
            console.logMessage("JRE compliant to " + version + ". " + entry);
            return entry;
          }
        }
      }
      IClasspathEntry entry = JavaRuntime.getDefaultJREContainerEntry();
      console.logMessage("No JRE compliant to " + version + ". Using default JRE container " + entry);
      return entry;
    }

    private Map getJREContainers() {
      Map jreContainers = new HashMap();

      jreContainers.put(getJREVersion(JavaRuntime.getDefaultVMInstall()), JavaRuntime.getDefaultJREContainerEntry());

      IVMInstallType[] installTypes = JavaRuntime.getVMInstallTypes();
      for(int i = 0; i < installTypes.length; i++ ) {
        IVMInstall[] installs = installTypes[i].getVMInstalls();
        for(int j = 0; j < installs.length; j++ ) {
          IVMInstall install = installs[j];
          String version = getJREVersion(install);
          if(!jreContainers.containsKey(version)) {
            // in Eclipse 3.2 one could use JavaRuntime.newJREContainerPath(install)
            IPath jreContainerPath = new Path(JavaRuntime.JRE_CONTAINER).append(install.getVMInstallType().getId())
                .append(install.getName());
            jreContainers.put(version, JavaCore.newContainerEntry(jreContainerPath));
          }
        }
      }

      return jreContainers;
    }

    private String getJREVersion(IVMInstall install) {
      LibraryLocation[] libraryLocations = JavaRuntime.getLibraryLocations(install);
      if(libraryLocations != null) {
        for(int k = 0; k < libraryLocations.length; k++ ) {
          IPath path = libraryLocations[k].getSystemLibraryPath();
          String jarName = path.lastSegment();
          // TODO that won't be the case on Mac
          if("rt.jar".equals(jarName)) {
            try {
              JarFile jarFile = new JarFile(path.toFile());
              Manifest manifest = jarFile.getManifest();
              Attributes attributes = manifest.getMainAttributes();
              return attributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
            } catch(Exception ex) {
              console.logError("Unable to read " + path + " " + ex.getMessage());
            }
          }
        }
      }
      return null;
    }

    private void collectSourceEntries(IProgressMonitor monitor) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      Maven2Plugin plugin = Maven2Plugin.getDefault();
      IPreferenceStore preferenceStore = plugin.getPreferenceStore();
      boolean offline = preferenceStore.getBoolean(Maven2PreferenceConstants.P_OFFLINE);
      boolean debug = preferenceStore.getBoolean(Maven2PreferenceConstants.P_DEBUG_OUTPUT);
      
      MavenEmbedder mavenEmbedder;
      try {
        mavenEmbedder = EmbedderFactory.createMavenEmbedder(EmbedderFactory.createExecutionCustomizer(),
            new PluginConsoleMavenEmbeddedLogger(console, debug));
      } catch(MavenEmbedderException ex) {
        console.logError("Unable to create embedder; " + ex.toString());
        return;
      }

      IFile pomResource = project.getFile(Maven2Plugin.POM_FILE_NAME);

      console.logMessage("Reading " + pomResource.getFullPath());

      monitor.subTask("Reading " + pomResource.getFullPath());
      File pomFile = pomResource.getLocation().toFile();

      MavenProject mavenProject;
      try {
        mavenProject = mavenEmbedder.readProject(pomFile);
      } catch(Exception ex) {
        console.logError("Unable to read project " + pomResource.getFullPath() + "; " + ex.toString());
        return;
      }

      String source = getBuildOption(mavenProject, "maven-compiler-plugin", "source");
      if(source != null) {
        console.logMessage("Setting source compatibility: " + source);
        setVersion(options, JavaCore.COMPILER_SOURCE, source);
        setVersion(options, JavaCore.COMPILER_COMPLIANCE, source);
      }

      String target = getBuildOption(mavenProject, "maven-compiler-plugin", "target");
      if(target != null) {
        console.logMessage("Setting target compatibility: " + source);
        setVersion(options, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, target);
      }

      File basedir = pomResource.getLocation().toFile().getParentFile();
      File projectBaseDir = project.getLocation().toFile();
      
      monitor.subTask("Generating Sources " + pomResource.getFullPath());
      try {
        console.logMessage("Generating sources " + pomResource.getFullPath());

        MavenExecutionRequest request = EmbedderFactory.createMavenExecutionRequest(mavenEmbedder, offline, debug);
        
        request.setBaseDirectory(pomFile.getParentFile());
        // request.setPomFile(pomFile.getAbsolutePath());
        // request.setGoals(Arrays.asList("generate-sources,generate-resources,generate-test-sources,generate-test-resources".split(",")));
        request.setGoals(Collections.singletonList("process-test-resources"));
        // request.setProfiles(...);
        request.addEventMonitor(new PluginConsoleEventMonitor(console));
        request.setTransferListener(new TransferListenerAdapter(monitor, console, indexManager));
        // request.setReactorFailureBehavior(MavenExecutionRequest.REACTOR_FAIL_AT_END);
        
        MavenExecutionResult result = mavenEmbedder.execute(request);
        
        ReactorManager reactorManager = result.getReactorManager();
        if(reactorManager!=null && reactorManager.getSortedProjects()!=null) {
          for(Iterator it = reactorManager.getSortedProjects().iterator(); it.hasNext();) {
            addDirs((MavenProject) it.next(), basedir, projectBaseDir);
          }
        }
        
        if(result.hasExceptions()) {
          for(Iterator it = result.getExceptions().iterator(); it.hasNext();) {
            Exception ex = (Exception) it.next();
            console.logError("Build error for " + pomResource.getFullPath() + "; " + ex.toString());
          }
        }

      } catch(Exception ex) {
        console.logError("Build error for " + pomResource.getFullPath() + "; " + ex.toString());
        ex.printStackTrace();

        addDirs(mavenProject, basedir, projectBaseDir);
      }
    }

    private void addDirs(MavenProject mavenProject, File basedir, File projectBaseDir) {
      addSourceDirs(mavenProject.getCompileSourceRoots(), basedir, projectBaseDir);
      addSourceDirs(mavenProject.getTestCompileSourceRoots(), basedir, projectBaseDir);

      addResourceDirs(mavenProject.getBuild().getResources(), basedir, projectBaseDir);
      addResourceDirs(mavenProject.getBuild().getTestResources(), basedir, projectBaseDir);
    }

    void addSourceDirs(List sourceRoots, File basedir, File projectBaseDir) {
      for(Iterator it = sourceRoots.iterator(); it.hasNext();) {
        String sourceRoot = (String) it.next();
        if(new File(sourceRoot).isDirectory()) {
          IResource r = project.findMember(toRelativeAndFixSeparator(projectBaseDir, sourceRoot));
          if(r != null && sources.add(r.getFullPath().toString())) {
            sourceEntries.add(JavaCore
                .newSourceEntry(r.getFullPath() /*, new IPath[] { new Path( "**"+"/.svn/"+"**")} */));
            console.logMessage("Adding source folder " + r.getFullPath());
          }
        }
      }
    }

    void addResourceDirs(List resources, File basedir, File projectBaseDir) {
      for(Iterator it = resources.iterator(); it.hasNext();) {
        Resource resource = (Resource) it.next();
        File resourceDirectory = new File(resource.getDirectory());
        if(resourceDirectory.exists() && resourceDirectory.isDirectory()) {
          IResource r = project.findMember(toRelativeAndFixSeparator(projectBaseDir, resource.getDirectory()));
          if(r != null && sources.add(r.getFullPath().toString())) {
            sourceEntries.add(JavaCore.newSourceEntry(r.getFullPath(), new IPath[] {new Path("**")}, r.getFullPath())); //, new IPath[] { new Path( "**"+"/.svn/"+"**")} ) );
            console.logMessage("Adding resource folder " + r.getFullPath());
          }
        }
      }
    }

    private String toRelativeAndFixSeparator(File basedir, String absolutePath) {
      String relative;
      if(absolutePath.equals(basedir.getAbsolutePath())) {
        relative = ".";
      } else if(absolutePath.startsWith(basedir.getAbsolutePath())) {
        relative = absolutePath.substring(basedir.getAbsolutePath().length() + 1);
      } else {
        relative = absolutePath;
      }
      return relative.replace('\\', '/'); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static String getBuildOption(MavenProject project, String artifactId, String optionName) {
      for(Iterator it = project.getModel().getBuild().getPlugins().iterator(); it.hasNext();) {
        Plugin plugin = (Plugin) it.next();
        if(artifactId.equals(plugin.getArtifactId())) {
          Xpp3Dom o = (Xpp3Dom) plugin.getConfiguration();
          if(o != null && o.getChild(optionName) != null) {
            return o.getChild(optionName).getValue();
          }
        }
      }
      return null;
    }

    
    public static final List VERSIONS = Arrays.asList("1.1,1.2,1.3,1.4,1.5,1.6,1.7".split(","));

    static void setVersion(Map options, String name, String value) {
      if(value == null) {
        return;
      }
      String current = (String) options.get(name);
      if(current == null) {
        options.put(name, value);
      } else {
        int oldIndex = VERSIONS.indexOf(current);
        int newIndex = VERSIONS.indexOf(value.trim());
        if(newIndex > oldIndex) {
          options.put(name, value);
        }
      }
    }
    
  }

}
