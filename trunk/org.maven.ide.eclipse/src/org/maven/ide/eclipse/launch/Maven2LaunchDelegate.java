
package org.maven.ide.eclipse.launch;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jdt.launching.sourcelookup.JavaProjectSourceLocation;
import org.eclipse.jdt.launching.sourcelookup.JavaSourceLocator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.preferences.Maven2PreferenceConstants;
import org.maven.ide.eclipse.util.ITraceable;
import org.maven.ide.eclipse.util.Tracer;


public class Maven2LaunchDelegate extends JavaLaunchDelegate implements Maven2LaunchConstants, ITraceable {
  private static final boolean TRACE_ENABLED = Boolean.valueOf(Platform.getDebugOption("org.maven.ide.eclipse/launcher")).booleanValue();
  
  private static final String MAVEN_EXECUTOR_CLASS = "org.maven.ide.eclipse.Maven2Executor";
  private static final String[] CLASSPATH_ENTRY = {
    "lib/maven-embedder-2.0.2-dep.jar",
    "bin",
    "m2plugin.jar"
  };
  
  public boolean isTraceEnabled() {
    return TRACE_ENABLED;
  }
  
  public String getMainTypeName( ILaunchConfiguration configuration ) {
    return MAVEN_EXECUTOR_CLASS;
  }
  
  public void launch( ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor ) throws CoreException {
    // just test
    Maven2Plugin.getDefault().getConsole().logMessage("Launching M2");
    launch.setSourceLocator(createSourceLocator());
    super.launch(configuration, "debug", launch, monitor);
  }
  
  // grab source locations from all open java projects
  // TODO: refactor to use 3.0 interfaces
  private ISourceLocator createSourceLocator()  throws CoreException {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IProject allProjects[] = root.getProjects();
    
    List/*<JavaProjectSourceLocation>*/ sourceLocation = new ArrayList();
    
    for( int i = 0; i < allProjects.length; i++ ) {
      IProject project = allProjects[i];
      if (project.isOpen() && project.hasNature("org.eclipse.jdt.core.javanature")) {
        IJavaProject javaProject = JavaCore.create(project);
        sourceLocation.add(new JavaProjectSourceLocation(javaProject));
      }
    }
    JavaProjectSourceLocation[] locationArray = (JavaProjectSourceLocation[])sourceLocation.toArray(new JavaProjectSourceLocation[]{}); 
    return new JavaSourceLocator(locationArray);
  }
  
  public String[] getClasspath( ILaunchConfiguration configuration ) {
    final URL rootURL = Maven2Plugin.getDefault().getRootURL(); 
    List cp = new ArrayList();
    
    for(int i = 0; i < CLASSPATH_ENTRY.length; i++) {
      try {
        cp.add(Platform.asLocalURL(new URL(rootURL, CLASSPATH_ENTRY[i])).getFile());
      }
      catch (Exception e) {
        //Tracer.trace(this, "Error adding classpath entry", CLASSPATH_ENTRY[i], e);
        Tracer.trace(this, "Error adding classpath entry", CLASSPATH_ENTRY[i]+" :"+e.getMessage());
      }
    }
    Tracer.trace(this, "classpath", cp);
    return (String[])cp.toArray(new String[0]);
  }
  
  public String getProgramArguments( ILaunchConfiguration configuration ) throws CoreException {
    String pomDirName = configuration.getAttribute(ATTR_POM_DIR, (String)null);
    pomDirName = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(pomDirName);
    Tracer.trace(this, "pomDirName", pomDirName);

    String sep = System.getProperty("file.separator"); //$NON-NLS-1$
    if (!pomDirName.endsWith(sep)) {
      pomDirName += sep;
    }
    String pomFileName = pomDirName+"pom.xml";
    // wrap file path with quotes to handle spaces
    if (pomFileName.indexOf(' ') >= 0) {
      pomFileName = '"'+pomFileName+'"';
    }
    Tracer.trace(this, "pomFileName", pomFileName);

    String goalsString = configuration.getAttribute(ATTR_GOALS, "");
    Tracer.trace(this, "goalsString", goalsString);

    return pomFileName+" "+goalsString;
  }
  
  public String getVMArguments( ILaunchConfiguration configuration ) throws CoreException {
    return super.getVMArguments(configuration)
      +" "+getProperties(configuration)
      +" "+getPreferences();
  }

  /**
   * Construct string with properties to pass to JVM as system properties 
   */
  private String getProperties(ILaunchConfiguration configuration) {
    List properties = null;
    try {
      properties = configuration.getAttribute(ATTR_PROPERTIES, Collections.EMPTY_LIST);
    } 
    catch(CoreException e) {
      String msg = "Exception while getting attribute "+ATTR_PROPERTIES+" from configuration";
      Maven2Plugin.log(msg, e);
      return "";
    }
    
    StringBuffer sb = new StringBuffer();
    for( Iterator iter = properties.iterator(); iter.hasNext(); ) {
      String[] s = ((String)iter.next()).split("=");
      String n = s[0];
      // substitute var if any
      String v = Maven2Plugin.substituteVar(s[1]);
      // enclose in quotes if spaces
      if (v.indexOf(' ') >= 0) {
        v = '"'+v+'"';
      }
      sb.append(" -D").append(n).append("=").append(v);
    }
    
    return sb.toString();
  }

  /**
   * Construct string with preferences to pass to JVM as system properties 
   */
  private String getPreferences() {
    StringBuffer sb = new StringBuffer();

    IPreferenceStore preferenceStore = Maven2Plugin.getDefault().getPreferenceStore();
    
    String s = preferenceStore.getString(Maven2PreferenceConstants.P_LOCAL_REPOSITORY_DIR);
    if (s != null && s.trim().length() > 0) {
      sb.append(" -D").append(Maven2PreferenceConstants.P_LOCAL_REPOSITORY_DIR).append("=\"").append(s).append('\"');
    }
    
    s = preferenceStore.getString(Maven2PreferenceConstants.P_GLOBAL_CHECKSUM_POLICY);
    if (s != null && s.trim().length() > 0) {
      sb.append(" -D").append(Maven2PreferenceConstants.P_GLOBAL_CHECKSUM_POLICY).append("=").append(s);
    }
     
    boolean b = preferenceStore.getBoolean(Maven2PreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION);
    sb.append(" -D").append(Maven2PreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION).append("=").append(b);
        
    b = preferenceStore.getBoolean(Maven2PreferenceConstants.P_OFFLINE);
    sb.append(" -D").append(Maven2PreferenceConstants.P_OFFLINE).append("=").append(b);

    b = preferenceStore.getBoolean(Maven2PreferenceConstants.P_UPDATE_SNAPSHOTS);
    sb.append(" -D").append(Maven2PreferenceConstants.P_UPDATE_SNAPSHOTS).append("=").append(b);

    b = preferenceStore.getBoolean(Maven2PreferenceConstants.P_DEBUG_OUTPUT);
    sb.append(" -D").append(Maven2PreferenceConstants.P_DEBUG_OUTPUT).append("=").append(b);
    
    return sb.toString();
  }

}

