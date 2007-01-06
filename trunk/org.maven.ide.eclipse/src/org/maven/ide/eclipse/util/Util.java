
package org.maven.ide.eclipse.util;

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
   * Substitute any variable
   */
  public static String substituteVar(String s) {
    if(s == null) {
      return s;
    }
    try {
      return VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(s);
    } catch(CoreException e) {
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

