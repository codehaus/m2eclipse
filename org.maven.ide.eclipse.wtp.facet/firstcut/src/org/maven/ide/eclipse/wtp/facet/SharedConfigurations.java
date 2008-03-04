package org.maven.ide.eclipse.wtp.facet;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.maven.ide.eclipse.embedder.ResolverConfiguration;

public class SharedConfigurations
{
	protected ResolverConfiguration resolverConfiguration;
	protected Model artifactModel;
	protected String[] directories;
	protected Dependency[] dependencies;
	protected boolean isLastPage;

	public SharedConfigurations()
	{
		super();
		resolverConfiguration = new ResolverConfiguration();
		
	}

	

	
	public Model getArtifactModel()
	{
		return artifactModel;
	}

	public void setArtifactModel(Model artifactModel)
	{
		this.artifactModel = artifactModel;
	}

	public String[] getDirectories()
	{
		return directories;
	}

	public void setDirectories(String[] directories)
	{
		this.directories = directories;
	}

	public ResolverConfiguration getResolverConfiguration()
	{
		return resolverConfiguration;
	}

	public Dependency[] getDependencies()
	{
		return dependencies;
	}

	public void setDependencies(Dependency[] dependencies)
	{
		this.dependencies = dependencies;
	}

	public boolean isLastPage()
	{
		return isLastPage;
	}

	public void setLastPage(boolean isLastPage)
	{
		this.isLastPage = isLastPage;
	}

}
