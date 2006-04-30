/* $Id: org.eclipse.jdt.ui.prefs,v 1.1 2005/09/30 23:08:35 eu Exp $ */

package org.maven.ide.eclipse.launch.console;

import org.eclipse.jface.action.Action;
import org.maven.ide.eclipse.Maven2Plugin;

public class Maven2ConsoleRemoveAction extends Action {

  public Maven2ConsoleRemoveAction() {
    setToolTipText("Close Maven2 Console");
    setImageDescriptor(Maven2Plugin.getImageDescriptor("icons/close.gif"));
  }
  
  public void run() {
    Maven2ConsoleFactory.closeConsole();
  }

}
