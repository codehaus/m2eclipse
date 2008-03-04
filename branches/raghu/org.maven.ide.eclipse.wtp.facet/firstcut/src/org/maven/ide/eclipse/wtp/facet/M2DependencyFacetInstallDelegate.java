package org.maven.ide.eclipse.wtp.facet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

public final class M2DependencyFacetInstallDelegate implements IDelegate
{
	public void execute(final IProject pj, final IProjectFacetVersion fv,
			final Object config, final IProgressMonitor monitor)

	throws CoreException

	{

		M2DependencyFacetInstallConfig cfg = (M2DependencyFacetInstallConfig) config;
		SharedConfigurations sharedConfigurations = cfg
				.getSharedConfigurations();
		System.out.println("in deps delegate");
		Utils.instance.enableMavenNature(pj, sharedConfigurations, monitor);

	}
}
