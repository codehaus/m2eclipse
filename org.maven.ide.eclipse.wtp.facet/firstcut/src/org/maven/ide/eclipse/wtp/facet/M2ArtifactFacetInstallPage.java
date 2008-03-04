package org.maven.ide.eclipse.wtp.facet;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Model;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.common.project.facet.ui.ModifyFacetedProjectWizard;
import org.eclipse.wst.common.project.facet.ui.internal.FacetsSelectionPage;
import org.maven.ide.eclipse.Messages;
import org.maven.ide.eclipse.embedder.ResolverConfiguration;
import org.maven.ide.eclipse.wizards.Maven2ArtifactComponent;
import org.maven.ide.eclipse.wizards.Maven2ProjectWizardArtifactPage;
import org.maven.ide.eclipse.wizards.Maven2ProjectWizardLocationPage;

public final class M2ArtifactFacetInstallPage extends
		AbstractM2FacetInstallPage
{

	public M2ArtifactFacetInstallPage()
	{
		super("m2.artifact.facet.install.page");
		
		sharedConfigurations = new SharedConfigurations();
		Maven2ProjectWizardLocationPage locationPage = new Maven2ProjectWizardLocationPage(
				sharedConfigurations.getResolverConfiguration())
		{
			public String getProjectName()
			{

				return M2ArtifactFacetInstallPage.this.context.getProjectName();
			}

		};

		displayLogic = new Maven2ProjectWizardArtifactPage(sharedConfigurations
				.getResolverConfiguration(), locationPage)
		{

		}

		;
		displayLogic.setWizard(this.getWizard());

		setTitle(Messages.getString("wizard.project.page.maven2.title"));
		setDescription(Messages
				.getString("wizard.project.page.maven2.description"));

		setPageComplete(false);
	}

	

	public void createControl(final Composite parent)
	{
		// Composite composite = new Composite(parent, 0);
		

		displayLogic.createControl(parent);
		workaround(parent);
		setControl(displayLogic.getControl());
		IWizard theWizard = this.getWizard();

		IWizardPage[] pages = theWizard.getPages();
		for (int i = 0; i < pages.length; i++)
		{

			if (pages[i] instanceof FacetsSelectionPage)
			{
				FacetsSelectionPage facetsSelectionPage = (FacetsSelectionPage) pages[i];
				try
				{
					Field field = facetsSelectionPage.getClass()
							.getDeclaredField("listeners");
					field.setAccessible(true);
					List listeners = (List) field.get(facetsSelectionPage);
					for (int j = 0, size = listeners.size(); j < size; j++)
					{
						Listener listener = (Listener) listeners.get(j);
						
						if(listener.getClass().isAnonymousClass())
						{
							if(listener.getClass().getEnclosingClass()==ModifyFacetedProjectWizard.class)
							{
								WorkaroundListener workaroundListener= new WorkaroundListener(listener, this.context, facetsSelectionPage);
								listeners.set(j, workaroundListener);
								workaroundListener.enableOrDisableM2RelatedControls();
								M2FacetPlugin.log("Modified listener" );
								
							}
						}
					}
					

				}
				catch (Exception e)
				{
					M2FacetPlugin.log(e);
					e.printStackTrace();
				}

				
				
			}
		}

		

	}

	

	private void workaround(final Composite parent)
	{

		Control[] controls = parent.getChildren();
		for (int i = 0; i < controls.length; i++)
		{

			if (controls[i] instanceof Combo)
			{
				Combo combo = (Combo) controls[i];
				String[] items = combo.getItems();
				String[] check = { "jar", "war", "ear", "rar" };
				int matches = 0;
				if (check.length == items.length)
				{
					for (int j = 0; j < items.length; j++)
					{
						if (check[j].equals(items[j]))
						{
							matches++;
						}
					}
				}

				if (matches == 4)
				{
					combo.select(1);
					combo.setEnabled(false);
				}

			}
			else if (controls[i] instanceof Composite)
			{
				Composite composite = (Composite) controls[i];
				workaround(composite);
			}
			else if (controls[i] instanceof Button)
			{
				Button button = (Button) controls[i];
				Object data = button.getData();

				if (data != null)
				{

					// if(data instanceof Maven2Directory)
					// unable to do above test unless the class is made isible
					// from the plugin
					if (button.toString().equals("Button {src/main/webapp}"))
					{
						button.setSelection(true);
						button.setEnabled(false);
						try
						{
							Field field = Maven2ProjectWizardArtifactPage.class
									.getDeclaredField("directoriesComponent");
							field.setAccessible(true);
							Object directoriesComponent = field
									.get(displayLogic);
							field = directoriesComponent.getClass()
									.getDeclaredField("directories");
							field.setAccessible(true);
							Set directories = (Set) field
									.get(directoriesComponent);
							directories.add(button.getData());
						}
						catch (Exception e)
						{
							M2FacetPlugin.log(e);
							e.printStackTrace();
						}

					}
				}

			}

		}
	}

	public void setConfig(final Object config)
	{
		this.config = (M2ArtifactFacetInstallConfig) config;
	}

	public void transferStateToConfig()
	{
		M2FacetPlugin.log("transferring artifacts in facets install page");
		Maven2ProjectWizardArtifactPage actualPage = (Maven2ProjectWizardArtifactPage) displayLogic;
		this.sharedConfigurations.setLastPage(false);
		this.sharedConfigurations.setDirectories(actualPage.getDirectories());
		Model model = actualPage.getModel();
		M2FacetPlugin.log("is model null" + (model == null));
		this.sharedConfigurations.setArtifactModel(model);
		config.setSharedConfigurations(this.sharedConfigurations);
		M2FacetPlugin.log("transferred sharedConfigurations ");
		M2FacetPlugin.log("transferred artifacts");

	}

	private M2ArtifactFacetInstallConfig config;

}
