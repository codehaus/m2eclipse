
package org.maven.ide.eclipse.actions;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.maven.ide.eclipse.Maven2Plugin;


public class LifeCycleAction implements IObjectActionDelegate, IMenuCreator {
  protected ISelection selection;

  protected IWorkbenchPart targetPart;

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
   */
  public void run( IAction action ) {
    // Nothing to do
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
   *      org.eclipse.jface.viewers.ISelection)
   */
  public void selectionChanged( IAction action, ISelection selection ) {
    this.selection = selection;
    action.setMenuCreator( this );

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
   *      org.eclipse.ui.IWorkbenchPart)
   */
  public void setActivePart( IAction action, IWorkbenchPart targetPart ) {
    this.targetPart = targetPart;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IMenuCreator#dispose()
   */
  public void dispose() {
    // Nothing to do

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IMenuCreator#getMenu(org.eclipse.swt.widgets.Control)
   */
  public Menu getMenu( Control parent ) {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IMenuCreator#getMenu(org.eclipse.swt.widgets.Menu)
   */
  public Menu getMenu( Menu parent ) {
    Menu menu = new Menu( parent );
    // new ActionContributionItem( new GoalExecuteAction( "validate" ) ).fill( menu, -1 );
    new ActionContributionItem( new GoalExecuteAction( "compile" ) ).fill( menu, -1 );
    new ActionContributionItem( new GoalExecuteAction( "test" ) ).fill( menu, -1 );
    new ActionContributionItem( new GoalExecuteAction( "package" ) ).fill( menu, -1 );
    new ActionContributionItem( new GoalExecuteAction( "integration-test" ) ).fill( menu, -1 );
    // new ActionContributionItem( new GoalExecuteAction( "verify" ) ).fill( menu, -1 );
    new ActionContributionItem( new GoalExecuteAction( "install" ) ).fill( menu, -1 );
    // new ActionContributionItem( new GoalExecuteAction( "deploy" ) ).fill( menu, -1 );
    // new MenuItem( menu, SWT.SEPARATOR );
    // new ActionContributionItem( new GoalExecuteAction( "site" ) ).fill( menu, -1 );
    // new ActionContributionItem( new GoalExecuteAction( "site:deploy" ) ).fill( menu, -1 );
    return menu;
  }

  
  private class GoalExecuteAction extends Action {

    String goalName;

    public GoalExecuteAction( String goal ) {
      this.goalName = goal;
    }

    public String getText() {
      return goalName;
    }

    // TODO this should allow to execute infividual poms (e.g. from modules)
    public void run() {
      if( selection instanceof IStructuredSelection ) {
        IStructuredSelection structuredSelection = ( IStructuredSelection ) selection;
        for( Iterator it = structuredSelection.iterator(); it.hasNext(); ) {
          Object element = it.next();
          IProject project = null;
          if( element instanceof IProject ) {
            project = ( IProject ) element;
          } else if( element instanceof IAdaptable ) {
            project = ( IProject ) ( ( IAdaptable ) element ).getAdapter( IProject.class );
          }
          if( project != null ) {
            IFile pom = project.getFile( "pom.xml" );
            if( !pom.exists() ) {
              Maven2Plugin.getDefault().getConsole().logError( "Project "+project+" does not have pm.xml" );
            } else {
              File pomFile = pom.getLocation().toFile();
              
              try {
                Maven2Plugin.getDefault().executeInEmbedder( "Executing "+goalName+" on "+project, 
                    new GoalExecutionCallBack( pomFile, Collections.singletonList( goalName ), null));
              } catch( CoreException ex ) {
                // ignore
              }
            }
          }
        }
      }
    }
  }

}

