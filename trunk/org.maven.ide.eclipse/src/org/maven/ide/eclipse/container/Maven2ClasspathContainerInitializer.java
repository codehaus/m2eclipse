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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.codehaus.plexus.util.FileUtils;
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


/**
 * Maven2ClasspathContainerInitializer
 * 
 * @author Eugene Kuleshov
 */
public class Maven2ClasspathContainerInitializer extends ClasspathContainerInitializer {

  public void initialize(IPath containerPath, final IJavaProject project) {
    if(Maven2ClasspathContainer.isMaven2ClasspathContainer(containerPath)) {
      IClasspathContainer container;
      final Maven2Plugin plugin = Maven2Plugin.getDefault();
      try {
        container = JavaCore.getClasspathContainer(containerPath, project);
      } catch(JavaModelException ex) {
        Maven2Plugin.log("Unable to get container for " + containerPath.toString(), ex);
        return;
      }

      plugin.getMavenModelManager().initMavenModel(new NullProgressMonitor());

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
            plugin.getClasspathResolver().updateClasspathContainer(project.getProject(), true, monitor);
          } catch(CoreException ex) {
            Maven2Plugin.log("Can't set classpath container", ex);
          }

          return Status.OK_STATUS;
        }
      }.schedule();
    }
  }

  public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
    return Maven2ClasspathContainer.isMaven2ClasspathContainer(containerPath);
  }
  
  public void requestClasspathContainerUpdate(IPath containerPath, final IJavaProject project,
      final IClasspathContainer containerSuggestion) throws CoreException {
    final IClasspathContainer currentContainer = getMaven2ClasspathContainer(project);
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
  
  public static IClasspathContainer getMaven2ClasspathContainer(IJavaProject project) throws JavaModelException {
    return JavaCore.getClasspathContainer(new Path(Maven2Plugin.CONTAINER_ID), project);
  }

  public static String getJavaDocUrl(String fileName) {
    try {
      URL fileUrl = new File(fileName).toURL();
      return "jar:"+fileUrl.toExternalForm()+"!/"+getJavaDocPathInArchive(fileName);
    } catch(MalformedURLException ex) {
      return null;
    }
  }
  
  private static String getJavaDocPathInArchive(String name) {
    long l1 = System.currentTimeMillis();
    ZipFile jarFile = null;
    try {
      jarFile = new ZipFile(name);
      String marker = "package-list";
      for(Enumeration en = jarFile.entries(); en.hasMoreElements();) {
        ZipEntry entry = (ZipEntry) en.nextElement();
        String entryName = entry.getName();
        if(entryName.endsWith(marker)) {
          return entry.getName().substring(0, entryName.length()-marker.length());
        }
      }
    } catch(IOException ex) {
      // ignore
    } finally {
      long l2 = System.currentTimeMillis();
      Maven2Plugin.getDefault().getConsole().logMessage("Scanned javadoc " + name + " " + (l2-l1)/1000f);
      try {
        if(jarFile!=null) jarFile.close();
      } catch(IOException ex) {
        //
      }
    }
    
    return "";
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
        final IClasspathEntry entry = newEntries[j];
        IPath entryPath = entry.getPath();
        IClasspathEntry oldEntry = getClasspathentry(entryPath);
        if(oldEntry==null) {
          // Maven2Plugin.getDefault().getConsole().logError("Unable to find entry for " + entryPath);
          continue;
        }
        
        File oldSrcPath = getSourceFile(oldEntry);
        File newSrcPath = getSourceFile(entry);
        boolean sourceUpdated = updateBundle(entryPath, oldSrcPath, newSrcPath, "sources");
          
        IClasspathAttribute oldJavaDocAttribute = getJavaDocAttribute(oldEntry);
        IClasspathAttribute newJavaDocAttribute = getJavaDocAttribute(entry);
        File oldJavaDocFile = getJavaDocFile(oldJavaDocAttribute);
        File newJavaDocFile = getJavaDocFile(newJavaDocAttribute);
        boolean javadocUpdated = updateBundle(entryPath, oldJavaDocFile, newJavaDocFile, "javadoc");
        
        if(sourceUpdated || javadocUpdated) {
          IPath sourcePath = oldEntry.getSourceAttachmentPath();
          if(sourceUpdated) {
            sourcePath = newSrcPath==null ? null : new Path(getTargetFile(entryPath, "sources").getAbsolutePath()); 
          }
          
          IClasspathAttribute javaDocAttribute = oldJavaDocAttribute;
          if(javadocUpdated) {
            if(newJavaDocAttribute==null || newJavaDocAttribute.getValue()==null) {
              javaDocAttribute = null;
            } else {
              javaDocAttribute = JavaCore.newClasspathAttribute( IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
                  getJavaDocUrl(getTargetFile(entryPath, "javadoc").getAbsolutePath()));
            }
          }
          
          newEntries[j] = JavaCore.newLibraryEntry(
              entryPath, sourcePath, null, new IAccessRule[0],
              javaDocAttribute==null ? new IClasspathAttribute[0] : new IClasspathAttribute[] {javaDocAttribute},
              false /*not exported*/);
          
          containerUpdated |= true;
        }
      }
    }

    private File getJavaDocFile(IClasspathAttribute javaDocAttribute) {
      if(javaDocAttribute==null) return null;
      String value = javaDocAttribute.getValue();
      
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

    private boolean updateBundle(IPath entryPath, File oldFile, File newFile, String suffix) {
      boolean entryUpdated = false;
      if(oldFile==null) {
        if(newFile!=null) {
          entryUpdated = installBundle(newFile, entryPath, suffix, this.display);
        }
      } else {
        if(newFile==null) {
          entryUpdated = removeBundle(oldFile, this.display);
        } else if(!oldFile.equals(newFile)) {
          entryUpdated = installBundle(newFile, entryPath, suffix, this.display);
        }
      }
      return entryUpdated;
    }

    private boolean installBundle(final File srcFile, IPath entryPath, String suffix, Display display) {
      String entryName = entryPath.lastSegment();
      if(entryName.endsWith(".zip") || entryName.endsWith(".jar")) {            
        final File target = getTargetFile(entryPath, suffix);
        if(target.getAbsolutePath().equals(srcFile.getAbsolutePath())) {
          return false;
        }
        if(MessageDialog.openConfirm(display.getActiveShell(), 
              "Install Bundle", "Install "+srcFile+" as "+target.getAbsolutePath())) {
          try {
            FileUtils.copyFile( srcFile, target );
            return true;
          } catch( IOException ex ) {
            Maven2Plugin.getDefault().getConsole().logError( "Unable to copy "+srcFile.getAbsolutePath()+
                " to "+target.getAbsolutePath()+"; "+ex.getMessage() );
          }
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
        if(MessageDialog.openConfirm(display.getActiveShell(), 
              "Delete Bundle", "Delete bundle "+file.getAbsolutePath())) {
          file.delete();
          return true;
        }
      }
      return false;
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

    private IClasspathAttribute getJavaDocAttribute(IClasspathEntry entry) {
      IClasspathAttribute[] attributes = entry.getExtraAttributes();
      for(int i = 0; i < attributes.length; i++ ) {
        IClasspathAttribute attribute = attributes[i];
        if(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME.equals(attribute.getName())) {
          return attribute;
        }
      }
      return null;
    }
  
  }

}

