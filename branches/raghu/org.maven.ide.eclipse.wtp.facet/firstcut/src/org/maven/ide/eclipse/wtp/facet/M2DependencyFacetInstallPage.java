package org.maven.ide.eclipse.wtp.facet;

import org.eclipse.swt.widgets.Composite;
import org.maven.ide.eclipse.Messages;
import org.maven.ide.eclipse.wizards.Maven2DependenciesWizardPage;

public class M2DependencyFacetInstallPage extends AbstractM2FacetInstallPage
{
	

	public M2DependencyFacetInstallPage()
	{
		super("m2.dependencies.facet.install.page");
		displayLogic = new Maven2DependenciesWizardPage(sharedConfigurations.getResolverConfiguration())
		
		{
		}
		;
		
		


		displayLogic.setWizard(this.getWizard());
		setTitle(Messages.getString("wizard.project.page.dependencies.title"));
		setDescription(Messages
				.getString("wizard.project.page.dependencies.description"));
		
		setPageComplete(true);
	}
	
	

	public void createControl(Composite parent)
	{
		//Composite composite = new Composite(parent, 0);
		displayLogic.createControl(parent);
		
		setControl(displayLogic.getControl());
		
	}
	/*
	public Control getControl()
	{
		return displayLogic.getControl();
	}*/

	public void setConfig(final Object config)
	{
		this.config = (M2ArtifactFacetInstallConfig) config;
	}
	

	public void transferStateToConfig()
	{
		M2FacetPlugin.log("transferring dependencies in dependency install page");
		Maven2DependenciesWizardPage actualPage=(Maven2DependenciesWizardPage) this.displayLogic;
		this.sharedConfigurations.setLastPage(true);
		this.sharedConfigurations.setDependencies(actualPage.getDependencies());
		this.config.setSharedConfigurations(this.sharedConfigurations);
		M2FacetPlugin.log("transferred sharedConfigurations ");
		M2FacetPlugin.log("transferred dependencies");

	}

	M2ArtifactFacetInstallConfig config;

	

}
