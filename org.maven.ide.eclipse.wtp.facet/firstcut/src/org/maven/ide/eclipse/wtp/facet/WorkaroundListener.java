package org.maven.ide.eclipse.wtp.facet;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import org.eclipse.wst.common.project.facet.core.internal.ProjectFacetVersion;
import org.eclipse.wst.common.project.facet.ui.IWizardContext;
import org.eclipse.wst.common.project.facet.ui.internal.FacetsSelectionPage;

public class WorkaroundListener implements Listener
{
	private Listener delegatedListener;
	private IWizardContext wizardContext;
	private FacetsSelectionPage facetsSelectionPage;

	private Label contentDirLabel;
	private Text contentDir;
	private Text sourceDir;
	private Label sourceDirLabel;

	public WorkaroundListener(Listener delegatedListener,
			IWizardContext wizardContext,
			FacetsSelectionPage facetsSelectionPage)
	{
		super();
		this.delegatedListener = delegatedListener;
		this.wizardContext = wizardContext;
		this.facetsSelectionPage = facetsSelectionPage;

		try
		{
			loadFields(facetsSelectionPage);
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void loadFields(FacetsSelectionPage facetsSelectionPage)
			throws NoSuchFieldException, IllegalAccessException
	{
		IWizardPage nextPage = facetsSelectionPage.getNextPage();

		Class c = nextPage.getClass();
		if (c
				.getName()
				.equals(
						"org.eclipse.jst.jee.servlet.ui.project.facet.WebJavaEEFacetInstallPage"))
		{
			loadFieldsInternal(c.getSuperclass(), nextPage);

		}
		else if (c.getName().equals(
				"org.eclipse.jst.servlet.ui.project.facet.WebFacetInstallPage"))
		{
			loadFieldsInternal(c, nextPage);
		}
	}

	private void loadFieldsInternal(Class webFacetInstallPageClass,
			IWizardPage webFacetInstallPage) throws NoSuchFieldException,
			IllegalAccessException
	{

		contentDirLabel = (Label) getWebFacetInstallPageField(
				"contentDirLabel", webFacetInstallPageClass,
				webFacetInstallPage);

		contentDir = (Text) getWebFacetInstallPageField("contentDir",
				webFacetInstallPageClass, webFacetInstallPage);
		sourceDir = (Text) getWebFacetInstallPageField("sourceDir",
				webFacetInstallPageClass, webFacetInstallPage);
		sourceDirLabel = (Label) getWebFacetInstallPageField("sourceDirLabel",
				webFacetInstallPageClass, webFacetInstallPage);
		;
	}

	private Object getWebFacetInstallPageField(String fieldName,
			Class webFacetInstallPageClass, IWizardPage webFacetInstallPage)
			throws NoSuchFieldException, IllegalAccessException
	{
		Field field = webFacetInstallPageClass.getDeclaredField(fieldName);
		field.setAccessible(true);
		Object ret = field.get(webFacetInstallPage);
		return ret;
	}

	public void handleEvent(Event event)
	{
		delegatedListener.handleEvent(event);
		enableOrDisableM2RelatedControls();
	}

	

	public void enableOrDisableM2RelatedControls()
	{
		boolean isM2Selected = isM2Selected();
		if (isM2Selected)
		{
			if(contentDir!=null)
			{
				contentDir.setText("WebContent");
			}
			if(sourceDir!=null)
			{
				sourceDir.setText("src");
			}
			
		}
		boolean enableFlag = !isM2Selected;
		if(contentDir!=null)
		{
			contentDir.setEditable(enableFlag);
			contentDir.setEnabled(enableFlag);
		}
		if(contentDirLabel!=null)
		{
			contentDirLabel.setEnabled(enableFlag);
		}
		if(sourceDir!=null)
		{
			sourceDir.setEditable(enableFlag);
			sourceDir.setEnabled(enableFlag);	
		}
		if(sourceDirLabel!=null)
		{
			sourceDirLabel.setEnabled(enableFlag);
		}
		

	}

	private boolean isM2Selected()
	{

		Collection coll = this.wizardContext.getSelectedProjectFacets();

		boolean isM2Selected = false;
		if (coll != null)
		{
			for (Iterator iterator = coll.iterator(); iterator.hasNext();)
			{
				ProjectFacetVersion projectFacetVersion = (ProjectFacetVersion) iterator
						.next();
				if (projectFacetVersion != null)
				{
					if (projectFacetVersion.getPluginId().equals(
							"org.maven.ide.eclipse.wtp.facet"))
					{
						isM2Selected = true;
					}

				}

			}
		}
		return isM2Selected;
	}

}
