package org.codehaus.tycho.targetplatform;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.tycho.CLITools;
import org.codehaus.tycho.TychoException;
import org.codehaus.tycho.osgitools.GroupMapper;
import org.codehaus.tycho.osgitools.OsgiStateController;
import org.codehaus.tycho.osgitools.PomGenerator;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.osgi.framework.BundleException;

/**
 * Installs or deploys a group of bundles from a a valid Eclipse install to the
 * Maven repository
 * 
 * @requiresProject false
 */
public abstract class AbstractDeployBundlesMojo extends AbstractMojo {

	/**
	 * @parameter expression="${baseDir}" default-value="."
	 * @required
	 */
	private File baseDir;

	/**
	 * a comma separated list of regular expressions to select the bundles that
	 * will be deployed
	 * 
	 * @parameter expression="${include}" default-value=".*"
	 * @required
	 */
	private String include;

	/**
	 * File that contains a list of symbolic-name-pattern=groupId
	 * 
	 * @parameter expression="${groupList}" default-value="group.list"
	 */
	private File groupList;

	/**
	 * String that will be appended to the version
	 * 
	 * @parameter expression="${versionClassifier}"
	 */
	private String versionQualifier;

	/**
	 * Component used to create an artifact
	 * 
	 * @component
	 */
	private ArtifactFactory artifactFactory;

	/**
	 * @component
	 */
	private PomGenerator pomGenerator;

	/**
	 * @component
	 */
	protected CLITools cliTools;

	private SourceManager sourceManager;

	private Model targetPlatformPom;

	private List noSourceFound = new ArrayList();

	/**
	 * @parameter expression="${artifactId}"
	 */
	private String artifactId;

	private Artifact targetPlatformArtifact;

	public void execute() throws MojoExecutionException {
		try {

			if (!StringUtils.isEmpty(artifactId)) {
				targetPlatformArtifact = cliTools.createArtifact(artifactId,
						"jar");
				targetPlatformPom = new Model();
				targetPlatformPom.setModelVersion("4.0.0");
				targetPlatformPom.setArtifactId(targetPlatformArtifact
						.getArtifactId());
				targetPlatformPom.setGroupId(targetPlatformArtifact
						.getGroupId());
				targetPlatformPom.setVersion(targetPlatformArtifact
						.getVersion());
				targetPlatformPom.setPackaging("eclipse-feature");
			}

			OsgiStateController state = createOsgiState();

			String filters[] = include.split(",");
			Pattern[] patterns = new Pattern[filters.length];
			for (int i = 0; i < patterns.length; i++) {
				patterns[i] = Pattern.compile(filters[i]);
			}

			GroupMapper groupMapper = new GroupMapper(groupList);
			BundleDescription[] bundles = state.getState().getBundles();
			List deployed = filterDeployed(patterns, bundles);

			sourceManager = new SourceManager(baseDir, state);

			for (Iterator iterator = deployed.iterator(); iterator.hasNext();) {
				BundleDescription bundle = (BundleDescription) iterator.next();
				if (state.getState().getResolverErrors(bundle).length == 0) {
					Model model = pomGenerator.createBundlePom(groupMapper,
							deployed, versionQualifier, bundle);
					deploy(bundle, model);
				} else {
					getLog().error(
							"Not deploying " + bundle
									+ " because of resolving errrors");
				}
			}

			if (targetPlatformPom != null) {
				File pom = writePom(targetPlatformPom);
				pom.deleteOnExit();
				targetPlatformArtifact.setFile(createEmptyJar());
				ArtifactMetadata metadata = new ProjectArtifactMetadata(
						targetPlatformArtifact, pom);
				targetPlatformArtifact.addMetadata(metadata);
				deployArtifact(targetPlatformArtifact);
			}

			if (!noSourceFound.isEmpty()) {
				getLog().info("No source found for: ");
				for (Iterator iter = noSourceFound.iterator(); iter.hasNext();) {
					String element = (String) iter.next();
					getLog().info("\t" + element);

				}
			}

		} catch (Exception e) {
			throw new MojoExecutionException("Error", e);
		}
	}

