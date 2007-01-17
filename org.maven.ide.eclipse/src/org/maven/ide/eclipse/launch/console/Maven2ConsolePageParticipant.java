
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

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;

public class Maven2ConsolePageParticipant implements IConsolePageParticipant {

  private Maven2Console console;
  private IPageBookViewPage page;
  private Maven2ConsoleRemoveAction consoleRemoveAction;
  
  public void init(IPageBookViewPage page, IConsole console) {
    this.console = (Maven2Console)console;
    this.page = page;
    this.consoleRemoveAction = new Maven2ConsoleRemoveAction();
    IActionBars bars = page.getSite().getActionBars();
    bars.getToolBarManager().appendToGroup(IConsoleConstants.LAUNCH_GROUP, consoleRemoveAction);
  }

  public void dispose() {
    this.consoleRemoveAction = null;
    this.page = null;
    this.console = null;
  }

  public void activated() {
  }

  public void deactivated() {
  }

  public Object getAdapter(Class adapter) {
    return null;
  }

}
