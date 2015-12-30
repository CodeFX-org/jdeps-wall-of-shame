package org.codefx.jwos.maven;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.DownloadedArtifact;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.codefx.jwos.artifact.ResolvedProject;
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
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.eclipse.aether.version.Version;

import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.codefx.jwos.Util.toImmutableSet;

/**
 * Internal API for communication with Maven.
 * <p>
 * Can resolve project versions and artifact dependencies and download individual artifacts to a local repository.
 * <p>
 * This class is as thread-safe as Aether, which seems to be handling concurrent requests well.
 */
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

	public ResolvedProject detectAllVersionsOf(ProjectCoordinates project) throws RepositoryException {
		Artifact artifact = new DefaultArtifact(project.groupId(), project.artifactId(), "jar", "[0,)");
		Stream<String> versions = repositorySystem
				.resolveVersionRange(
						repositorySystemSession,
						new VersionRangeRequest(artifact, singletonList(mavenCentral), NULL_CONTEXT))
				.getVersions().stream()
				.map(Version::toString);
		return new ResolvedProject(project, project.toArtifactsWithVersions(versions));
	}

	public DownloadedArtifact downloadArtifact(ArtifactCoordinates artifact) throws RepositoryException {
		return new DownloadedArtifact(artifact, downloadArtifact(artifact.toMavenArtifact()));
	}

	private Path downloadArtifact(Artifact artifact) throws RepositoryException {
		ArtifactRequest artifactRequest = new ArtifactRequest(artifact, singletonList(mavenCentral), NULL_CONTEXT);
		ArtifactResult artifactResult = repositorySystem.resolveArtifact(repositorySystemSession, artifactRequest);
		return artifactResult.getArtifact().getFile().toPath();
	}

	public ResolvedArtifact resolveArtifact(ArtifactCoordinates artifact) throws RepositoryException {
		return new ResolvedArtifact(artifact, getDirectDependencies(artifact.toMavenArtifact()));
	}

	private ImmutableSet<ArtifactCoordinates> getAllDependencies(Artifact artifact) throws RepositoryException {
		return ImmutableSet.copyOf(Iterables.concat(
				getAllDependencies(artifact, "compile"),
				getAllDependencies(artifact, "runtime")
		));
	}

	private ImmutableSet<ArtifactCoordinates> getAllDependencies(Artifact artifact, String scope)
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

	private ImmutableSet<ArtifactCoordinates> getDirectDependencies(Artifact artifact) throws RepositoryException {
		ArtifactDescriptorRequest descriptorRequest =
				new ArtifactDescriptorRequest(artifact, singletonList(mavenCentral), null);
		return repositorySystem
				.readArtifactDescriptor(repositorySystemSession, descriptorRequest)
				.getDependencies().stream()
				.filter(this::noTestDependency)
				.map(Dependency::getArtifact)
				.map(ArtifactCoordinates::from)
				.collect(toImmutableSet());
	}

	private boolean noTestDependency(Dependency dependency) {
		return !Objects.equals(dependency.getScope(), "test");
	}

}
