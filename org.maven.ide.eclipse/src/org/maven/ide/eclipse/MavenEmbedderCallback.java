/* $Id: org.eclipse.jdt.ui.prefs,v 1.1 2005/09/30 23:08:35 eu Exp $ */

package org.maven.ide.eclipse;

import org.apache.maven.embedder.MavenEmbedder;

public interface MavenEmbedderCallback {
  Object doInEmbedder(MavenEmbedder mavenEmbedder);
}