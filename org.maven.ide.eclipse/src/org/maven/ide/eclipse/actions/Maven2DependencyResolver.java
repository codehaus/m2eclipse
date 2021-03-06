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

package org.maven.ide.eclipse.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.ui.actions.OrganizeImportsAction;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.embedder.BuildPathManager;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.embedder.ResolverConfiguration;
import org.maven.ide.eclipse.index.Indexer;
import org.maven.ide.eclipse.index.Indexer.FileInfo;


public class Maven2DependencyResolver implements IQuickAssistProcessor {

  public boolean hasAssists(IInvocationContext context) {
    return true;
  }

  public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations) {
    List proposals = new ArrayList();
    for(int i = 0; i < locations.length; i++ ) {
      IProblemLocation location = locations[i];
      String[] arguments = location.getProblemArguments();
      int id = location.getProblemId();
      switch(id) {
        case IProblem.UndefinedType:
        case IProblem.UndefinedName:
          proposals.add(new OpenBuildPathCorrectionProposal(arguments[0], context, 0, true));
          break;

        case IProblem.IsClassPathCorrect:
        case IProblem.ImportNotFound:
          proposals.add(new OpenBuildPathCorrectionProposal(arguments[0], context, 0, false));
          break;
      }

    }
    return (IJavaCompletionProposal[]) proposals.toArray(new IJavaCompletionProposal[proposals.size()]);

    /*
    if (coveringNode == null) {
      return null; 
    }
    if(coveringNode.getNodeType()==ASTNode.SIMPLE_NAME) {
      ASTNode parent = coveringNode.getParent();
      if(parent!=null) {
        switch(parent.getNodeType()) {
          case ASTNode.ASSIGNMENT:
          case ASTNode.SIMPLE_TYPE:
          case ASTNode.QUALIFIED_NAME:
          case ASTNode.MARKER_ANNOTATION:
            return new IJavaCompletionProposal[] { 
                new OpenBuildPathCorrectionProposal(coveringNode.toString(), context, 1, null) 
              };
        }
      }
    }
    
    return null;
    */
  }

  static public final class OpenBuildPathCorrectionProposal extends ChangeCorrectionProposal {
    private final String query;

    private final IInvocationContext context;

    private final boolean organizeImports;

    OpenBuildPathCorrectionProposal(String query, IInvocationContext context, int relevance, boolean organizeImports) {
      super("Search dependency for " + query, null, relevance, Maven2Plugin.getImage("icons/mjar.gif"));
      this.query = query;
      this.context = context;
      this.organizeImports = organizeImports;
    }

    public void apply(IDocument document) {
      Maven2Plugin plugin = Maven2Plugin.getDefault();
      MavenModelManager modelManager = plugin.getMavenModelManager();

      ICompilationUnit cu = context.getCompilationUnit();
      IJavaProject javaProject = cu.getJavaProject();
      IFile pomFile = javaProject.getProject().getFile(new Path(Maven2Plugin.POM_FILE_NAME));

      MavenProject mavenProject = null;
      try {
        ResolverConfiguration resolverConfiguration = BuildPathManager.getResolverConfiguration(javaProject);
        MavenExecutionResult result = modelManager.readMavenProject(pomFile.getLocation().toFile(),
            new NullProgressMonitor(), true, false, resolverConfiguration);
        mavenProject = result.getProject();
//      } catch(CoreException ex) {
//        // TODO move into ReadProjectTask
//        Maven2Plugin.log(ex);
//        Maven2Plugin.getDefault().getConsole().logError(ex.getMessage());
      } catch(Exception ex) {
        // TODO move into ReadProjectTask
        String msg = "Unable to read project";
        Maven2Plugin.log(msg, ex);
        Maven2Plugin.getDefault().getConsole().logError(msg + "; " + ex.toString());
      }

      Set artifacts = mavenProject == null ? Collections.EMPTY_SET : mavenProject.getArtifacts();

      IWorkbench workbench = plugin.getWorkbench();
      Shell shell = workbench.getDisplay().getActiveShell();

      Maven2RepositorySearchDialog dialog = new Maven2RepositorySearchDialog(shell, artifacts, Indexer.NAMES);
      dialog.setQuery(query);

      if(dialog.open() == Window.OK) {
        FileInfo fileInfo = (FileInfo) dialog.getFirstResult();

        modelManager.addDependency(pomFile, fileInfo.getDependency());

        if(organizeImports) {
          IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
          IWorkbenchPage page = window.getActivePage();

          try {
            // plugin.update(pomFile, null);

            // organize imports
            IEditorPart activeEditor = page.getActiveEditor();
            OrganizeImportsAction organizeImportsAction = new OrganizeImportsAction(activeEditor.getEditorSite());
            organizeImportsAction.run(cu);
            activeEditor.doSave(null);

          } catch(Exception e) {
            Maven2Plugin.getDefault().getConsole().logError("Build error; " + e.getMessage());
            return;

          }
        }
      }
    }

    public String getAdditionalProposalInfo() {
      return "Resolve dependencies from Maven repositories";
    }

  }

}
