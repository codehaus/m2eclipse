/*
 * Licensed to the Codehaus Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.maven.ide.eclipse.launch;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
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
import org.eclipse.ui.externaltools.internal.model.ExternalToolBuilder;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.preferences.Maven2PreferenceConstants;
import org.maven.ide.eclipse.util.ITraceable;
import org.maven.ide.eclipse.util.Tracer;
import org.maven.ide.eclipse.util.Util;


public class Maven2LaunchDelegate extends JavaLaunchDelegate implements Maven2LaunchConstants, ITraceable {
  
  private static final boolean TRACE_ENABLED = Boolean.valueOf(Platform.getDebugOption("org.maven.ide.eclipse/launcher")).booleanValue();
  
  private static final String MAVEN_EXECUTOR_CLASS = org.maven.ide.eclipse.embedder.Maven2Executor.class.getName();
  
  public boolean isTraceEnabled() {
    return TRACE_ENABLED;
  }
  
  public String getMainTypeName(ILaunchConfiguration configuration) {
    return MAVEN_EXECUTOR_CLASS;
  }
  
  public void launch( ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor ) throws CoreException {
    Maven2Plugin.getDefault().getConsole().logMessage("Launching Maven " + getProgramArguments(configuration));
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
  
  private static String[] CLASSPATH;
  
  public String[] getClasspath(ILaunchConfiguration configuration) {
    if(CLASSPATH == null) {
      List cp = new ArrayList();

      Enumeration entries = Maven2Plugin.getDefault().getBundle().findEntries("/", "*", true);
      while(entries.hasMoreElements()) {
        URL url = (URL) entries.nextElement();
        String path = url.getPath();
        if(path.endsWith(".jar") || path.endsWith("bin/")) {
          try {
            cp.add(FileLocator.toFileURL(url).getFile());
          } catch(IOException ex) {
            // TODO Auto-generated catch block
            Tracer.trace(this, "Error adding classpath entry", url.toString() + "; " + ex.getMessage());
          }
        }
      }

      Tracer.trace(this, "classpath", cp);

      CLASSPATH = (String[]) cp.toArray(new String[cp.size()]);
    }
    return CLASSPATH;
  }
  
  public String getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
    String pomDirName = configuration.getAttribute(ATTR_POM_DIR, (String) null);
    pomDirName = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(pomDirName);
    Tracer.trace(this, "pomDirName", pomDirName);

    String sep = System.getProperty("file.separator"); //$NON-NLS-1$
    if(!pomDirName.endsWith(sep)) {
      pomDirName += sep;
    }
    String pomFileName = pomDirName + "pom.xml";
    // wrap file path with quotes to handle spaces
    if(pomFileName.indexOf(' ') >= 0) {
      pomFileName = '"' + pomFileName + '"';
    }
    Tracer.trace(this, "pomFileName", pomFileName);

    return pomFileName + " " + getGoals(configuration);
  }

  private String getGoals(ILaunchConfiguration configuration) throws CoreException {
    String buildType = ExternalToolBuilder.getBuildType();
    String goals = null;
    if(IExternalToolConstants.BUILD_TYPE_AUTO.equals(buildType)) {
      goals = configuration.getAttribute(Maven2LaunchConstants.ATTR_GOALS_AUTO_BUILD, "");
    } else if(IExternalToolConstants.BUILD_TYPE_CLEAN.equals(buildType)) {
      goals = configuration.getAttribute(Maven2LaunchConstants.ATTR_GOALS_CLEAN, "");
    } else if(IExternalToolConstants.BUILD_TYPE_FULL.equals(buildType)) {
      goals = configuration.getAttribute(Maven2LaunchConstants.ATTR_GOALS_AFTER_CLEAN, "");
    } else if(IExternalToolConstants.BUILD_TYPE_INCREMENTAL.equals(buildType)) {
      goals = configuration.getAttribute(Maven2LaunchConstants.ATTR_GOALS_MANUAL_BUILD, "");
    } else {
      goals = configuration.getAttribute(Maven2LaunchConstants.ATTR_GOALS, "");
    }
    return goals;
  }
  
  public String getVMArguments(ILaunchConfiguration configuration) throws CoreException {
    return super.getVMArguments(configuration) + " " + getProperties(configuration) + " " + getPreferences();
  }

  /**
   * Construct string with properties to pass to JVM as system properties 
   * @throws CoreException 
   */
  private String getProperties(ILaunchConfiguration configuration) {
    StringBuffer sb = new StringBuffer();

    try {
      List properties = configuration.getAttribute(ATTR_PROPERTIES, Collections.EMPTY_LIST);
      for(Iterator it = properties.iterator(); it.hasNext();) {
        String[] s = ((String) it.next()).split("=");
        String n = s[0];
        // substitute var if any
        String v = Util.substituteVar(s[1]);
        // enclose in quotes if spaces
        if(v.indexOf(' ') >= 0) {
          v = '"' + v + '"';
        }
        sb.append(" -D").append(n).append("=").append(v);
      }
    } catch(CoreException e) {
      String msg = "Exception while getting configuration attribute " + ATTR_PROPERTIES;
      Maven2Plugin.log(msg, e);
    }

    try {
      String profiles = configuration.getAttribute(ATTR_PROFILES, (String) null);
      if(profiles != null) {
        sb.append(" -D").append(ATTR_PROFILES).append("=").append(profiles);
      }
    } catch(CoreException ex) {
      String msg = "Exception while getting configuration attribute " + ATTR_PROFILES;
      Maven2Plugin.log(msg, ex);
    }

    return sb.toString();
  }

  /**
   * Construct string with preferences to pass to JVM as system properties 
   */
  private String getPreferences() {
    StringBuffer sb = new StringBuffer();

    IPreferenceStore preferenceStore = Maven2Plugin.getDefault().getPreferenceStore();

//    String s = preferenceStore.getString(Maven2PreferenceConstants.P_LOCAL_REPOSITORY_DIR);
//    if(s != null && s.trim().length() > 0) {
//      sb.append(" -D").append(Maven2PreferenceConstants.P_LOCAL_REPOSITORY_DIR).append("=\"").append(s).append('\"');
//    }

//    String s = preferenceStore.getString(Maven2PreferenceConstants.P_GLOBAL_CHECKSUM_POLICY);
//    if(s != null && s.trim().length() > 0) {
//      sb.append(" -D").append(Maven2PreferenceConstants.P_GLOBAL_CHECKSUM_POLICY).append("=").append(s);
//    }


    boolean debugOutput = preferenceStore.getBoolean(Maven2PreferenceConstants.P_DEBUG_OUTPUT);
    sb.append(" -D").append(Maven2PreferenceConstants.P_DEBUG_OUTPUT).append("=").append(debugOutput);

    boolean offline = preferenceStore.getBoolean(Maven2PreferenceConstants.P_OFFLINE);
    sb.append(" -D").append(Maven2PreferenceConstants.P_OFFLINE).append("=").append(offline);

    String globalSettings = preferenceStore.getString(Maven2PreferenceConstants.P_GLOBAL_SETTINGS_FILE);
    if(globalSettings != null && globalSettings.trim().length() > 0) {
      sb.append(" -D").append(Maven2PreferenceConstants.P_GLOBAL_SETTINGS_FILE).append("=");
      if(globalSettings.indexOf(' ')>-1) {
        sb.append('\"').append(globalSettings).append('\"');
      } else {
        sb.append(globalSettings);
      }
    }
    
    // boolean b = preferenceStore.getBoolean(Maven2PreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION);
    // sb.append(" -D").append(Maven2PreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION).append("=").append(b);

    // b = preferenceStore.getBoolean(Maven2PreferenceConstants.P_UPDATE_SNAPSHOTS);
    // sb.append(" -D").append(Maven2PreferenceConstants.P_UPDATE_SNAPSHOTS).append("=").append(b);

    return sb.toString();
  }

}

