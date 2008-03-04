package org.maven.ide.eclipse.wtp.facet;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.common.project.facet.ui.AbstractFacetWizardPage;
import org.maven.ide.eclipse.embedder.ResolverConfiguration;
import org.maven.ide.eclipse.wizards.ResolverConfigurationComponent;


public abstract class AbstractM2FacetInstallPage extends
		AbstractFacetWizardPage
{
	protected WizardPage displayLogic;
	public AbstractM2FacetInstallPage(String name)
	{
		super(name);

	}

	public abstract void setConfig(final Object config);

	public abstract void transferStateToConfig();


	public void setVisible(boolean visible)
	{
		displayLogic.setVisible(visible);
		super.setVisible(visible);
	}
	
	public void setPageComplete(boolean complete)
	{
		super.setPageComplete(complete);
		displayLogic.setPageComplete(complete);
	}
	
	protected static SharedConfigurations sharedConfigurations;
	public boolean isPageComplete()
	{
		return displayLogic.isPageComplete();
	}

}
