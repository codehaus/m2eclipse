
package org.maven.ide.eclipse.launch.console;

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

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.IConsoleManager;
import org.maven.ide.eclipse.Maven2Plugin;

/**
 * Console factory is used to show the console from the Console view "Open Console"
 * drop-down action. This factory is registered via the org.eclipse.ui.console.consoleFactory 
 * extension point.
 * 
 *  @author Dmitri Maximovich
 */
public class Maven2ConsoleFactory implements IConsoleFactory {

  public void openConsole() {
    showConsole();
  }

  public static void showConsole() {
    Maven2Console console = Maven2Plugin.getDefault().getConsole();
    if (console != null) {
      IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
      IConsole[] existing = manager.getConsoles();
      boolean exists = false;
      for (int i = 0; i < existing.length; i++) {
        if(console == existing[i])
          exists = true;
      }
      if(!exists) {
        manager.addConsoles(new IConsole[] {console});
      }
      manager.showConsoleView(console);
    }
  }
  
  public static void closeConsole() {
    IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    Maven2Console console = Maven2Plugin.getDefault().getConsole();
    if (console != null) {
      manager.removeConsoles(new IConsole[] {console});
      ConsolePlugin.getDefault().getConsoleManager().addConsoleListener(console.new MyLifecycle());
    }
  }
  
}
