package org.maven.ide.eclipse.launch.console;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.eclipse.core.runtime.Platform;
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
import org.maven.ide.eclipse.util.ITraceable;
import org.maven.ide.eclipse.util.Tracer;

/**
 * Maven2 plugin Console
 *
 * @author Dmitri Maximovich
 */
public class Maven2Console extends MessageConsole implements IPropertyChangeListener, ITraceable {
  private static final boolean TRACE_ENABLED = Boolean.valueOf(Platform.getDebugOption("org.maven.ide.eclipse/console")).booleanValue();
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

  public boolean isTraceEnabled() {
    return TRACE_ENABLED;
  }
  
  public Maven2Console() {
    // TODO: extract constants
    super("M2", Maven2Plugin.getImageDescriptor("icons/m2.gif")); //$NON-NLS-1$
    this.document = new ConsoleDocument();
//    CVSProviderPlugin.getPlugin().setConsoleListener(CVSOutputConsole.this);
//    CVSUIPlugin.getPlugin().getPreferenceStore().addPropertyChangeListener(CVSOutputConsole.this);
    //showConsole(true);
  }

  protected void init() {
    Tracer.trace(this, "init()");
    super.init();
    //  Ensure that initialization occurs in the ui thread
    Maven2Plugin.getStandardDisplay().asyncExec(new Runnable() {
      public void run() {
        JFaceResources.getFontRegistry().addListener(Maven2Console.this);
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
        commandColor = new Color(Maven2Plugin.getStandardDisplay(), new RGB(0, 0, 0));
        commandStream.setColor(commandColor);
        messageColor = new Color(Maven2Plugin.getStandardDisplay(), new RGB(0, 0, 255));
        messageStream.setColor(messageColor);
        errorColor = new Color(Maven2Plugin.getStandardDisplay(), new RGB(255, 0, 0));
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
    showConsole();
    synchronized(document) {
      if(visible) {
        switch(type) {
          case ConsoleDocument.COMMAND:
            commandStream.println(line);
            break;
          case ConsoleDocument.MESSAGE:
            messageStream.println(line); //$NON-NLS-1$
            break;
          case ConsoleDocument.ERROR:
            errorStream.println(line); //$NON-NLS-1$
            break;
        }
      } else {
        document.appendConsoleLine(type, line);
      }
    }
  }
  
  private void showConsole() {
    show(false);
  }
  
  /**
   * Show the console.
   * @param showNoMatterWhat ignore preferences if <code>true</code>
   */
  public void show(boolean showNoMatterWhat) {
  if(showNoMatterWhat /*|| showOnMessage*/) {
    if(!visible)
      Maven2ConsoleFactory.showConsole();
    else
      ConsolePlugin.getDefault().getConsoleManager().showConsoleView(this);
  }
      
  }
  
  public void propertyChange( PropertyChangeEvent event ) {
    // font changed
    setFont(JFaceResources.getFontRegistry().get("pref_console_font"));
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
        JFaceResources.getFontRegistry().removeListener(this);
      }
    }
  
    public void shutdown() {
      // Call super dispose because we want the partitioner to be
      // disconnected.
      super.dispose();
      if (commandColor != null)
        commandColor.dispose();
      if (messageColor != null)
        messageColor.dispose();
      if (errorColor != null)
        errorColor.dispose();
    }

  private DateFormat getDateFormat() {
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG, Locale.getDefault());
  }
    
  public void logMessage(String message) {
    appendLine(ConsoleDocument.MESSAGE, getDateFormat().format(new Date())+": "+message); //$NON-NLS-1$
  }
  
  public void logError(String message) {
    bringConsoleToFront();
    appendLine(ConsoleDocument.ERROR, getDateFormat().format(new Date())+": "+message); //$NON-NLS-1$
  }
      

  /**
   * Used to notify this console of lifecycle methods <code>init()</code>
   * and <code>dispose()</code>.
   */
  public class MyLifecycle implements org.eclipse.ui.console.IConsoleListener, ITraceable {
    private final boolean TRACE_ENABLED = Boolean.valueOf(Platform.getDebugOption("org.maven.ide.eclipse/console")).booleanValue();

    public boolean isTraceEnabled() {
      return TRACE_ENABLED;
    }

    public void consolesAdded(IConsole[] consoles) {
      Tracer.trace(this, "consolesAdded()");
      for (int i = 0; i < consoles.length; i++) {
        IConsole console = consoles[i];
        if (console == Maven2Console.this) {
          init();
        }
      }

    }
    public void consolesRemoved(IConsole[] consoles) {
      Tracer.trace(this, "consolesRemoved()");
      for (int i = 0; i < consoles.length; i++) {
        IConsole console = consoles[i];
        if (console == Maven2Console.this) {
          ConsolePlugin.getDefault().getConsoleManager().removeConsoleListener(this);
          dispose();
        }
      }
    }

  }
  
  
}