	private List filterDeployed(Pattern[] patterns, BundleDescription[] bundles) {
		ArrayList result = new ArrayList();
		for (int i = 0; i < bundles.length; i++) {
			BundleDescription b = bundles[i];
			getLog().info("checking " + b.getSymbolicName());
			for (int j = 0; j < patterns.length; j++) {
				Pattern p = patterns[j];
				if (p.matcher(b.getSymbolicName()).matches()) {
					result.add(b);
				}
			}
		}
		return result;
	}

	private OsgiStateController createOsgiState() throws MojoExecutionException {
		OsgiStateController state = new OsgiStateController();
		File[] bundles = new File(baseDir, "plugins")
				.listFiles(new FileFilter() {
					public boolean accept(File pathname) {
						return pathname.getName().endsWith(".jar")
								|| (pathname.isDirectory() && (new File(
										pathname, JarFile.MANIFEST_NAME)
										.exists() || new File(pathname,
										"plugin.xml").exists()));
					}
				});

		if (bundles == null) {
			throw new MojoExecutionException("No bundles found!");
		}

		for (int i = 0; i < bundles.length; i++) {
			File bundle = bundles[i];
			try {
				state.addBundle(bundle);
			} catch (BundleException e) {
				getLog().info("Could not add bundle: " + bundle);
			}
		}
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

		return state;
	}

	private void deploy(BundleDescription bundle, Model model)
			throws MojoExecutionException {

		File location = new File(bundle.getLocation());
		if (!SourceManager.getSourceDirs(location).isEmpty()) {
			return;
		}

		if (location.isDirectory()) {
			location = createJar(location);
			location.deleteOnExit();
		}

		Artifact artifact = artifactFactory.createArtifactWithClassifier(model
				.getGroupId(), model.getArtifactId(), model.getVersion(),
				"jar", null);
		artifact.setFile(location);

		try {
			File pom = writePom(model);
			pom.deleteOnExit();

			ArtifactMetadata metadata = new ProjectArtifactMetadata(artifact,
					pom);
			artifact.addMetadata(metadata);

			deployArtifact(artifact);

			if (targetPlatformPom != null) {
				Dependency dep = new Dependency();
				dep.setGroupId(model.getGroupId());
				dep.setArtifactId(model.getArtifactId());
				dep.setVersion(model.getVersion());
				dep.setScope(Artifact.SCOPE_COMPILE);
				targetPlatformPom.addDependency(dep);
			}

			File sourceFile = sourceManager.getSourceZip(model.getArtifactId(),
					model.getVersion());

			if (sourceFile != null) {
				Artifact sourceArtifact = artifactFactory
						.createArtifactWithClassifier(model.getGroupId(), model
								.getArtifactId(), model.getVersion(), "jar",
								"sources");
				sourceArtifact.setFile(sourceFile);
				deployArtifact(sourceArtifact);
			} else {
				noSourceFound.add(model.getArtifactId());
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Could not write POM", e);
		} catch (TychoException e) {
			throw new MojoExecutionException("Could not install/deploy "
					+ artifact, e);
		}
	}

	public abstract void deployArtifact(Artifact artifact)
			throws TychoException;

	private File writePom(Model model) throws IOException {
		File pom = File.createTempFile("deploy", ".pom");
		FileWriter fw = new FileWriter(pom);
		try {
			new MavenXpp3Writer().write(fw, model);
			return pom;
		} finally {
			fw.close();
		}
	}

	private File createJar(File location) {
		try {
			File temp = File.createTempFile("deploy", ".jar");
			JarArchiver archiver = new JarArchiver();
			archiver.addDirectory(location, new String[] { "**/*" },
					new String[] { "**/.svn", "**/.svn/**" });
			archiver.setDestFile(temp);
			File manifest = new File(location, JarFile.MANIFEST_NAME);
			if (manifest.exists()) {
				archiver.setManifest(manifest);
			}
			archiver.createArchive();
			return temp;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private File createEmptyJar() {
		try {
			File temp = File.createTempFile("deploy", ".jar");
			JarArchiver archiver = new JarArchiver();
			archiver.setDestFile(temp);
			archiver.createArchive();
			return temp;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}