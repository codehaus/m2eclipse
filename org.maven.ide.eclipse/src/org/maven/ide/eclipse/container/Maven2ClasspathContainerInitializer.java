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

package org.maven.ide.eclipse.container;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.embedder.BuildPathManager;
import org.maven.ide.eclipse.embedder.EmbedderFactory;
import org.maven.ide.eclipse.launch.console.Maven2Console;


/**
 * Maven2ClasspathContainerInitializer
 * 
 * @author Eugene Kuleshov
 */
public class Maven2ClasspathContainerInitializer extends ClasspathContainerInitializer {
  
  public void initialize(IPath containerPath, final IJavaProject project) {
    if(BuildPathManager.isMaven2ClasspathContainer(containerPath)) {
      IClasspathContainer container;
      final Maven2Plugin plugin = Maven2Plugin.getDefault();
      try {
        container = JavaCore.getClasspathContainer(containerPath, project);
      } catch(JavaModelException ex) {
        Maven2Plugin.log("Unable to get container for " + containerPath.toString(), ex);
        return;
      }

      plugin.getMavenModelManager().initModels(new NullProgressMonitor());

      Maven2ClasspathContainer mavenContainer;
      if(container == null) {
        mavenContainer = new Maven2ClasspathContainer();
      } else {
        mavenContainer = new Maven2ClasspathContainer(container.getClasspathEntries());
      }

      try {
        JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {project},
            new IClasspathContainer[] {mavenContainer}, new NullProgressMonitor());
      } catch(JavaModelException ex) {
        Maven2Plugin.log("Unable to set container for " + containerPath.toString(), ex);
        return;
      }

      if(container != null) {
        return;
      }
      
      new Job("Initializing " + project.getProject().getName()) {
        protected IStatus run(IProgressMonitor monitor) {
          try {
            plugin.getBuildpathManager().updateClasspathContainer(project.getProject(), true, monitor);
          } catch(CoreException ex) {
            Maven2Plugin.log("Can't set classpath container", ex);
          }

          return Status.OK_STATUS;
        }
      }.schedule();
    }
  }

  public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
    return BuildPathManager.isMaven2ClasspathContainer(containerPath);
  }
  
  public void requestClasspathContainerUpdate(IPath containerPath, final IJavaProject project,
      final IClasspathContainer containerSuggestion) throws CoreException {
    final IClasspathContainer currentContainer = BuildPathManager.getMaven2ClasspathContainer(project);
    if(currentContainer == null) {
      Maven2Plugin.getDefault().getConsole().logError("Unable to find Maven classpath container");
      return;
    }

    Display display = Maven2Plugin.getDefault().getWorkbench().getDisplay();
    BundleUpdater bundleUpdater = new BundleUpdater(display, currentContainer.getClasspathEntries(),
        containerSuggestion.getClasspathEntries());
    display.syncExec(bundleUpdater);

    if(bundleUpdater.containerUpdated) {
      try {
        JavaCore.setClasspathContainer(containerSuggestion.getPath(), new IJavaProject[] {project},
            new IClasspathContainer[] {new Maven2ClasspathContainer(bundleUpdater.newEntries)}, null);
      } catch(JavaModelException ex) {
        Maven2Plugin.getDefault().getConsole().logError(ex.getMessage());
      }
    }
  }
  
  static final class BundleUpdater implements Runnable {
    private final Display display;
    IClasspathEntry[] newEntries;
    IClasspathEntry[] oldEntries;
    
    boolean containerUpdated = false;

    BundleUpdater(Display display, IClasspathEntry[] oldEntries, IClasspathEntry[] newEntries) {
      this.display = display;
      this.oldEntries = oldEntries;
      this.newEntries = newEntries;
    }

    public void run() {
      for( int j = 0; j < newEntries.length; j++ ) {
        IClasspathEntry entry = newEntries[j];
        List attributes = Arrays.asList(entry.getExtraAttributes());
        
        IPath entryPath = entry.getPath();
        IClasspathEntry oldEntry = getClasspathentry(entryPath);
        if(oldEntry==null) {
          // Maven2Plugin.getDefault().getConsole().logError("Unable to find entry for " + entryPath);
          continue;
        }
        
        File oldSrcPath = getSourceFile(oldEntry);
        File newSrcPath = getSourceFile(entry);
        boolean sourceUpdated = updateBundle(entry, oldSrcPath, newSrcPath, Maven2ClasspathContainer.SOURCES_CLASSIFIER);
          
        String oldJavaDocValue = getAttribute(oldEntry, IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME);
        String newJavaDocValue = getAttribute(entry, IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME);
        File oldJavaDocFile = getJavaDocFile(oldJavaDocValue);
        File newJavaDocFile = getJavaDocFile(newJavaDocValue);
        boolean javadocUpdated = updateBundle(entry, oldJavaDocFile, newJavaDocFile, Maven2ClasspathContainer.JAVADOC_CLASSIFIER);
        
        if(sourceUpdated || javadocUpdated) {
          IPath sourcePath = oldEntry.getSourceAttachmentPath();
          if(sourceUpdated) {
            sourcePath = newSrcPath==null ? null : new Path(getTargetFile(entryPath, Maven2ClasspathContainer.SOURCES_CLASSIFIER).getAbsolutePath()); 
          }
          
          List newAttributes = new ArrayList(attributes);
          
          if(javadocUpdated) {
            for(ListIterator it = newAttributes.listIterator(); it.hasNext();) {
              IClasspathAttribute attribute = (IClasspathAttribute) it.next();
              if(attribute.getName().equals(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME)) {
                it.remove();
              }
            }
            
            if(newJavaDocValue!=null) {
              newAttributes.add(JavaCore.newClasspathAttribute( IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
                  Maven2ClasspathContainer.getJavaDocUrl(getTargetFile(entryPath, Maven2ClasspathContainer.JAVADOC_CLASSIFIER).getAbsolutePath())));
            }
          }
          
          newEntries[j] = JavaCore.newLibraryEntry(
              entryPath, sourcePath, null, new IAccessRule[0],
              (IClasspathAttribute[]) newAttributes.toArray(new IClasspathAttribute[newAttributes.size()]),
              false /*not exported*/);
          
          containerUpdated = true;
        }
      }
    }

    private File getJavaDocFile(String value) {
      if(value==null) return null;
      
      // jar:file:/C:/temp/spring-framework-1.2.7.zip!/
      if(value.startsWith("jar:")) {
        value = value.substring(4);
      }
      if(value.startsWith("file:")) {
        value = value.substring(5);
      }
      int n = value.indexOf("!/");
      if(n!=-1) {
        value = value.substring(0, n);
      }
      
      return value==null ? null : new File(value);
    }

    private boolean updateBundle(IClasspathEntry entry, File oldFile, File newFile, String suffix) {
      boolean entryUpdated = false;
      if(oldFile==null) {
        if(newFile!=null) {
          entryUpdated = installBundle(newFile, entry, suffix, this.display);
        }
      } else {
        if(newFile==null) {
          entryUpdated = removeBundle(oldFile, this.display);
        } else if(!oldFile.equals(newFile)) {
          entryUpdated = installBundle(newFile, entry, suffix, this.display);
        }
      }
      return entryUpdated;
    }

    private boolean installBundle(final File srcFile, IClasspathEntry entry, String classifier, Display display) {
      String groupId = getAttribute(entry, Maven2ClasspathContainer.GROUP_ID_ATTRIBUTE);
      String artifactId = getAttribute(entry, Maven2ClasspathContainer.ARTIFACT_ID_ATTRIBUTE);
      String version = getAttribute(entry, Maven2ClasspathContainer.VERSION_ATTRIBUTE);

      String entryName = entry.getPath().lastSegment();
      
      if(entryName.endsWith(".zip") || entryName.endsWith(".jar")) {            
//        final File target = getTargetFile(entry, suffix);
//        if(target.getAbsolutePath().equals(srcFile.getAbsolutePath())) {
//          return false;
//        }
        if(MessageDialog.openConfirm(display.getActiveShell(), 
              "Install Bundle", "Install "+srcFile+" as "+groupId + ":" + artifactId + ":" + version + ":" + classifier)) {
//          try {
//            FileUtils.copyFile( srcFile, target );
//          } catch( IOException ex ) {
//            Maven2Plugin.getDefault().getConsole().logError( "Unable to copy "+srcFile.getAbsolutePath()+
//                " to "+target.getAbsolutePath()+"; "+ex.getMessage() );
//          }
          
          // TODO maybe move this into MavenEmbedderManager
          MavenEmbedder embedder = Maven2Plugin.getDefault().getMavenEmbedderManager().getProjectEmbedder();
          
          // TODO read offline and debug settings
          MavenExecutionRequest request = EmbedderFactory.createMavenExecutionRequest(embedder, false, true);
          
          // TODO convert to request.addProperty();
          Properties properties = new Properties();
          properties.put("file", srcFile.getAbsolutePath());
          properties.put("groupId", groupId);
          properties.put("artifactId", artifactId);
          properties.put("version", version);
          properties.put("createChecksum", "true");
          properties.put("packaging", "jar");
          properties.put("classifier", classifier); // "sources" or "javadoc"

          request.setProperties(properties);
          request.setGoals(Collections.singletonList("install:install-file"));
          request.setBaseDirectory(new File(".")); // HACK!
            
          MavenExecutionResult result = embedder.execute(request);
          
          if(result.hasExceptions()) {
            Maven2Console console = Maven2Plugin.getDefault().getConsole();
            console.logError("Unable to install " + srcFile.getAbsolutePath());
            for(Iterator it = result.getExceptions().iterator(); it.hasNext();) {
              Throwable ex = (Throwable) it.next();
              console.logError("  " + ex.getMessage());
            }
          }
            
          return true;
        }
      }
      return false;
    }

    private File getTargetFile(IPath entryPath, String suffix) {
      String entryName = entryPath.lastSegment();
      return new File(entryPath.toFile().getParentFile(), entryName.substring(0, entryName.length() - 4) + "-"+ suffix+".jar");
    }

    private boolean removeBundle(final File file, Display display) {
      if(file.exists()) {
        if(!MessageDialog.openConfirm(display.getActiveShell(), 
              "Delete Bundle", "Delete bundle "+file.getAbsolutePath())) {
          return false;
        }
        file.delete();
      }
      return true;
    }

    private IClasspathEntry getClasspathentry(IPath path) {
      for(int i = 0; i < oldEntries.length; i++ ) {
        IClasspathEntry entry = oldEntries[i];
        if(path.equals(entry.getPath())) {
          return entry;
        }
      }
      return null;
    }

    private File getSourceFile(IClasspathEntry entry) {
      IPath path = entry.getSourceAttachmentPath();
      return path==null ? null : path.toFile();
    }

    private String getAttribute(IClasspathEntry entry, String name) {
      IClasspathAttribute[] attributes = entry.getExtraAttributes();
      for(int i = 0; i < attributes.length; i++ ) {
        IClasspathAttribute attribute = attributes[i];
        if(attribute.getName().equals(name)) {
          return attribute.getValue();
        }
      }
      return null;
    }
  }

}

