package org.maven.ide.eclipse.wtp.facet;

import java.util.ArrayList;

import org.apache.maven.model.Dependency;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.wst.common.project.facet.core.IActionConfigFactory;
import org.maven.ide.eclipse.wizards.Maven2ArtifactComponent;



public final class M2DependencyFacetInstallConfig {
	protected SharedConfigurations sharedConfigurations;

	public static final class Factory implements IActionConfigFactory {
		public Object create() {
			return new M2DependencyFacetInstallConfig();
		}
	}

	public SharedConfigurations getSharedConfigurations()
	{
		return sharedConfigurations;
	}

	public void setSharedConfigurations(SharedConfigurations sharedConfigurations)
	{
		this.sharedConfigurations = sharedConfigurations;
	}

	

}
