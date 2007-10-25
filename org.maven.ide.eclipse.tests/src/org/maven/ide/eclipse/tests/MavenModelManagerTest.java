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

package org.maven.ide.eclipse.tests;

import java.io.File;

import junit.framework.TestCase;

import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.embedder.ResolverConfiguration;


/**
 * @author Eugene Kuleshov
 */
public class MavenModelManagerTest extends TestCase {

  public void testReadMavenProjectWithWebdavExtension() throws Exception {
    File pomFile = new File("projects/MNGECLIPSE-418a/pom.xml");
    ResolverConfiguration resolverConfiguration = new ResolverConfiguration();

    MavenModelManager modelManager = Maven2Plugin.getDefault().getMavenModelManager();
    MavenExecutionResult result = modelManager.readMavenProject(pomFile, new NullProgressMonitor(), false, false,
        resolverConfiguration);

    assertFalse(result.getExceptions().toString(), result.hasExceptions());

    MavenProject project = result.getProject();
    assertNotNull(project);
  }

  public void testReadMavenProjectWithSshExtension() throws Exception {
    File pomFile = new File("projects/MNGECLIPSE-418b/pom.xml");
    ResolverConfiguration resolverConfiguration = new ResolverConfiguration();

    MavenModelManager modelManager = Maven2Plugin.getDefault().getMavenModelManager();
    MavenExecutionResult result = modelManager.readMavenProject(pomFile, new NullProgressMonitor(), false, false,
        resolverConfiguration);

    assertFalse(result.getExceptions().toString(), result.hasExceptions());

    MavenProject project = result.getProject();
    assertNotNull(project);
  }

}
