package org.maven.ide.eclipse.wtp.facet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

public final class M2ArtifactFacetInstallDelegate implements IDelegate
{
	public void execute(final IProject pj, final IProjectFacetVersion fv,
			final Object config, final IProgressMonitor monitor)

	throws CoreException

	{

		final M2ArtifactFacetInstallConfig cfg = (M2ArtifactFacetInstallConfig) config;
		SharedConfigurations sharedConfigurations = cfg
				.getSharedConfigurations();
		M2FacetPlugin.log("In execute:is cfg null?= "
				+ (cfg==null));
		M2FacetPlugin.log("In execute:sharedConfigurations.isM2DependencyPageSelected= "
						+ sharedConfigurations.isLastPage());
		
		if (sharedConfigurations.isLastPage())
		{

			Utils.instance.enableMavenNature(pj, sharedConfigurations, monitor);

		}
	}
}
