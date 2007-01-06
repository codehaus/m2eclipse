
package org.maven.ide.eclipse.embedder;

import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.embedder.ContainerCustomizer;
import org.apache.maven.embedder.DefaultMavenEmbedRequest;
import org.apache.maven.embedder.MavenEmbedRequest;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.embedder.MavenEmbedderLogger;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.ComponentDescriptor;


public class EmbedderFactory {

  public static MavenEmbedder createMavenEmbedder(ContainerCustomizer customizer, MavenEmbedderLogger logger) throws MavenEmbedderException {
    MavenEmbedRequest request = new DefaultMavenEmbedRequest();
    
    request.setConfigurationCustomizer(customizer);
      
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    MavenEmbedder embedder = new MavenEmbedder(loader, logger);
    
    embedder.start(request);
    
    return embedder;
  }

  
  public static ContainerCustomizer createProjectCustomizer() {
    return new ContainerCustomizer() {
        public void customize(PlexusContainer container) {
          // desc = plexusContainer.getComponentDescriptor(ArtifactFactory.ROLE);
          // desc.setImplementation(org.maven.ide.eclipse.embedder.EclipseArtifactFactory.class.getName());

          // Used for building hierarchy of dependencies
          // desc = container.getComponentDescriptor(ResolutionListener.ROLE);
          // if(desc == null) {
          //   desc = new ComponentDescriptor();
          //   desc.setRole(ResolutionListener.ROLE);
          //   container.addComponentDescriptor(desc);
          // }
          // desc.setImplementation(EclipseResolutionListener.class.getName());

          // Custom artifact resolver for resolving artifacts from Eclipse Worspace
          ComponentDescriptor resolverDescriptor = container.getComponentDescriptor(ArtifactResolver.ROLE);
          // ComponentRequirement requirement = new ComponentRequirement();
          // requirement.setRole(ResolutionListener.ROLE);
          // desc.addRequirement(requirement);
          resolverDescriptor.setImplementation(EclipseArtifactResolver.class.getName());
        
//          desc = container.getComponentDescriptor(WagonManager.ROLE);
//          desc.setImplementation(EclipseWagonManager.class.getName());
        }
      };
  }

  
  public static ContainerCustomizer createExecutionCustomizer() {
    return new ContainerCustomizer() {
        public void customize(PlexusContainer plexusContainer) {
//          ComponentDescriptor desc = plexusContainer.getComponentDescriptor(LifecycleExecutor.ROLE);
//          desc.setImplementation(org.maven.ide.eclipse.embedder.EclipseLifecycleExecutor.class.getName());
//          try {
//            PlexusConfiguration oldConf = desc.getConfiguration();
//            XmlPlexusConfiguration conf = new XmlPlexusConfiguration(oldConf.getName());
//            copyConfig(oldConf, conf);
//            desc.setConfiguration(conf);
//          } catch(PlexusConfigurationException ex) {
//            // XXX log error
//          }
        }
  
//        private void copyConfig(PlexusConfiguration old, XmlPlexusConfiguration conf)
//            throws PlexusConfigurationException {
//          conf.setValue(old.getValue());
//          String[] attrNames = old.getAttributeNames();
//          if(attrNames != null && attrNames.length > 0) {
//            for(int i = 0; i < attrNames.length; i++ ) {
//              conf.setAttribute(attrNames[i], old.getAttribute(attrNames[i]));
//            }
//          }
//          if("lifecycle".equals(conf.getName())) {
//            conf.setAttribute("implementation", "org.apache.maven.lifecycle.Lifecycle");
//          }
//          for(int i = 0; i < old.getChildCount(); i++ ) {
//            PlexusConfiguration oldChild = old.getChild(i);
//            XmlPlexusConfiguration newChild = new XmlPlexusConfiguration(oldChild.getName());
//            conf.addChild(newChild);
//            copyConfig(oldChild, newChild);
//          }
//        }
      };
  }


  public static MavenExecutionRequest createMavenExecutionRequest(MavenEmbedder embedder, boolean offline, boolean debug) {
    DefaultMavenExecutionRequest request = new DefaultMavenExecutionRequest();

    request.setOffline(offline);
    request.setUseReactor(false);
    request.setRecursive(true);
    
    if(debug) {
      request.setShowErrors(true);
      request.setLoggingLevel(MavenExecutionRequest.LOGGING_LEVEL_DEBUG);
    } else {
      request.setShowErrors(false);
      request.setLoggingLevel(MavenExecutionRequest.LOGGING_LEVEL_INFO);
    }
    
    return request;
  }
  
}

