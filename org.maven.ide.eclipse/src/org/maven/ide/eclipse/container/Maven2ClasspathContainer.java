
package org.maven.ide.eclipse.container;

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
  
  public Maven2ClasspathContainer(Set entries) {
    IClasspathEntry[] e = ( IClasspathEntry[]) entries.toArray( new IClasspathEntry[ entries.size()]);
    Arrays.sort( e, new Comparator() {
      public int compare( Object o1, Object o2) {
        return o1.toString().compareTo( o2.toString());
      }
    } );
    this.entries = e;
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

