package org.codefx.jwos.analysis;

import com.google.common.collect.ImmutableSet;
import org.codefx.jwos.analysis.task.Task;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.DeeplyAnalyzedArtifact;
import org.codefx.jwos.artifact.IdentifiesArtifact;
import org.codefx.jwos.artifact.IdentifiesProject;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.jdeps.dependency.Violation;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.codefx.jwos.Util.toImmutableSet;

/**
 * The graph of projects, artifacts, and their dependencies.
 * <p>
 * The graph's nodes not only encapsulate an artifact or project but also all the tasks that have to be completed for
 * them. Nodes should only be accessed directly (via {@link #artifactNodes()} or {@link #projectNodes()}) to identify
 * open tasks. Otherwise tasks should be accessed via methods like {@link #downloadOf(IdentifiesArtifact)}.
 * <p>
 * This class serves as a data structure for {@link AnalysisTaskManager}.
 * It is highly mutable and pretty thread-unsafe. The only thread-safety it guarantees is that concurrent read and write
 * access to the collections of project and artifact nodes, respectively, will not fail. Invariants regarding individual
 * projects/artifacts can not be guaranteed for concurrent read or write access.
 */
class AnalysisGraph {

	/*
	 * To allow concurrent read and write access to the projects and artifacts, the maps have to be 'ConcurrentMap's.
	 */
	private final ConcurrentMap<ProjectCoordinates, ProjectNode> projects;
	private final ConcurrentMap<ArtifactCoordinates, ArtifactNode> artifacts;

	// CREATE

	public AnalysisGraph() {
		projects = new ConcurrentHashMap<>();
		artifacts = new ConcurrentHashMap<>();
	}

	// GET & PUT

	private Optional<ArtifactNode> getNodeForArtifact(IdentifiesArtifact artifact) {
		return Optional.of(artifact)
				.map(IdentifiesArtifact::coordinates)
				.map(artifacts::get);
	}

	private ArtifactNode createNodeForArtifact(IdentifiesArtifact artifact) {
		ArtifactNode node = new ArtifactNode(artifact);
		artifacts.put(artifact.coordinates(), node);
		getOrCreateNodeForProject(artifact.coordinates().project())
				.versions()
				.add(node);
		return node;
	}

	private ArtifactNode getOrCreateNodeForArtifact(IdentifiesArtifact artifact) {
		return getNodeForArtifact(artifact)
				.orElseGet(() -> createNodeForArtifact(artifact));
	}

	private ArtifactNode getExistingNodeForArtifact(IdentifiesArtifact artifact) {
		return getNodeForArtifact(artifact)
				.orElseThrow(() -> {
					String message = "There is supposed to be a node for artifact %s but there isn't.";
					return new IllegalStateException(format(message, artifact.coordinates()));
				});
	}

	private Optional<ProjectNode> getNodeForProject(IdentifiesProject project) {
		return Optional.of(project)
				.map(IdentifiesProject::coordinates)
				.map(projects::get);
	}

	private ProjectNode createNodeForProject(IdentifiesProject project) {
		ProjectNode node = new ProjectNode(project);
		projects.put(project.coordinates(), node);
		return node;
	}

	private ProjectNode getOrCreateNodeForProject(IdentifiesProject project) {
		return getNodeForProject(project)
				.orElseGet(() -> createNodeForProject(project));
	}

	private ProjectNode getExistingNodeForProject(IdentifiesProject project) {
		return getNodeForProject(project)
				.orElseThrow(() -> {
					String message = "There is supposed to be a node for project %s but there isn't.";
					return new IllegalStateException(format(message, project.coordinates()));
				});
	}

	// PROJECTS

	public void addProject(ProjectCoordinates project) {
		projects.putIfAbsent(project, new ProjectNode(project));
	}

	public Stream<ProjectNode> projectNodes() {
		return projects.values().stream();
	}

	public Task<ImmutableSet<ArtifactCoordinates>> versionResolutionOf(IdentifiesProject project) {
		return new GraphUpdatingProjectVersionTask(getExistingNodeForProject(project));
	}

