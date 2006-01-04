
package org.maven.ide.eclipse.launch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.embedder.SummaryPluginDescriptor;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.Messages;


public class Maven2GoalSelectionDialog extends ElementTreeSelectionDialog {

  public Maven2GoalSelectionDialog( Shell parent, String title ) {
    super( parent, new GoalsLabelProvider(), new GoalsContentProvider() );
    setValidator( new GoalsSelectionValidator() );
    setTitle( title );
    
    // just test
    Maven2Plugin.getDefault().getConsole().logMessage("Test 123");
  }

  
  private static class GoalsSelectionValidator implements ISelectionStatusValidator {
    public IStatus validate( Object[] selection ) {
      if( selection.length == 0 ) {
        return new Status( IStatus.ERROR, Maven2Plugin.PLUGIN_ID, IStatus.ERROR, "", null );
      }
      for( int j = 0; j < selection.length; j++ ) {
        if( selection[j] instanceof LifecyclePhase ) {
          continue;
        } else if( selection[j] instanceof MojoDescriptor ) {
          continue;
        } else {
          return new Status( IStatus.ERROR, Maven2Plugin.PLUGIN_ID, IStatus.ERROR, "", null );
        }
      }
      return new Status( IStatus.OK, Maven2Plugin.PLUGIN_ID, IStatus.OK, "", null );
    }
  }

  
  private static class GoalsLabelProvider extends LabelProvider {
    public String getText( Object element ) {
      if( element instanceof LifecyclePhases ) {
        return ( ( LifecyclePhases ) element ).getName();
      }
      if( element instanceof LifecyclePhase ) {
        return ( ( LifecyclePhase ) element ).getName();
      } else if( element instanceof SummaryPluginDescriptor ) {
        SummaryPluginDescriptor summaryPluginDescriptor = ( SummaryPluginDescriptor ) element;
        return summaryPluginDescriptor.getName();
      } else if( element instanceof MojoDescriptor ) {
        return ( ( MojoDescriptor ) element ).getGoal();
      }
      return super.getText( element );
    }
  }

  
  private static class GoalsContentProvider implements ITreeContentProvider {
    private MavenEmbedder mavenEmbedder;

    private static Object[] EMPTY = new Object[0];

    public Object[] getChildren( Object parentElement ) {
      if( parentElement instanceof LifecyclePhases ) {
        try {
          List lifecyclePhases = mavenEmbedder.getLifecyclePhases();
          List result = new ArrayList();
          for( Iterator iter = lifecyclePhases.iterator(); iter.hasNext(); ) {
            result.add( new LifecyclePhase( ( String ) iter.next() ) );
          }
          return result.toArray();
        } catch( MavenEmbedderException e ) {
          String msg = "Exception in getLifecyclePhases()";
          Maven2Plugin.log( msg, e );
          return new Object[] { msg };
        }
      } else if( parentElement instanceof SummaryPluginDescriptor ) {
        SummaryPluginDescriptor summaryPluginDescriptor = ( SummaryPluginDescriptor ) parentElement;
        PluginDescriptor pluginDescriptor;
        try {
          pluginDescriptor = this.mavenEmbedder.getPluginDescriptor( summaryPluginDescriptor );
        } catch( MavenEmbedderException e ) {
          String msg = "Exception in getPluginDescriptor() for " + summaryPluginDescriptor.getName();
          Maven2Plugin.log( msg, e );
          return new Object[] { msg };
        }
        List components = pluginDescriptor.getComponents();
        List result = new ArrayList();
        for( Iterator iterator = components.iterator(); iterator.hasNext(); ) {
          MojoDescriptor mojoDescriptor = ( MojoDescriptor ) iterator.next();
          result.add( mojoDescriptor );
        }
        return result.toArray();
      }
      return EMPTY;
    }

    public Object getParent( Object element ) {
      // TODO
      return null;
    }

    public boolean hasChildren( Object element ) {
      if( element instanceof LifecyclePhases ) {
        return true;
      } else if( element instanceof SummaryPluginDescriptor ) {
        SummaryPluginDescriptor summaryPluginDescriptor = ( SummaryPluginDescriptor ) element;
        try {
          PluginDescriptor pluginDescriptor = this.mavenEmbedder.getPluginDescriptor( summaryPluginDescriptor );
          List components = pluginDescriptor.getComponents();
          return ( components.size() > 0 );
        } catch( MavenEmbedderException e ) {
          String msg = "Exception in getPluginDescriptor() for " + summaryPluginDescriptor.getName();
          Maven2Plugin.log( msg, e );
          // IStatus status = new Status(IStatus.ERROR, Maven2Plugin.PLUGIN_ID,
          // 0, msg, e);
          // ErrorDialog.openError((Shell)null, "getPluginDescription", msg,
          // status, IStatus.ERROR);
        }
      }
      return false;
    }

    public Object[] getElements( Object inputElement ) {
      if( inputElement instanceof MavenEmbedder ) {
        MavenEmbedder mavenEmbedder = ( MavenEmbedder ) inputElement;
        List availablePlugins = mavenEmbedder.getAvailablePlugins();
        List result = new ArrayList();
        // placeholder for phases
        result.add( new LifecyclePhases() );

        for( Iterator iter = availablePlugins.iterator(); iter.hasNext(); ) {
          SummaryPluginDescriptor summaryPluginDescriptor = ( SummaryPluginDescriptor ) iter.next();
          result.add( summaryPluginDescriptor );
        }
        return result.toArray();
      }
      return EMPTY;
    }

    public void dispose() {
    }

    public void inputChanged( Viewer viewer, Object oldInput, Object newInput ) {
      this.mavenEmbedder = ( MavenEmbedder ) newInput;
    }
  }

  
  /**
   * Placeholder for Lifecycle phases
   */
  static class LifecyclePhases {
    private static final String NAME = Messages.getString( "launch.goalsDialog.lifecyclePhases" ); //$NON-NLS-1$ 

    public String getName() {
      return NAME;
    }
  }

  
  /**
   * Placeholder for Lifecycle phase
   */
  static class LifecyclePhase {
    private final String name;

    public LifecyclePhase( String name ) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }
  }

}

