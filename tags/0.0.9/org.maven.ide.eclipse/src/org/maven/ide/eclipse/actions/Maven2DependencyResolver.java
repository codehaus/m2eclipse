
package org.maven.ide.eclipse.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.project.MavenProject;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.ui.text.correction.ChangeCorrectionProposal;
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
import org.maven.ide.eclipse.index.Indexer;
import org.maven.ide.eclipse.index.Indexer.FileInfo;


public class Maven2DependencyResolver implements IQuickAssistProcessor {
  
  public boolean hasAssists(IInvocationContext context) {
    return true;
  }

  public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations) {
    List proposals = new ArrayList();
    for( int i = 0; i < locations.length; i++ ) {
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
    return (IJavaCompletionProposal[]) proposals.toArray( new IJavaCompletionProposal[proposals.size()] );

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
      super("Search dependency for "+query, null, relevance, Maven2Plugin.getImageDescriptor("icons/mjar.gif").createImage());
      this.query = query;
      this.context = context;
      this.organizeImports = organizeImports;
    }

    public void apply(IDocument document) {
      Maven2Plugin plugin = Maven2Plugin.getDefault();
      
      ICompilationUnit cu = context.getCompilationUnit();
      IProject project = cu.getJavaProject().getProject();
      IFile pomFile = project.getFile( new Path( Maven2Plugin.POM_FILE_NAME));
      
      MavenProject mavenProject = ( MavenProject ) plugin.executeInEmbedder( "Read Project", new Maven2Plugin.ReadProjectTask(pomFile) ); 
      Set artifacts = mavenProject==null ? Collections.EMPTY_SET : mavenProject.getArtifacts();

      IWorkbench workbench = plugin.getWorkbench();
      Shell shell = workbench.getDisplay().getActiveShell();

      Maven2RepositorySearchDialog dialog = new Maven2RepositorySearchDialog( shell, plugin.getIndexes(), artifacts, Indexer.NAMES );
      dialog.setQuery(query);

      if( dialog.open()==Window.OK) {
        FileInfo fileInfo = (FileInfo) dialog.getFirstResult();
        
        plugin.addDependency(pomFile, fileInfo.getDependency());
        
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
  
          } catch( Exception e ) {
            Maven2Plugin.log( "Build error", e);
            return;
            
          }
        }
        
      }
    }

    public String getAdditionalProposalInfo() {
      return "Resolve dependencies from Maven2 repository";
    }

  }

}
