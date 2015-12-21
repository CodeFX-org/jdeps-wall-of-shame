package org.codefx.jwos.analysis;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.codefx.jwos.Util;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.DeeplyAnalyzedArtifact;
import org.codefx.jwos.artifact.IdentifiesArtifact;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.file.ResultFile;
import org.codefx.jwos.file.WallOfShame;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

public class AnalysisGraph {

	private final SetMultimap<ProjectCoordinates, AnalysisGraphNode> projects;
	private final Map<ArtifactCoordinates, AnalysisGraphNode> nodes;

	public AnalysisGraph() {
		projects = HashMultimap.create();
		nodes = new HashMap<>();
	}

	public AnalysisGraph(Collection<DeeplyAnalyzedArtifact> analyzedArtifacts) {
		this();
		analyzedArtifacts.forEach(this::addAnalyzedArtifact);
	}

	private void addAnalyzedArtifact(DeeplyAnalyzedArtifact artifact) {
		if (nodes.containsKey(artifact.coordinates()))
			return;

		AnalysisGraphNode artifactNode = new AnalysisGraphNode(artifact.coordinates());
		artifactNode.analyzed(artifact.violations());
		artifactNode.resolved(
				artifact
						.dependees().stream()
						// recursive call to add dependees as nodes so we can find them in the following 'map'
						.peek(this::addAnalyzedArtifact)
						.<AnalysisGraphNode>map(this::getNodeForArtifact)
						.collect(Util.toImmutableSet()));

		projects.put(artifact.coordinates().project(), artifactNode);
		nodes.put(artifact.coordinates(), artifactNode);
	}

	private AnalysisGraphNode getNodeForArtifact(IdentifiesArtifact artifact) {
		return Optional.of(artifact)
				.map(IdentifiesArtifact::coordinates)
				.map(nodes::get)
				.orElseThrow(() -> {
					String message = "There is supposed to be a node for % but there isn't.";
					return new IllegalStateException(format(message, artifact.coordinates()));
				});
	}

}
