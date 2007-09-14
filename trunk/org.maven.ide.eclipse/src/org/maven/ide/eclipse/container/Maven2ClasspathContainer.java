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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.maven.ide.eclipse.Maven2Plugin;


/**
 * Maven2ClasspathContainer
 * 
 * @author Eugene Kuleshov
 */
public class Maven2ClasspathContainer implements IClasspathContainer {
  private final IClasspathEntry[] entries;
  private final IPath path;

  
  public Maven2ClasspathContainer() {
    this.path = new Path(Maven2Plugin.CONTAINER_ID);
    this.entries = new IClasspathEntry[0];
  }
  
  public Maven2ClasspathContainer(IPath path, IClasspathEntry[] entries) {
    this.path = path;
    IClasspathEntry[] e = new IClasspathEntry[entries.length]; 
    System.arraycopy(entries, 0, e, 0, entries.length);
    Arrays.sort( e, new Comparator() {
      public int compare( Object o1, Object o2) {
        return o1.toString().compareTo( o2.toString());
      }
    } );
    this.entries = e;
  }
  
  public Maven2ClasspathContainer(IPath path, Set entrySet) {
    this(path, (IClasspathEntry[]) entrySet.toArray(new IClasspathEntry[entrySet.size()]));
  }

  public synchronized IClasspathEntry[] getClasspathEntries() {
    return entries;
  }

  public String getDescription() {
    return "Maven Dependencies";  // TODO move to properties
  }

  public int getKind() {
    return IClasspathContainer.K_APPLICATION;
  }

  public IPath getPath() {
    return path; 
  }

  public static String getJavaDocUrl(String fileName) {
    try {
      URL fileUrl = new File(fileName).toURL();
      return "jar:"+fileUrl.toExternalForm()+"!/"+Maven2ClasspathContainer.getJavaDocPathInArchive(fileName);
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
  
}

