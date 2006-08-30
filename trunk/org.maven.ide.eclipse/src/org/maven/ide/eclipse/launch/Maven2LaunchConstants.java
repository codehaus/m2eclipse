package org.maven.ide.eclipse.launch;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

public interface Maven2LaunchConstants {
    // this should correspond with launchConfigurationType.id attribute in plugin.xml!
    public final String LAUNCH_CONFIGURATION_TYPE_ID = "org.maven.ide.eclipse.Maven2LaunchConfigurationType";
    
    // pom directory automatically became working directory for maven embedder launch
    public final String ATTR_POM_DIR = IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY;
    public final String ATTR_GOALS = "M2_GOALS";
    public final String ATTR_PROFILES = "M2_PROFILES";
    public final String ATTR_PROPERTIES = "M2_PROPERTIES";

}
