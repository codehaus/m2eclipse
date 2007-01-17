package org.maven.ide.eclipse.launch;

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