	// ARTIFACT TASKS

	public Stream<ArtifactNode> artifactNodes() {
		return artifacts.values().stream();
	}

	public Task<Path> downloadOf(IdentifiesArtifact artifact) {
		return getExistingNodeForArtifact(artifact).download();
	}

	public Task<ImmutableSet<Violation>> analysisOf(IdentifiesArtifact artifact) {
		return getExistingNodeForArtifact(artifact).analysis();
	}

	public Task<ImmutableSet<ArtifactCoordinates>> dependencyResolutionOf(IdentifiesArtifact artifact) {
		return new GraphUpdatingArtifactDependeeTask(getExistingNodeForArtifact(artifact));
	}

	public Task<Void> outputOf(IdentifiesArtifact artifact) {
		return getExistingNodeForArtifact(artifact).output();
	}

	/**
	 * Presents {@link ArtifactCoordinates} instead of {@link ArtifactNode}s.
	 * <p>
	 * Updates the graph when the computation succeeded by
	 * {@link #getOrCreateNodeForArtifact(IdentifiesArtifact) registering} the new artifacts.
	 */
	private abstract class GraphUpdatingArtifactTask extends Task<ImmutableSet<ArtifactCoordinates>> {

		private final Task<ImmutableSet<ArtifactNode>> task;

		protected GraphUpdatingArtifactTask(Task<ImmutableSet<ArtifactNode>> task) {
			this.task = requireNonNull(task, "The argument 'task' must not be null.");
		}

		@Override
		public void queued() {
			task.queued();
		}

		@Override
		public void started() {
			task.started();
		}

		@Override
		public void failed(Exception exception) {
			task.failed(exception);
		}

		@Override
		public void succeeded(ImmutableSet<ArtifactCoordinates> result) {
			ImmutableSet<ArtifactNode> dependees = result.stream()
					.map(AnalysisGraph.this::getOrCreateNodeForArtifact)
					.collect(toImmutableSet());
			updateGraph(dependees);
			task.succeeded(dependees);
		}

		protected abstract void updateGraph(ImmutableSet<ArtifactNode> artifacts);

		@Override
		public Exception error() {
			return task.error();
		}

		@Override
		public ImmutableSet<ArtifactCoordinates> result() {
			return task
					.result().stream()
					.map(ArtifactNode::coordinates)
					.collect(toImmutableSet());
		}
	}

	/**
	 * Presents {@link ArtifactCoordinates} instead of {@link ArtifactNode}s and updates the graph when dependees
	 * were resolved.
	 * <p>
	 * In addition to the update performed by {@link GraphUpdatingArtifactTask} it will add the node specified
	 * during construction {@link ArtifactNode#addAsDependent(ArtifactNode) as a dependent} for the newly resolved
	 * dependees.
	 */
	private class GraphUpdatingArtifactDependeeTask extends GraphUpdatingArtifactTask {

		private final ArtifactNode artifactNode;

		public GraphUpdatingArtifactDependeeTask(ArtifactNode artifactNode) {
			super(artifactNode.resolution());
			this.artifactNode = artifactNode;
		}

		@Override
		protected void updateGraph(ImmutableSet<ArtifactNode> dependees) {
			dependees.forEach(dependee -> dependee.addAsDependent(artifactNode));
		}
	}

	/**
	 * Presents {@link ArtifactCoordinates} instead of {@link ArtifactNode}s and updates the graph when versions were
	 * resolved.
	 * <p>
	 * In addition to the update performed by {@link GraphUpdatingArtifactTask} it will add the newly resolved
	 * artifact versions to {@link ProjectNode#versions}.
	 */
	private class GraphUpdatingProjectVersionTask extends GraphUpdatingArtifactTask {

		private final ProjectNode projectNode;

		public GraphUpdatingProjectVersionTask(ProjectNode projectNode) {
			super(projectNode.resolution());
			this.projectNode = projectNode;
		}

		@Override
		protected void updateGraph(ImmutableSet<ArtifactNode> versions) {
			projectNode.versions().addAll(versions);
		}
	}

}
