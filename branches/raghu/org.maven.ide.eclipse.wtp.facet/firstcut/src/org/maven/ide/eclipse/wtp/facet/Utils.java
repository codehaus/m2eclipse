package org.maven.ide.eclipse.wtp.facet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.model.Model;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.j2ee.internal.J2EEConstants;
import org.eclipse.jst.j2ee.web.componentcore.util.WebArtifactEdit;
import org.eclipse.jst.j2ee.webapplication.Servlet;
import org.eclipse.jst.j2ee.webapplication.ServletMapping;
import org.eclipse.jst.j2ee.webapplication.ServletType;
import org.eclipse.jst.j2ee.webapplication.WebApp;
import org.eclipse.jst.j2ee.webapplication.WebapplicationFactory;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualResource;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleType;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerUtil;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.embedder.BuildPathManager;
import org.maven.ide.eclipse.embedder.ResolverConfiguration;

public final class Utils
{
	private static final int UPDATE_FLAGS = IVirtualResource.FORCE
			| IVirtualResource.IGNORE_UNDERLYING_RESOURCE;

	private Utils()
	{

	}

	public static final Utils instance = new Utils();

	public void enableMavenNature(final IProject pj, SharedConfigurations cfg,
			IProgressMonitor monitor) throws CoreException
	{

		if (pj.exists())
		{
			try
			{
				monitor.beginTask("Begining My task", 1);
				final IFile file = pj.getFile("pom.xml");

				if (!file.exists())
				{

					doWork(cfg, monitor, file, pj);

				}
				else
				{
					throwCoreException("POM already exists");
				}

			}
			catch (IOException e)
			{
				throwCoreException("IO Problem:" + e.getMessage(), e);
			}
			finally
			{
				monitor.done();
			}
		}
	}

	boolean completed = false;
	boolean success = false;

	private void doWork(SharedConfigurations cfg,
			final IProgressMonitor monitor, IFile file, final IProject project)
			throws IOException, CoreException
	{
		Model model = cfg.getArtifactModel();
		model.getDependencies().addAll(Arrays.asList(cfg.getDependencies()));
		StringWriter w = new StringWriter();
		MavenEmbedder mavenEmbedder = Maven2Plugin.getDefault()
				.getMavenEmbedderManager().getWorkspaceEmbedder();
		mavenEmbedder.writeModel(w, model, true);
		file.create(new ByteArrayInputStream(w.toString().getBytes("ASCII")),
				true, null);
		monitor.subTask("Enable Maven nature");
		ArrayList newNatures = new ArrayList();
		newNatures.add("org.eclipse.jdt.core.javanature");
		newNatures.add(Maven2Plugin.NATURE_ID);
		IProjectDescription description = project.getDescription();
		String natures[] = description.getNatureIds();
		for (int i = 0; i < natures.length; i++)
		{
			String id = natures[i];
			if (!Maven2Plugin.NATURE_ID.equals(id)
					&& !"org.eclipse.jdt.core.javanature".equals(natures[i]))
				newNatures.add(natures[i]);
		}

		description.setNatureIds((String[]) newNatures
				.toArray(new String[newNatures.size()]));
		project.setDescription(description, monitor);

		final String[] directories = cfg.getDirectories();
		for (int i = 0; i < directories.length; i++)
		{
			System.out.println(directories[i]);
          IFolder folder=project.getFolder(directories[i]);
			createFolder(folder);
			
			if(directories[i].equals("src/main/webapp"))
			{
				IFolder folder1=folder.getFolder("WEB-INF");
				createFolder(folder1);
				IPath dest=folder1.getFullPath().append("web.xml");
				IFile src=project.getFile("WebContent/WEB-INF/web.xml");
				src.move(dest, true, monitor);
				folder1=folder.getFolder("WEB-INF/lib");
				createFolder(folder1);
				folder1=folder.getFolder("META-INF");
				createFolder(folder1);
				dest=folder1.getFullPath().append("MANIFEST.MF");
				src=project.getFile("WebContent/META-INF/MANIFEST.MF");
				src.move(dest, true, monitor);
				project.getFolder("WebContent").delete(true, monitor);
				project.getFolder("build").delete(true, monitor);
				
			}
		}
		final IJavaProject javaProject = JavaCore.create(project);
		final IPath projectPath = project.getFullPath();
		if (javaProject != null)
		{

			final IPath defaultOutputPath = projectPath
					.append("target/classes");

			final IClasspathEntry[] finalEntries = calculateClassPathEntries(
					projectPath, directories, javaProject.getRawClasspath(),cfg.getResolverConfiguration());
			

			WorkspaceJob job = new WorkspaceJob("J2EEComponentMappingUpdateJob")
			{
				public IStatus runInWorkspace(IProgressMonitor monitor)
						throws CoreException
				{
					javaProject.setRawClasspath(finalEntries,
							defaultOutputPath, monitor);
					adjustComponentSettings(project, projectPath,
							monitor, directories);
					System.out.println("completed run");

					return Status.OK_STATUS;
				}
			};

			
			job.setRule(project);
			job.schedule();
			monitor.worked(1);
			// newThreadStart(monitor, project, directories, projectPath);

		}

	}

	

