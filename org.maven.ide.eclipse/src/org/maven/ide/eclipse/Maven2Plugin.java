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

package org.maven.ide.eclipse;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.maven.ide.eclipse.embedder.BuildPathManager;
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.index.MavenRepositoryIndexManager;
import org.maven.ide.eclipse.launch.console.Maven2Console;
import org.osgi.framework.BundleContext;


/**
 * Maven2Plugin main plug-in class.
 */
public class Maven2Plugin extends AbstractUIPlugin {

  public static final String PLUGIN_ID = Maven2Plugin.class.getPackage().getName();
  public static final String NATURE_ID = PLUGIN_ID + ".maven2Nature"; //$NON-NLS-1$
  public static final String BUILDER_ID = PLUGIN_ID + ".maven2Builder"; //$NON-NLS-1$
  public static final String MARKER_ID = PLUGIN_ID + ".maven2Problem"; //$NON-NLS-1$

  public static final String POM_FILE_NAME = "pom.xml"; //$NON-NLS-1$

  // container settings
  public static final String CONTAINER_ID = PLUGIN_ID + ".MAVEN2_CLASSPATH_CONTAINER"; //$NON-NLS-1$
  public static final String INCLUDE_MODULES = "modules"; //$NON-NLS-1$
  public static final String NO_WORKSPACE_PROJECTS = "noworkspace"; //$NON-NLS-1$
  public static final String ACTIVE_PROFILES = "profiles"; //$NON-NLS-1$
  // entry attributes
  public static final String GROUP_ID_ATTRIBUTE = "maven.groupId"; //$NON-NLS-1$
  public static final String ARTIFACT_ID_ATTRIBUTE = "maven.artifactId"; //$NON-NLS-1$
  public static final String VERSION_ATTRIBUTE = "maven.version"; //$NON-NLS-1$
  public static final String JAVADOC_CLASSIFIER = "javadoc"; //$NON-NLS-1$
  public static final String SOURCES_CLASSIFIER = "sources"; //$NON-NLS-1$
  
  // The shared instance
  private static Maven2Plugin plugin;

  private Maven2Console console;
  private MavenModelManager mavenModelManager;
  private MavenRepositoryIndexManager mavenRepositoryIndexManager;
  private MavenEmbedderManager mavenEmbedderManager;

  private BuildPathManager buildpathManager;
  private IResourceChangeListener resourceChangeListener;
  
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

    this.mavenEmbedderManager = new MavenEmbedderManager(console, getPreferenceStore());

    this.mavenRepositoryIndexManager = new MavenRepositoryIndexManager(mavenEmbedderManager, console, getBundle(),
        getStateLocation());

    this.mavenModelManager = new MavenModelManager(mavenEmbedderManager, mavenRepositoryIndexManager, console);
    // this.mavenModelManager.initMavenModel(new NullProgressMonitor());
    
    this.buildpathManager = new BuildPathManager(mavenEmbedderManager, console, mavenModelManager,
        mavenRepositoryIndexManager, getPreferenceStore());
    
    this.resourceChangeListener = new Maven2ResourceChangeListener(mavenModelManager, buildpathManager, console);
    
    ResourcesPlugin.getWorkspace().addResourceChangeListener(
        this.resourceChangeListener,
        IResourceChangeEvent.PRE_BUILD | IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_CLOSE
            | IResourceChangeEvent.PRE_DELETE);
  }

  /**
   * This method is called when the plug-in is stopped
   */
  public void stop( BundleContext context) throws Exception {
    super.stop( context);
    
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
    
    this.mavenEmbedderManager.shutdown();
    
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

  public MavenModelManager getMavenModelManager() {
    return this.mavenModelManager;
  }

  public MavenRepositoryIndexManager getMavenRepositoryIndexManager() {
    return this.mavenRepositoryIndexManager;
  }

  public Maven2Console getConsole() {
    return this.console;
  }
  
  public MavenEmbedderManager getMavenEmbedderManager() {
    return this.mavenEmbedderManager;
  }
  
  public BuildPathManager getBuildpathManager() {
    return this.buildpathManager;
  }


  /**
   * Returns an Image for the file at the given relative path.
   */
  public static Image getImage(String path) {
    ImageRegistry registry = getDefault().getImageRegistry();
    Image image = registry.get(path);
    if(image==null) {
      registry.put(path, imageDescriptorFromPlugin(PLUGIN_ID, path));
      image = registry.get(path);
    }
    return image;
  }
  
  public static ImageDescriptor getImageDescriptor(String path) {
    return imageDescriptorFromPlugin(PLUGIN_ID, path);
  }

  public static void log(IStatus status) {
    getDefault().getLog().log(status);
  }

  public static void log(CoreException ex) {
    log(ex.getStatus());
  }

  public static void log(String msg, Throwable t) {
    log(new Status(IStatus.ERROR, PLUGIN_ID, 0, msg, t));
  }

}

