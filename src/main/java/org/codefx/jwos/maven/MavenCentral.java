package org.codefx.jwos.maven;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RemoteRepository.Builder;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.eclipse.aether.version.Version;

import java.nio.file.Path;

import static java.util.Collections.singletonList;

public class MavenCentral {

	private final RepositorySystem repositorySystem;
	private final RepositorySystemSession repositorySystemSession;
	private final RemoteRepository mavenCentral;

	private static final String NULL_CONTEXT = null;

	public MavenCentral() {
		repositorySystem = newRepositorySystem();
		repositorySystemSession = newSession(repositorySystem);
		mavenCentral = new Builder("central", "default", "http://repo1.maven.org/maven2/").build();
	}

	private static RepositorySystem newRepositorySystem() {
		DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
		locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
		locator.addService(TransporterFactory.class, FileTransporterFactory.class);
		locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

		return locator.getService(RepositorySystem.class);
	}

	private static RepositorySystemSession newSession(RepositorySystem system) {
		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

		LocalRepository localRepo = new LocalRepository("target/local-repo");
		session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

		return session;
	}

	public ResolvedArtifact downloadMavenArtifact(ArtifactCoordinates artifactCoordinates) throws RepositoryException {
		return new ResolvedArtifact(
				artifactCoordinates,
				downloadArtifact(artifactCoordinates.toArtifact()),
				getDependencies(artifactCoordinates.toArtifact()));
	}

	private Path downloadArtifact(Artifact artifact) throws RepositoryException {
		ArtifactRequest artifactRequest = new ArtifactRequest(artifact, singletonList(mavenCentral), NULL_CONTEXT);
		ArtifactResult artifactResult = repositorySystem.resolveArtifact(repositorySystemSession, artifactRequest);
		return artifactResult.getArtifact().getFile().toPath();
	}

	private ImmutableSet<ArtifactCoordinates> getDependencies(Artifact artifact) throws RepositoryException {
		return ImmutableSet.copyOf(Iterables.concat(
				getDependencies(artifact, "compile"),
				getDependencies(artifact, "runtime")
		));
	}

	private ImmutableSet<ArtifactCoordinates> getDependencies(Artifact artifact, String scope)
			throws DependencyCollectionException, DependencyResolutionException {
		Dependency artifactAsDependency = new Dependency(artifact, scope);
		CollectRequest collectRequest = new CollectRequest(artifactAsDependency, singletonList(mavenCentral));
		DependencyNode dependencyRoot =
				repositorySystem.collectDependencies(repositorySystemSession, collectRequest).getRoot();

		DependencyRequest dependencyRequest = new DependencyRequest(dependencyRoot, null);
		repositorySystem.resolveDependencies(repositorySystemSession, dependencyRequest);

		PreorderNodeListGenerator dependencies = new PreorderNodeListGenerator();
		dependencyRoot.getChildren().forEach(node -> node.accept(dependencies));

		ImmutableSet.Builder<ArtifactCoordinates> dependencyCoordinates = ImmutableSet.builder();
		dependencies.getArtifacts(true).stream().map(ArtifactCoordinates::from).forEach(dependencyCoordinates::add);
		return dependencyCoordinates.build();
	}

	public ImmutableList<Version> detectAllVersionsOf(ProjectCoordinates project) throws RepositoryException {
		Artifact artifact = new DefaultArtifact(project.groupId(), project.artifactId(), "jar", "[0,)");
		VersionRangeResult versionRange = repositorySystem.resolveVersionRange(
				repositorySystemSession,
				new VersionRangeRequest(artifact, singletonList(mavenCentral), NULL_CONTEXT));
		return ImmutableList.copyOf(versionRange.getVersions());
	}

}