	private void adjustComponentSettings(final IProject project,
			final IPath projectFullPath, IProgressMonitor monitor,
			String[] directories) throws CoreException
	{
		final IVirtualComponent c = ComponentCore.createComponent(project);
		c.create(0, null);
		Properties properties = c.getMetaProperties();
		final IVirtualFolder root = c.getRootFolder();
		c.setMetaProperty("java-output-path", "target/classes");

		IVirtualFolder destFolder = null;
		
		destFolder = root;
		addSrcToDest(project, monitor, directories, destFolder, "src/main/webapp");
		removeSrcFromDest(project, monitor, destFolder, "WebContent");
		
		
		
		destFolder = root.getFolder(new Path(J2EEConstants.WEB_INF_CLASSES));
		removeSrcFromDest(project, monitor, destFolder, "src/test/java");
		removeSrcFromDest(project, monitor, destFolder, "src/test/resources");
		
		
		
		

	}

	

	private void removeSrcFromDest(final IProject project,
			IProgressMonitor monitor, IVirtualFolder destFolder, String srcLink)
			throws CoreException
	{
		IPath path=project.getFolder(srcLink).getProjectRelativePath();
		
		destFolder.removeLink(path,
				UPDATE_FLAGS, monitor);
	}

	private void addSrcToDest(final IProject project, IProgressMonitor monitor,
			String[] directories, IVirtualFolder destFolder, String srcLink)
			throws CoreException
	{
		IPath path=project.getFolder(srcLink).getProjectRelativePath();
		for (int i1 = 0; i1 < directories.length; i1++)
		{
			String check = srcLink;
			if (directories[i1].equals(check))
			{
				destFolder.createLink(path,
						UPDATE_FLAGS, monitor);
			}
		}
	}

	// private static final int
	// UPDATE_FLAGS=IVirtualResource.FORCE|IVirtualResource.IGNORE_UNDERLYING_RESOURCE;

	private IClasspathEntry[] calculateClassPathEntries(IPath projectPath,
			String[] directories, IClasspathEntry[] entries,
			ResolverConfiguration configuration)
	{
		ArrayList originalMinusSrcAndOutput = new ArrayList();

		for (int i = 0; i < entries.length; i++)
		{
			IClasspathEntry entry = entries[i];
		
				if ((entry.getEntryKind() != IClasspathEntry.CPE_SOURCE))
				{
					originalMinusSrcAndOutput.add(entry);
				}
			
		}

		ArrayList newEntries = new ArrayList();
		for (int i = 0; i < directories.length; i++)
		{
			String directory = directories[i];
			if (directory.equals("src/main/java")
					|| directory.equals("src/main/resources"))
			{
				
					IClasspathEntry entry = JavaCore.newSourceEntry(projectPath
							.append(directory));
					newEntries.add(entry);
			
			}
			else if (directory.equals("src/test/java")
					|| directory.equals("src/test/resources"))
			{
				
					IPath outputPath = projectPath
							.append("target/test-classes");
					IClasspathEntry entry = JavaCore.newSourceEntry(projectPath
							.append(directory), new IPath[0], outputPath);
					newEntries.add(entry);
				
			}
		}
		for (Iterator iterator = originalMinusSrcAndOutput.iterator(); iterator
				.hasNext();)
		{
			IClasspathEntry entry = (IClasspathEntry) iterator.next();
			newEntries.add(entry);

		}
		
		IPath mavenContainerPath = new Path(Maven2Plugin.CONTAINER_ID);
        if(configuration.shouldIncludeModules())
            mavenContainerPath = mavenContainerPath.append("modules");
        if(!configuration.shouldResolveWorkspaceProjects())
            mavenContainerPath = mavenContainerPath.append("noworkspace");
        if(configuration.getActiveProfiles().length() > 0)
            mavenContainerPath = mavenContainerPath.append("profiles[" + configuration.getActiveProfiles().trim() + "]");
        IClasspathAttribute attribute=JavaCore.newClasspathAttribute("org.eclipse.jst.component.dependency", "/WEB-INF/lib");
		IClasspathEntry containerEntry=JavaCore.newContainerEntry(mavenContainerPath, new IAccessRule[0],
				new IClasspathAttribute[]{attribute},false);
		
		newEntries.add(containerEntry);

		IClasspathEntry[] finalEntries = new IClasspathEntry[newEntries.size()];
		newEntries.toArray(finalEntries);
		return finalEntries;
	}

	public void createFolder(IFolder folder) throws CoreException
	{
		if (!folder.exists())
		{
			org.eclipse.core.resources.IContainer parent = folder.getParent();
			if (parent instanceof IFolder)
				createFolder((IFolder) parent);
			folder.create(false, true, null);
		}
	}

	

	private void throwCoreException(String message, Throwable cause)
			throws CoreException
	{
		CoreException e = new CoreException(new Status(4,
				M2FacetPlugin.PLUGIN_ID, 0, message, cause));

		e.printStackTrace();
		throw e;
	}

	private void throwCoreException(String message) throws CoreException
	{
		this.throwCoreException(message, null);
	}

}
