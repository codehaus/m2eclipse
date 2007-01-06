
package org.maven.ide.eclipse.launch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;


public class Maven2GoalSelectionDialog extends ElementTreeSelectionDialog {

  public Maven2GoalSelectionDialog(Shell parent, String title, MavenEmbedderManager embedderManager) {
    super(parent, new GoalsLabelProvider(), new GoalsContentProvider(embedderManager));

    setTitle(title);
    setValidator(new GoalsSelectionValidator());
  }

  static class GoalsSelectionValidator implements ISelectionStatusValidator {
    public IStatus validate(Object[] selection) {
      if(selection.length == 0) {
        return new Status(IStatus.ERROR, Maven2Plugin.PLUGIN_ID, IStatus.ERROR, "", null);
      }
      for(int j = 0; j < selection.length; j++ ) {
        if(selection[j] instanceof LifecyclePhase) {
          continue;
        } else if(selection[j] instanceof MojoDescriptor) {
          continue;
        } else {
          return new Status(IStatus.ERROR, Maven2Plugin.PLUGIN_ID, IStatus.ERROR, "", null);
        }
      }
      return new Status(IStatus.OK, Maven2Plugin.PLUGIN_ID, IStatus.OK, "", null);
    }
  }

  static class GoalsLabelProvider extends LabelProvider {
    public String getText(Object element) {
      if(element instanceof LifecyclePhases) {
        return ((LifecyclePhases) element).getName();
      }
      if(element instanceof LifecyclePhase) {
        return ((LifecyclePhase) element).getName();
      } else if(element instanceof SummaryPluginDescriptor) {
        SummaryPluginDescriptor summaryPluginDescriptor = (SummaryPluginDescriptor) element;
        return summaryPluginDescriptor.getName();
      } else if(element instanceof MojoDescriptor) {
        return ((MojoDescriptor) element).getGoal();
      }
      return super.getText(element);
    }
  }

  static class GoalsContentProvider implements ITreeContentProvider {
    private static Object[] EMPTY = new Object[0];
    
    private final MavenEmbedderManager embedderManager;

    public GoalsContentProvider(MavenEmbedderManager embedderManager) {
      this.embedderManager = embedderManager;
    }

    public Object[] getChildren(Object parentElement) {
      if(parentElement instanceof LifecyclePhases) {
        try {
          List lifecyclePhases = embedderManager.getProjectEmbedder().getLifecyclePhases();
          List result = new ArrayList();
          for(Iterator it = lifecyclePhases.iterator(); it.hasNext();) {
            result.add(new LifecyclePhase((String) it.next()));
          }
          return result.toArray();
        } catch(Exception e) {
          Maven2Plugin.log("Unable to get lifecycle phases", e);
        }
      } else if(parentElement instanceof SummaryPluginDescriptor) {
        List components = getPluginComponents((SummaryPluginDescriptor) parentElement);
        return components.toArray(); // MojoDescriptor
      }
      return EMPTY;
    }

    public Object getParent(Object element) {
      // TODO
      return null;
    }

    public boolean hasChildren(Object element) {
      if(element instanceof LifecyclePhases) {
        return true;
      } else if(element instanceof SummaryPluginDescriptor) {
        return getPluginComponents((SummaryPluginDescriptor) element).size() > 0;
      }
      return false;
    }

    public Object[] getElements(Object inputElement) {
      try {
        List result = new ArrayList();
        // placeholder for phases
        result.add(new LifecyclePhases());
        List availablePlugins = embedderManager.getProjectEmbedder().getAvailablePlugins();
        for(Iterator it = availablePlugins.iterator(); it.hasNext();) {
          result.add(it.next()); // SummaryPluginDescriptor
        }
        return result.toArray();
      } catch(Exception ex) {
        Maven2Plugin.log("Unable to get plugins", ex);
        return EMPTY;
      }
    }

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      // ???
    }

    private List getPluginComponents(final SummaryPluginDescriptor summaryPluginDescriptor) {
      try {
        PluginDescriptor pluginDescriptor = embedderManager.getProjectEmbedder().getPluginDescriptor(summaryPluginDescriptor);
        return pluginDescriptor.getComponents();
      } catch(Exception ex) {
        Maven2Plugin.log("Unable to get components for " + summaryPluginDescriptor.getName(), ex);
        return Collections.EMPTY_LIST;
      }
    }

  }

  /**
   * Placeholder for Lifecycle phases
   */
  static class LifecyclePhases {
    private static final String NAME = Messages.getString("launch.goalsDialog.lifecyclePhases"); //$NON-NLS-1$ 

    public String getName() {
      return NAME;
    }
  }

  /**
   * Placeholder for Lifecycle phase
   */
  static class LifecyclePhase {
    private final String name;

    public LifecyclePhase(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }
  }

}
