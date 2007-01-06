
package org.maven.ide.eclipse;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.maven.ide.eclipse.embedder.ClassPathResolver;
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
  public static final String CONTAINER_ID = PLUGIN_ID + ".MAVEN2_CLASSPATH_CONTAINER"; //$NON-NLS-1$
  public static final String NATURE_ID = PLUGIN_ID + ".maven2Nature"; //$NON-NLS-1$
  public static final String BUILDER_ID = PLUGIN_ID + ".maven2Builder"; //$NON-NLS-1$
  public static final String MARKER_ID = PLUGIN_ID + ".maven2Problem"; //$NON-NLS-1$

  public static final String POM_FILE_NAME = "pom.xml"; //$NON-NLS-1$

  // The shared instance
  private static Maven2Plugin plugin;

  private Maven2Console console;
  private MavenModelManager mavenModelManager;
  private MavenRepositoryIndexManager mavenRepositoryIndexManager;
  private MavenEmbedderManager mavenEmbedderManager;

  private ClassPathResolver classpathResolver;
  
  
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

    this.mavenModelManager = new MavenModelManager(mavenEmbedderManager, console);
    this.mavenRepositoryIndexManager = new MavenRepositoryIndexManager(mavenEmbedderManager, console, getBundle(),
        getStateLocation());

    this.classpathResolver = new ClassPathResolver(mavenEmbedderManager, console, mavenModelManager,
        mavenRepositoryIndexManager, getPreferenceStore());
  }

  /**
   * This method is called when the plug-in is stopped
   */
  public void stop( BundleContext context) throws Exception {
    super.stop( context);
    
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
  
  public ClassPathResolver getClasspathResolver() {
    return this.classpathResolver;
  }

  // XXX replace with preference store listener
  public void invalidateMavenSettings() {
    mavenEmbedderManager.invalidateMavenSettings();
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

