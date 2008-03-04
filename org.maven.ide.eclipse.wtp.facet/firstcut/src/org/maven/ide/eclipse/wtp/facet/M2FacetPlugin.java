
package org.maven.ide.eclipse.wtp.facet;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public final class M2FacetPlugin

extends AbstractUIPlugin

{
	public static final String PLUGIN_ID = "org.maven.ide.eclipse.wtp.facet";

	private static M2FacetPlugin plugin;

	public M2FacetPlugin()
	{
		plugin = this;
	}

	public static M2FacetPlugin getInstance()
	{
		return plugin;
	}

	public static void log(final Exception e)
	{
		final String msg = e.getMessage() + "";
		log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.OK, msg, e));
	}

	public static void log(final IStatus status)
	{
		getInstance().getLog().log(status);
	}

	public static void log(final String msg)
	{
		log(new Status(IStatus.INFO, PLUGIN_ID, IStatus.OK, msg, null));
	}

	public static IStatus createErrorStatus(final String msg)
	{
		return createErrorStatus(msg, null);
	}

	public static IStatus createErrorStatus(final String msg, final Exception e)
	{
		return new Status(IStatus.ERROR, PLUGIN_ID, 0, msg, e);
	}

}
