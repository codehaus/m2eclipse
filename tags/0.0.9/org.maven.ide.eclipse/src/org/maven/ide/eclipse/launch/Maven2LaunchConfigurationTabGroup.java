package org.maven.ide.eclipse.launch;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;

public class Maven2LaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
                new Maven2LaunchMainTab(),
                new JavaJRETab(), 
                new RefreshTab(), 
                //new EnvironmentTab(), 
                new CommonTab()};
        setTabs(tabs);
    }

}
