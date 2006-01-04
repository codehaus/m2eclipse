package org.maven.ide.eclipse.launch.console;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.maven.ide.eclipse.Maven2Plugin;

/**
 * Maven2 plugin Console
 *
 * @author Dmitri Maximovich
 */
public class Maven2Console extends MessageConsole implements IPropertyChangeListener {
  private boolean initialized = false;
  // console is visible in the Console view
  private boolean visible = false;
  private ConsoleDocument document;

  // created colors for each line type - must be disposed at shutdown
  private Color commandColor;
  private Color messageColor;
  private Color errorColor;

  // streams for each command type - each stream has its own color
  private MessageConsoleStream commandStream;
  private MessageConsoleStream messageStream;
  private MessageConsoleStream errorStream;
  
  public Maven2Console() {
    // TODO: extract constants
    super("M2", Maven2Plugin.getImageDescriptor("icons/m2.gif")); //$NON-NLS-1$
    this.document = new ConsoleDocument();
    showConsole(true);
  }

  protected void init() {
    super.init();
    //  Ensure that initialization occurs in the ui thread
    Maven2Plugin.getStandardDisplay().asyncExec(new Runnable() {
      public void run() {
        //JFaceResources.getFontRegistry().addListener(MavenConsole.this);
        initializeStreams();
        dump();
      }
    });
  }

  /*
   * Initialize three streams of the console. Must be called from the UI thread.
   */
  void initializeStreams() {
    synchronized(document) {
      if (!initialized) {
        commandStream = newMessageStream();
        errorStream = newMessageStream();
        messageStream = newMessageStream();
        // install colors
        commandColor = new Color(Maven2Plugin.getStandardDisplay(), new RGB(100, 100, 100));
        commandStream.setColor(commandColor);
        messageColor = new Color(Maven2Plugin.getStandardDisplay(), new RGB(150, 150, 150));
        messageStream.setColor(messageColor);
        errorColor = new Color(Maven2Plugin.getStandardDisplay(), new RGB(200, 200, 200));
        errorStream.setColor(errorColor);
        // install font
        // TODO: extract constants
        setFont(JFaceResources.getFontRegistry().get("pref_console_font"));
        initialized = true;
      }
    }
  }
  
  void dump() {
    synchronized(document) {
      visible = true;
      ConsoleDocument.ConsoleLine[] lines = document.getLines();
      for (int i = 0; i < lines.length; i++) {
        ConsoleDocument.ConsoleLine line = lines[i];
        appendLine(line.type, line.line);
      }
      document.clear();
    }
  }

  private void appendLine(int type, String line) {
    synchronized(document) {
      if(visible) {
        switch(type) {
          case ConsoleDocument.COMMAND:
            commandStream.println(line);
            break;
          case ConsoleDocument.MESSAGE:
            messageStream.println("  " + line); //$NON-NLS-1$
            break;
          case ConsoleDocument.ERROR:
            errorStream.println("  " + line); //$NON-NLS-1$
            break;
        }
      } else {
        document.appendConsoleLine(type, line);
      }
    }
  }
  
  public void propertyChange( PropertyChangeEvent event ) {
  }

  private void showConsole(boolean show) {
      IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
      if(!visible) {
        manager.addConsoles(new IConsole[] {this});
      }
      if (show) {
        manager.showConsoleView(this);
      }
  }

    private void bringConsoleToFront() {
        IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
        if(!visible) {
            manager.addConsoles(new IConsole[] {this});
        }
        manager.showConsoleView(this);
    }
  
    // Called when console is removed from the console view
    protected void dispose() {
      // Here we can't call super.dispose() because we actually want the partitioner to remain
      // connected, but we won't show lines until the console is added to the console manager
      // again.
      synchronized (document) {
        visible = false;
        //JFaceResources.getFontRegistry().removeListener(this);
      }
    }
  
//    public void shutdown() {
//      // Call super dispose because we want the partitioner to be
//      // disconnected.
//      super.dispose();
//      if (commandColor != null)
//        commandColor.dispose();
//      if (messageColor != null)
//        messageColor.dispose();
//      if (errorColor != null)
//        errorColor.dispose();
//    }

    public void logMessage(String message) {
      appendLine(ConsoleDocument.MESSAGE, "  " + message); //$NON-NLS-1$
    }
  
  public void logError(String message) {
      //if (showOnError) {
        bringConsoleToFront();
      //}
      appendLine(ConsoleDocument.ERROR, "  " + message); //$NON-NLS-1$
  }
    
}
