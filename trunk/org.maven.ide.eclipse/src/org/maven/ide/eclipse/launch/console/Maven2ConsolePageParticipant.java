
package org.maven.ide.eclipse.launch.console;

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
