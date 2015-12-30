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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
 * This class is highly mutable and thread-unsafe. It serves as a data structure for {@link AnalysisTaskManager} and
 * 
 */
class AnalysisGraph {

	private final Map<ProjectCoordinates, ProjectNode> projects;
	private final Map<ArtifactCoordinates, ArtifactNode> artifacts;

	// CREATE

	public AnalysisGraph() {
		projects = new HashMap<>();
		artifacts = new HashMap<>();
	}

	public AnalysisGraph(
			Collection<ProjectCoordinates> resolvedProjects, Collection<DeeplyAnalyzedArtifact> analyzedArtifacts) {
		this();
		analyzedArtifacts.forEach(this::addAnalyzedArtifact);
		resolvedProjects.forEach(this::markProjectAsResolved);
	}

	private void addAnalyzedArtifact(DeeplyAnalyzedArtifact artifact) {
		if (getNodeForArtifact(artifact).isPresent())
			return;

		ArtifactNode artifactNode = new ArtifactNode(artifact.coordinates());
		artifactNode.analysis().succeeded(artifact.violations());
		artifactNode.resolution().succeeded(
				artifact
						.dependees().stream()
						// recursive call to add computation as artifacts so we can find them in the following 'map'
						.peek(this::addAnalyzedArtifact)
						.<ArtifactNode>map(this::getExistingNodeForArtifact)
						.collect(toImmutableSet()));

		registerArtifactForProject(artifact.coordinates().project(), artifactNode);
		registerArtifactInGraph(artifactNode);
	}

	private void markProjectAsResolved(ProjectCoordinates projectCoordinates) {
		ProjectNode node = getOrCreateNodeForProject(projectCoordinates);
		ImmutableSet<ArtifactNode> versions = ImmutableSet.copyOf(node.versions());
		node.resolution().succeeded(versions);
	}

	// GET & PUT

	private Optional<ArtifactNode> getNodeForArtifact(IdentifiesArtifact artifact) {
		return Optional.of(artifact)
				.map(IdentifiesArtifact::coordinates)
				.map(artifacts::get);
	}

	private ArtifactNode getOrCreateNodeForArtifact(IdentifiesArtifact artifact) {
		return getNodeForArtifact(artifact).orElseGet(() -> new ArtifactNode(artifact));
	}

	private ArtifactNode getExistingNodeForArtifact(IdentifiesArtifact artifact) {
		return getNodeForArtifact(artifact)
				.orElseThrow(() -> {
					String message = "There is supposed to be a node for artifact % but there isn't.";
					return new IllegalStateException(format(message, artifact.coordinates()));
				});
	}

	private Optional<ProjectNode> getNodeForProject(IdentifiesProject project) {
		return Optional.of(project)
				.map(IdentifiesProject::coordinates)
				.map(projects::get);
	}

	private ProjectNode getOrCreateNodeForProject(IdentifiesProject project) {
		return getNodeForProject(project).orElseGet(() -> new ProjectNode(project));
	}

	private ProjectNode getExistingNodeForProject(IdentifiesProject project) {
		return getNodeForProject(project)
				.orElseThrow(() -> {
					String message = "There is supposed to be a node for project % but there isn't.";
					return new IllegalStateException(format(message, project.coordinates()));
				});
	}

	private void registerArtifactForProject(IdentifiesProject project, ArtifactNode artifactNode) {
		getOrCreateNodeForProject(project).versions().add(artifactNode);
	}

	private void registerArtifactInGraph(ArtifactNode node) {
		artifacts.put(node.coordinates(), node);
	}

	private ArtifactNode registerArtifact(ArtifactCoordinates artifact) {
		ArtifactNode node = getOrCreateNodeForArtifact(artifact);
		registerArtifactForProject(artifact.project(), node);
		registerArtifactInGraph(node);
		return node;
	}

	// PROJECTS

	public void addProject(ProjectCoordinates project) {
		projects.putIfAbsent(project, new ProjectNode(project));
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

	public Stream<ProjectNode> projectNodes() {
		return projects.values().stream();
	}

	public Task<ImmutableSet<ArtifactCoordinates>> versionResolutionOf(IdentifiesProject project) {
		return new GraphUpdatingProjectVersionTask(getExistingNodeForProject(project));
	}

	/**
	 * Presents {@link ArtifactCoordinates} instead of {@link ArtifactNode}s and updates the graph when the computation
	 * succeeded.
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
					.map(AnalysisGraph.this::registerArtifact)
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
