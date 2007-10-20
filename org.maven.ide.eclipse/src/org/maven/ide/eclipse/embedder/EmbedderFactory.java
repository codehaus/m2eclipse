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

package org.maven.ide.eclipse.embedder;

import java.io.File;

import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.ConfigurationValidationResult;
import org.apache.maven.embedder.ContainerCustomizer;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.embedder.MavenEmbedderLogger;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.ComponentDescriptor;


public class EmbedderFactory {

  public static MavenEmbedder createMavenEmbedder(ContainerCustomizer customizer, MavenEmbedderLogger logger, String globalSettings) throws MavenEmbedderException {
    Configuration configuration = new DefaultConfiguration();
    
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    configuration.setMavenEmbedderLogger(logger);
    configuration.setClassLoader(loader);
    configuration.setConfigurationCustomizer(customizer);
    
    File userSettingsFile = MavenEmbedder.DEFAULT_USER_SETTINGS_FILE;
    ConfigurationValidationResult userResult = validateConfiguration(userSettingsFile, logger);
    if(userResult.isValid()) {
      configuration.setUserSettingsFile(userSettingsFile);
    } else {
      logger.error("Invalid user settings " + userSettingsFile);
      if(userResult.getUserSettingsException()!=null) {
        logger.error(userResult.getUserSettingsException().getMessage());
      }
    }
    
    if(globalSettings!=null && globalSettings.length()>0) {
      File globalSettingsFile = new File(globalSettings);
      ConfigurationValidationResult globalResult = validateConfiguration(globalSettingsFile, logger);
      if(globalResult.isValid()) {
        configuration.setGlobalSettingsFile(globalSettingsFile);
      } else {
        logger.error("Invalid settings " + globalSettings);
        if(globalResult.getUserSettingsException()!=null) {
          logger.error(userResult.getUserSettingsException().getMessage());
        }
      }
    }
      
    return new MavenEmbedder(configuration);
  }

  public static ConfigurationValidationResult validateConfiguration(File file, MavenEmbedderLogger logger) {
    Configuration configuration = new DefaultConfiguration();
    configuration.setMavenEmbedderLogger(logger);
    configuration.setUserSettingsFile(file);
    return MavenEmbedder.validateConfiguration(configuration);
  }

  public static ContainerCustomizer createWorkspaceCustomizer(final boolean resolveWorkspaceProjects) {
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
          if(resolveWorkspaceProjects) {
            ComponentDescriptor resolverDescriptor = container.getComponentDescriptor(ArtifactResolver.ROLE);
            // ComponentRequirement requirement = new ComponentRequirement();
            // requirement.setRole(ResolutionListener.ROLE);
            // desc.addRequirement(requirement);
            resolverDescriptor.setImplementation(EclipseArtifactResolver.class.getName());
          }
        
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

