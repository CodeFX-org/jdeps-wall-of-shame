package org.codefx.jwos.analysis;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import org.codefx.jwos.analysis.state.Computation;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.DeeplyAnalyzedArtifact;
import org.codefx.jwos.artifact.IdentifiesArtifact;
import org.codefx.jwos.artifact.IdentifiesProject;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.artifact.ResolvedProject;
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

class AnalysisGraph {

	/**
	 * The code relies on the set-semantic of (key,value)-pairs.
	 */
	private final SetMultimap<ProjectCoordinates, AnalysisNode> projects;
	private final Map<ArtifactCoordinates, AnalysisNode> nodes;

	// CREATE

	public AnalysisGraph() {
		projects = HashMultimap.create();
		nodes = new HashMap<>();
	}

	public AnalysisGraph(Collection<DeeplyAnalyzedArtifact> analyzedArtifacts) {
		this();
		analyzedArtifacts.forEach(this::addAnalyzedArtifact);
	}

	private void addAnalyzedArtifact(DeeplyAnalyzedArtifact artifact) {
		if (getNodeForArtifact(artifact).isPresent())
			return;

		AnalysisNode artifactNode = new AnalysisNode(artifact.coordinates());
		artifactNode.analysis().succeeded(artifact.violations());
		artifactNode.resolution().succeeded(
				artifact
						.dependees().stream()
						// recursive call to add dependees as nodes so we can find them in the following 'map'
						.peek(this::addAnalyzedArtifact)
						.<AnalysisNode>map(this::getExistingNodeForArtifact)
						.collect(toImmutableSet()));

		registerNodeForProject(artifact.coordinates().project(), artifactNode);
		registerNodeInGraph(artifactNode);
	}

	// GET & PUT

	private Optional<AnalysisNode> getNodeForArtifact(IdentifiesArtifact artifact) {
		return Optional.of(artifact)
				.map(IdentifiesArtifact::coordinates)
				.map(nodes::get);
	}

	private AnalysisNode getOrCreateNodeForArtifact(IdentifiesArtifact artifact) {
		return getNodeForArtifact(artifact).orElseGet(() -> new AnalysisNode(artifact));
	}

	private AnalysisNode getExistingNodeForArtifact(IdentifiesArtifact artifact) {
		return getNodeForArtifact(artifact)
				.orElseThrow(() -> {
					String message = "There is supposed to be a node for % but there isn't.";
					return new IllegalStateException(format(message, artifact.coordinates()));
				});
	}

	private void registerNodeForProject(IdentifiesProject project, AnalysisNode artifactNode) {
		projects.put(project.coordinates(), artifactNode);
	}

	private void registerNodeInGraph(AnalysisNode node) {
		nodes.put(node.coordinates(), node);
	}

	private AnalysisNode registerArtifact(ArtifactCoordinates artifact) {
		AnalysisNode node = getOrCreateNodeForArtifact(artifact);
		registerNodeForProject(artifact.project(), node);
		registerNodeInGraph(node);
		return node;
	}

	// UPDATE

	public void resolvedVersions(ResolvedProject project) {
		project
				.versions().stream()
				.map(this::getOrCreateNodeForArtifact)
				.peek(artifactNode -> registerNodeForProject(project, artifactNode))
				.forEach(this::registerNodeInGraph);
	}

	private void resolvedDepenencies(ArtifactCoordinates dependent, ImmutableSet<ArtifactCoordinates> dependees) {
		dependees.forEach(this::registerArtifact);
	}

	// QUERY

	public Stream<AnalysisNode> artifactNodes() {
		return nodes.values().stream();
	}

	public Computation<Path> downloadOf(IdentifiesArtifact artifact) {
		return getExistingNodeForArtifact(artifact).download();
	}

	public Computation<ImmutableSet<Violation>> analysisOf(IdentifiesArtifact artifact) {
		return getExistingNodeForArtifact(artifact).analysis();
	}

	public Computation<ImmutableSet<ArtifactCoordinates>> resolutionOf(IdentifiesArtifact artifact) {
		return new GraphUpdatingDependeeComputation(
				artifact.coordinates(), getExistingNodeForArtifact(artifact).resolution());
	}

	/**
	 * Presents {@link ArtifactCoordinates} instead of {@link AnalysisNode}s and updates the graph when dependees were
	 * computed.
	 */
	private class GraphUpdatingDependeeComputation extends Computation<ImmutableSet<ArtifactCoordinates>> {

		private final ArtifactCoordinates artifact;
		private final Computation<ImmutableSet<AnalysisNode>> dependees;

		private GraphUpdatingDependeeComputation(
				ArtifactCoordinates artifact, Computation<ImmutableSet<AnalysisNode>> dependees) {
			this.artifact = requireNonNull(artifact, "The argument 'artifact' must not be null.");
			this.dependees = requireNonNull(dependees, "The argument 'dependees' must not be null.");
		}

		@Override
		public void queued() {
			dependees.queued();
		}

		@Override
		public void started() {
			dependees.started();
		}

		@Override
		public void failed(Exception exception) {
			dependees.failed(exception);
		}

		@Override
		public void succeeded(ImmutableSet<ArtifactCoordinates> result) {
			AnalysisNode dependent = getExistingNodeForArtifact(artifact);
			ImmutableSet<AnalysisNode> dependees = result.stream()
					.map(AnalysisGraph.this::registerArtifact)
					.peek(dependee -> dependee.addAsDependent(dependent))
					.collect(toImmutableSet());
			this.dependees.succeeded(dependees);
		}

		@Override
		public Exception error() {
			return dependees.error();
		}

		@Override
		public ImmutableSet<ArtifactCoordinates> result() {
			return dependees
					.result().stream()
					.map(AnalysisNode::coordinates)
					.collect(toImmutableSet());
		}
	}

}
