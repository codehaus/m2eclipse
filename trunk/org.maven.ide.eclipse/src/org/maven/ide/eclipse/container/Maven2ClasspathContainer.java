
package org.maven.ide.eclipse.container;

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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

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
  private IClasspathEntry[] entries;

  
  public Maven2ClasspathContainer() {
    this.entries = new IClasspathEntry[0];
  }
  
  public Maven2ClasspathContainer(IClasspathEntry[] entries) {
    IClasspathEntry[] e = new IClasspathEntry[entries.length]; 
    System.arraycopy(entries, 0, e, 0, entries.length);
    Arrays.sort( e, new Comparator() {
      public int compare( Object o1, Object o2) {
        return o1.toString().compareTo( o2.toString());
      }
    } );
    this.entries = e;
  }
  
  public Maven2ClasspathContainer(Set entrySet) {
    this((IClasspathEntry[]) entrySet.toArray(new IClasspathEntry[entrySet.size()]));
  }

  public synchronized IClasspathEntry[] getClasspathEntries() {
    return entries;
  }

  public String getDescription() {
    return "Maven2 Dependencies";  // TODO move to properties
  }

  public int getKind() {
    return IClasspathContainer.K_APPLICATION;
  }

  public IPath getPath() {
    return new Path(Maven2Plugin.CONTAINER_ID);
  }

  // TODO will need this to support for multiple containers per project, but may as well just use one
//  public boolean isContainerFor( IResource resource) {
//    return true;
//  }

  public static boolean isMaven2ClasspathContainer( IPath containerPath) {
    return containerPath!=null && containerPath.segmentCount()>0
        && Maven2Plugin.CONTAINER_ID.equals(containerPath.segment(0));
  }
  
}

