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

package org.maven.ide.eclipse.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.maven.ide.eclipse.embedder.ResolverConfiguration;


/**
 * AbstractMavenImportWizardPage
 * 
 * @author Eugene Kuleshov
 */
public abstract class AbstractMavenWizardPage extends WizardPage {

  /** The resolver configuration panel */
  protected ResolverConfigurationComponent resolverConfigurationComponent;

  /**
   * The resolver configuration
   */
  protected ResolverConfiguration resolverConfiguration;

  protected AbstractMavenWizardPage(String pageName) {
    this(pageName, null);
  }
  
  /**
   * Creates a page. This constructor should be used for the wizards where you need to have the advanced settings box on
   * each page. Pass the same bean to each page so they can share the data.
   */
  protected AbstractMavenWizardPage(String pageName, ResolverConfiguration resolverConfiguration) {
    super(pageName);
    this.resolverConfiguration = resolverConfiguration;
  }

  /** Creates an advanced settings panel. */
  protected void createAdvancedSettings(Composite composite, GridData gridData) {
    if(resolverConfiguration != null) {
      resolverConfigurationComponent = new ResolverConfigurationComponent(composite, resolverConfiguration);
      resolverConfigurationComponent.setLayoutData(gridData);
    }
  }

  /** Returns the resolver configuration based on the advanced settings. */
  public ResolverConfiguration getResolverConfiguration() {
    return resolverConfiguration == null ? new ResolverConfiguration() : //
        resolverConfigurationComponent.getResolverConfiguration();
  }

  /** Loads the advanced settings data when the page is displayed. */
  public void setVisible(boolean visible) {
    if(visible && resolverConfigurationComponent != null) {
      resolverConfigurationComponent.loadData();
    }
    super.setVisible(visible);
  }

}
