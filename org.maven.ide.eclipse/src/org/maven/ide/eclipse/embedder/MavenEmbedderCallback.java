
package org.maven.ide.eclipse.embedder;

import org.apache.maven.embedder.MavenEmbedder;

import org.eclipse.core.runtime.IProgressMonitor;


public interface MavenEmbedderCallback {

  Object run(MavenEmbedder mavenEmbedder, IProgressMonitor monitor) throws Exception;
  
}
