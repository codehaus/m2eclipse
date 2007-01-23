
package org.maven.ide.eclipse.util;

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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.VariablesPlugin;
import org.maven.ide.eclipse.Maven2Plugin;


/**
 * MarkerUtil
 *
 * @author Eugene Kuleshov
 */
public class Util {

  /**
   * Helper method which creates a folder and, recursively, all its parent
   * folders.
   *
   * @param folder  The folder to create.
   *
   * @throws CoreException if creating the given <code>folder</code> or any of
   *                       its parents fails.
   */
  public static void createFolder(IFolder folder) throws CoreException {
    // Recurse until we find a parent folder which already exists.
    if(!folder.exists()) {
      IContainer parent = folder.getParent();
      // First, make sure that all parent folders exist.
      if(parent instanceof IFolder) {
        createFolder((IFolder) parent);
      }
      folder.create(false, true, null);
    }
  }

  /**
   * Substitute any variable
   */
  public static String substituteVar(String s) {
    if(s == null) {
      return s;
    }
    try {
      return VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(s);
    } catch(CoreException e) {
      Maven2Plugin.log(e);
      return null;
    }
  }

  public static void addMarker(IResource resource, String message, int lineNumber, int severity) {
    try {
      IMarker marker = resource.createMarker(Maven2Plugin.MARKER_ID);
      marker.setAttribute(IMarker.MESSAGE, message);
      marker.setAttribute(IMarker.SEVERITY, severity);
      if(lineNumber == -1) {
        lineNumber = 1;
      }
      marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
    } catch(CoreException ex) {
      Maven2Plugin.getDefault().getConsole().logError("Unable to add marker; " + ex.toString());
    }
  }

  public static void deleteMarkers(IResource resource) {
    try {
      resource.deleteMarkers(Maven2Plugin.MARKER_ID, false, IResource.DEPTH_ZERO);
    } catch(CoreException ex) {
      Maven2Plugin.getDefault().getConsole().logError("Unable to delete marker; " + ex.toString());
    }
  }
  
}

