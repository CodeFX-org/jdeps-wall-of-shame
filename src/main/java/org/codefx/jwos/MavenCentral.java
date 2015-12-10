package org.codefx.jwos;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
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
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

import java.nio.file.Path;

import static java.util.Collections.singletonList;

public class MavenCentral {

	private final RepositorySystem repositorySystem;
	private final RepositorySystemSession repositorySystemSession;
	private final RemoteRepository mavenCentral;

	private static final String NULL_CONTEXT = null;

	public MavenCentral(
			RepositorySystem repositorySystem,
			RepositorySystemSession repositorySystemSession,
			RemoteRepository mavenCentral) {
		this.repositorySystem = repositorySystem;
		this.repositorySystemSession = repositorySystemSession;
		this.mavenCentral = mavenCentral;
	}

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

	public MavenArtifact downloadMavenArtifact(Artifact artifact) throws RepositoryException {
		return new MavenArtifact(
				downloadArtifact(artifact),
				getDependencies(artifact));
	}

	public Path downloadArtifact(Artifact artifact) throws RepositoryException {
		ArtifactRequest artifactRequest = new ArtifactRequest(artifact, singletonList(mavenCentral), NULL_CONTEXT);
		ArtifactResult artifactResult = repositorySystem.resolveArtifact(repositorySystemSession, artifactRequest);
		return artifactResult.getArtifact().getFile().toPath();
	}

	public ImmutableSet<Artifact> getDependencies(Artifact artifact) throws RepositoryException {
		return ImmutableSet.copyOf(Iterables.concat(
				getDependencies(artifact, "compile"),
				getDependencies(artifact, "runtime")
		));
	}

	private ImmutableSet<Artifact> getDependencies(Artifact artifact, String scope)
			throws DependencyCollectionException, DependencyResolutionException {
		Dependency artifactAsDependency = new Dependency(artifact, scope);
		CollectRequest collectRequest = new CollectRequest(artifactAsDependency, singletonList(mavenCentral));
		DependencyNode dependencyRoot =
				repositorySystem.collectDependencies(repositorySystemSession, collectRequest).getRoot();

		DependencyRequest dependencyRequest = new DependencyRequest(dependencyRoot, null);
		repositorySystem.resolveDependencies(repositorySystemSession, dependencyRequest);

		PreorderNodeListGenerator dependencies = new PreorderNodeListGenerator();
		dependencyRoot.getChildren().forEach(node -> node.accept(dependencies));
		return ImmutableSet.copyOf(dependencies.getArtifacts(true));
	}

	public static class MavenArtifact {

		private final Path path;
		private final ImmutableSet<Artifact> dependencies;

		public MavenArtifact(Path path, ImmutableSet<Artifact> dependencies) {
			this.path = path;
			this.dependencies = dependencies;
		}

		public Path path() {
			return path;
		}

		public ImmutableSet<Artifact> dependencies() {
			return dependencies;
		}
	}

}
