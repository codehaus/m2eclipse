
package org.maven.ide.eclipse.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;

/**
 * Helper IPropertyTester implementation to check if receiver can be launched with Maven.
 * E.g. it is pom.xml file of folder or project that has pom.xml. 
 *
 * @author Eugene Kuleshov
 */
public class MavenPropertyTester extends PropertyTester {

  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    IAdaptable adaptable = (IAdaptable) receiver;
    
    IProject projectAdapter = (IProject) adaptable.getAdapter(IProject.class);
    if(projectAdapter!=null) {
      return projectAdapter.getFile("pom.xml").exists();
    }
    
    IFolder folderAdapter = (IFolder) adaptable.getAdapter(IFolder.class);
    if(folderAdapter!=null) {
      return folderAdapter.getFile("pom.xml").exists();
    }

    IFile fileAdapter = (IFile) adaptable.getAdapter(IFile.class);
    if(fileAdapter!=null) {
      return fileAdapter.exists() && "pom.xml".equals(fileAdapter.getName());
    }
    
    return false;
  }

}

