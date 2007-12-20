package org.codehaus.tycho.plugin.pom;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarFile;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.osgi.framework.BundleException;
import org.xml.sax.InputSource;

import org.codehaus.tycho.osgitools.GroupMapper;
import org.codehaus.tycho.osgitools.OsgiStateController;

/**
 * @goal synchronize-plugin-pom
 * @requiresProject true
 */
public class SynchronizePluginPomMojo extends AbstractMojo {

	/**
	 * @parameter expression="${targetPlatform}"
	 * @required	 
	 */
	private File targetPlatform;

	/**
	 * @parameter expression="${targetPlatform}/group.list"
	 */
	private File groupList;

	/**
	 * @parameter expression="${project.basedir}"
	 */
	private File baseDir;

	private Map versions = new HashMap();

	/**
	 * @parameter expression="${project.basedir}/pom.xml"
	 */
	private File pomFile;

	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			OsgiStateController state = new OsgiStateController();
			BundleDescription thisBundle;
			try {
				thisBundle = state.addBundle(baseDir);
			} catch (BundleException e) {
				throw new MojoExecutionException(
						"This is not a valid plugin project", e);
			}
			File pluginDir = new File(targetPlatform, "plugins");
			File[] pluginFiles = pluginDir.listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					return pathname.getName().endsWith(".jar")
							|| (pathname.isDirectory() && new File(pathname,
									JarFile.MANIFEST_NAME).exists());
				}
			});
			for (int i = 0; i < pluginFiles.length; i++) {
				File f = pluginFiles[i];
				try {
					state.addBundle(f);
				} catch (BundleException e) {
					throw new MojoExecutionException("Could not add bundle "
							+ f, e);
				}
			}
			File rootProject = findRootProject(baseDir);
			collectPluginProjects(state, rootProject);
			resolveState(state);
			BundleDescription[] dependent = OsgiStateController
					.getDependentBundles(thisBundle);

			Model model = readPom(pomFile);
			
			Iterator it = model.getDependencies().iterator();
			while (it.hasNext()) {
				String scope = ((Dependency) it.next()).getScope();
				if (scope ==null || scope.equals(DefaultArtifact.SCOPE_COMPILE)) {
					it.remove();
				}
			}
			
			GroupMapper groupMapper = new GroupMapper(groupList);
			getLog().info("Dependent bundles:");
			for (int i = 0; i < dependent.length; i++) {
				BundleDescription bd = dependent[i];
				addDependency(model, groupMapper, bd);
			}
			
			new MavenXpp3Writer().write(new OutputStreamWriter(System.out), model);
			
		} catch (IOException e) {
			throw new MojoExecutionException("", e);
		} catch (XmlPullParserException e) {
			throw new MojoExecutionException("", e);
		}
	}

	private void addDependency(Model model, GroupMapper groupMapper, BundleDescription bd) {
		String artifactId = bd.getSymbolicName();
		String groupId = groupMapper.getGroupId(artifactId);

		String version = (String) versions.get(bd);
		if (version == null) {
			version = bd.getVersion().toString();
		}
		
		Dependency d = new Dependency();
		d.setGroupId(groupId);
		d.setArtifactId(artifactId);
		d.setVersion(version);
		model.getDependencies().add(d);
		
		getLog().info(groupId + ":" + artifactId + ":" + version);

		BundleDescription[] fragments = bd.getFragments();
		for (int i = 0; i < fragments.length; i++) {
			addDependency(model, groupMapper, fragments[i]);
		}
		
	}
	
	private Model readPom(File pom) throws IOException, XmlPullParserException {
		return new MavenXpp3Reader().read(new FileReader(pom));
	}

	private File findRootProject(File basedir) {
		File parentDir = basedir.getParentFile();
		if (new File(parentDir, "pom.xml").exists()) {
			return findRootProject(parentDir);
		} else {
			return basedir;
		}
	}

	private void collectPluginProjects(OsgiStateController state, File dir)
			throws MojoExecutionException {
		File manifest = new File(dir, JarFile.MANIFEST_NAME);
		File project = new File(dir, ".project");
		if (project.exists()) {
			if (!dir.equals(baseDir) && manifest.exists()) {
				try {
					BundleDescription bd = state.addBundle(manifest, dir);
					File pom = new File(dir, "pom.xml");
					if (pom.exists()) {
						String version = getVersionFromPom(pom);
						if (version != null) {
							versions.put(bd, version);
						}
					}
				} catch (BundleException e) {
					throw new MojoExecutionException("Could not add bundle "
							+ dir, e);
				}
			}
		} else {
			File[] dirs = dir.listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					return pathname.isDirectory();
				}
			});
			for (int i = 0; i < dirs.length; i++) {
				collectPluginProjects(state, dirs[i]);
			}
		}
	}

	private void resolveState(OsgiStateController state)
			throws MojoExecutionException {
		state.resolveState();
		BundleDescription[] bds = state.getState().getBundles();
		for (int i = 0; i < bds.length; i++) {
			BundleDescription bundle = bds[i];
			ResolverError[] errors = state.getState().getResolverErrors(bundle);
			if (errors.length > 0) {
				getLog()
						.error("Errors for bundle: " + bundle.getSymbolicName());
				for (int j = 0; j < errors.length; j++) {
					ResolverError error = errors[j];
					getLog().error(error.toString());
				}
			}
		}
	}

	private String getVersionFromPom(File pomFile)
			throws MojoExecutionException {
		try {
			XPath path = XPathFactory.newInstance().newXPath();
			String version = path.evaluate("/project/version/text()", new InputSource(
					new FileInputStream(pomFile)));
			if (version == null || "".equals(version)) {
				version = path.evaluate("/project/parent/version/text()",
						new InputSource(new FileInputStream(pomFile)));
			}
			return version;
		} catch (XPathExpressionException e) {
			throw new MojoExecutionException("", e);
		} catch (FileNotFoundException e) {
			throw new MojoExecutionException("", e);
		}
	}

}
