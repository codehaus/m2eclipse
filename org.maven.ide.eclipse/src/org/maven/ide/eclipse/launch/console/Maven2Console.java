
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

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
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
    // TODO extract constants
    super("Maven Console", Maven2Plugin.getImageDescriptor("icons/m2.gif")); //$NON-NLS-1$
    this.document = new ConsoleDocument();
  }

  protected void init() {
    Tracer.trace(this, "init()");
    super.init();

    //  Ensure that initialization occurs in the UI thread
    final Display display = Maven2Plugin.getDefault().getWorkbench().getDisplay();
    display.asyncExec(new Runnable() {
      public void run() {
        JFaceResources.getFontRegistry().addListener(Maven2Console.this);
        initializeStreams(display);
        dump();
      }
    });
  }

  /*
   * Initialize three streams of the console. Must be called from the UI thread.
   */
  void initializeStreams(Display display) {
    synchronized(document) {
      if(!initialized) {
        commandStream = newMessageStream();
        errorStream = newMessageStream();
        messageStream = newMessageStream();

        // TODO convert this to use themes
        // install colors
        commandColor = new Color(display, new RGB(0, 0, 0));
        messageColor = new Color(display, new RGB(0, 0, 255));
        errorColor = new Color(display, new RGB(255, 0, 0));

        commandStream.setColor(commandColor);
        messageStream.setColor(messageColor);
        errorStream.setColor(errorColor);

        // install font
        setFont(JFaceResources.getFontRegistry().get("pref_console_font"));
        
        initialized = true;
      }
    }
  }

  void dump() {
    synchronized(document) {
      visible = true;
      ConsoleDocument.ConsoleLine[] lines = document.getLines();
      for(int i = 0; i < lines.length; i++ ) {
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

  public void propertyChange(PropertyChangeEvent event) {
    // font changed
    setFont(JFaceResources.getFontRegistry().get("pref_console_font"));
  }

  private void showConsole(boolean show) {
    IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    if(!visible) {
      manager.addConsoles(new IConsole[] {this});
    }
    if(show) {
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
    synchronized(document) {
      visible = false;
      JFaceResources.getFontRegistry().removeListener(this);
    }
  }

  public void shutdown() {
    // Call super dispose because we want the partitioner to be
    // disconnected.
    super.dispose();
    if(commandColor != null) {
      commandColor.dispose();
    }
    if(messageColor != null) {
      messageColor.dispose();
    }
    if(errorColor != null) {
      errorColor.dispose();
    }
  }

  private DateFormat getDateFormat() {
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG, Locale.getDefault());
  }

  public void logMessage(String message) {
    appendLine(ConsoleDocument.MESSAGE, getDateFormat().format(new Date()) + ": " + message); //$NON-NLS-1$
  }

  public void logError(String message) {
    bringConsoleToFront();
    appendLine(ConsoleDocument.ERROR, getDateFormat().format(new Date()) + ": " + message); //$NON-NLS-1$
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
      for(int i = 0; i < consoles.length; i++ ) {
        IConsole console = consoles[i];
        if(console == Maven2Console.this) {
          init();
        }
      }

    }

    public void consolesRemoved(IConsole[] consoles) {
      Tracer.trace(this, "consolesRemoved()");
      for(int i = 0; i < consoles.length; i++ ) {
        IConsole console = consoles[i];
        if(console == Maven2Console.this) {
          ConsolePlugin.getDefault().getConsoleManager().removeConsoleListener(this);
          dispose();
        }
      }
    }

  }

}
