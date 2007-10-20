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

package org.maven.ide.eclipse.actions;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;
import org.maven.ide.eclipse.Maven2Plugin;


/**
 * Maven menu action
 * 
 * @author Eugene Kuleshov
 */
public class MavenMenuAction implements IObjectActionDelegate, IMenuCreator, IWorkbenchWindowPulldownDelegate2 {

  boolean fillMenu;

  IStructuredSelection selection;

  private IAction delegateAction;

  // IWorkbenchWindowPulldownDelegate

  public void init(IWorkbenchWindow window) {
  }

  // IMenuCreator

  public void dispose() {
  }

  public Menu getMenu(Control parent) {
    Menu menu = new Menu(parent);
    return menu;
  }

  public Menu getMenu(Menu parent) {
    Menu menu = new Menu(parent);
    /**
     * Add listener to re-populate the menu each time it is shown because MenuManager.update(boolean, boolean) doesn't
     * dispose pull-down ActionContribution items for each popup menu.
     */
    menu.addMenuListener(new MenuAdapter() {
      public void menuShown(MenuEvent e) {
        if(fillMenu) {
          Menu m = (Menu) e.widget;
          MenuItem[] items = m.getItems();
          for(int i = 0; i < items.length; i++ ) {
            items[i].dispose();
          }
          fillMenu(m);
          fillMenu = false;
        }
      }

    });
    return menu;
  }

  void fillMenu(Menu menu) {
    if(selection.size() == 1) {
      Object element = selection.getFirstElement();
      IFile file = (IFile) getType(element, IFile.class);
      if(file != null) {
        ActionContributionItem item = new ActionContributionItem( //
            new ActionProxy("org.maven.ide.eclipse.addFileDependencyAction", "Add Dependency",
                new AddDependencyAction()));
        item.fill(menu, -1);
      }
    }
    if(!selection.isEmpty()) {
      boolean allProjects = true;
      boolean withNature = true;
      boolean noNature = true;
      for(Iterator it = selection.iterator(); it.hasNext();) {
        Object element = it.next();
        IProject project = (IProject) getType(element, IProject.class);
        if(project != null) {
          try {
            if(project.hasNature(Maven2Plugin.NATURE_ID)) {
              noNature = false;
            } else {
              withNature = false;
            }
          } catch(CoreException ex) {
            // ignore
          }
        } else {
          allProjects = false;
        }
      }
      if(allProjects) {
        if(noNature) {
          addMenu("org.maven.ide.eclipse.enableAction", "Enable Dependency Management", new EnableNatureAction(), menu);
        }
        if(withNature) {
          if(selection.size() == 1) {
            addMenu("org.maven.ide.eclipse.addProjectDependencyAction", "Add Dependency", new AddDependencyAction(),
                menu);
          }

          addMenu("org.maven.ide.eclipse.updateSourcesAction", "Update Source Folders", new UpdateSourcesAction(), menu);
          new Separator().fill(menu, -1);
          addMenu("org.maven.ide.eclipse.workspaceResolution", "Enable Workspace Resolution", new EnableNatureAction(
              "workspace"), menu);
          addMenu("org.maven.ide.eclipse.enableModules", "Enable Nested Modules", new EnableNatureAction("modules"),
              menu);
          addMenu("org.maven.ide.eclipse.disableAction", "Disable Dependency Management", new DisableNatureAction(),
              menu);
        }
      }
    }
  }

  private Object getType(Object element, Class type) {
    if(type.isInstance(element)) {
      return element;
    } else if(element instanceof IAdaptable) {
      return ((IAdaptable) element).getAdapter(type);
    }
    return Platform.getAdapterManager().getAdapter(element, type); 
  }

  private void addMenu(String id, String text, IActionDelegate actionDelegate, Menu menu) {
    ActionContributionItem item = new ActionContributionItem(new ActionProxy(id, text, actionDelegate));
    item.fill(menu, -1);
  }

  // IObjectActionDelegate

  public void run(IAction action) {
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
      this.fillMenu = true;

      if(delegateAction != action) {
        delegateAction = action;
        delegateAction.setMenuCreator(this);
      }

      action.setEnabled(!selection.isEmpty());
    }
  }

  private final class ActionProxy extends Action {
    private IActionDelegate action;

    public ActionProxy(String id, String text, IActionDelegate action) {
      super(text);
      this.action = action;
      setId(id);
    }

    public void run() {
      action.selectionChanged(this, selection);
      action.run(this);
    }
  }

}
