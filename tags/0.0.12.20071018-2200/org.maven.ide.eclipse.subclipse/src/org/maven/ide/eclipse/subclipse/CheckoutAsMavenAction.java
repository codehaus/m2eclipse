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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.ui.actions.SVNAction;

/**
 * Checkout as Maven project action
 *  
 * @author @author Eugene Kuleshov
 */
public class CheckoutAsMavenAction extends SVNAction {

  protected boolean isEnabled() {
    return getSelectedRemoteFolders().length > 0;
  }

/*
 * @see IActionDelegate#run(IAction)
 */
  public void execute(IAction action) throws InvocationTargetException, InterruptedException {
    ISVNRemoteFolder[] folders = getSelectedRemoteFolders();

    MavenCheckoutWizard wizard = new MavenCheckoutWizard(folders);
    WizardDialog dialog = new WizardDialog(shell, wizard);
    dialog.open();
  }

}
