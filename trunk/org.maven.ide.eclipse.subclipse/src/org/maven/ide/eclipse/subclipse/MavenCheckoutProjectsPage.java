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

package org.maven.ide.eclipse.subclipse;

import org.eclipse.swt.widgets.Composite;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.wizards.AbstractProjectScanner;
import org.maven.ide.eclipse.wizards.Maven2ImportWizardPage;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;

/**
 * @author Eugene Kuleshov
 */
public class MavenCheckoutProjectsPage extends Maven2ImportWizardPage {

  private final ISVNRemoteFolder[] folders;
  private final MavenCheckoutLocationPage checkoutLocationPage;

  protected MavenCheckoutProjectsPage(ISVNRemoteFolder[] folders, MavenCheckoutLocationPage checkoutLocationPage) {
    setMessage("Select Maven Projects to check out");

    this.folders = folders;
    this.checkoutLocationPage = checkoutLocationPage;
  }

  public void createControl(Composite parent) {
    super.createControl(parent);
//    scanProjects();
  }

  protected AbstractProjectScanner getProjectScanner() {
    return new MavenProjectSVNScanner(checkoutLocationPage.getLocation(), folders, Maven2Plugin.getDefault().getMavenModelManager());
  }

  protected boolean showLocation() {
    return false;
  }
  
}
