package org.codefx.jwos.analysis;

import com.google.common.collect.ImmutableSet;
import org.codefx.jwos.analysis.state.Computation;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.IdentifiesArtifact;
import org.codefx.jwos.artifact.MarkInternalDependencies;
import org.codefx.jwos.jdeps.dependency.Violation;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.util.Objects.requireNonNull;

class ArtifactNode implements IdentifiesArtifact {

	// Naming is hard: dependent -> dependee (so dependents are parents, dependees are children)

	private final ArtifactCoordinates artifact;
	private final Set<ArtifactNode> dependents;

	private final Computation<Path> download;
	private final Computation<ImmutableSet<Violation>> analysis;
	private final Computation<ImmutableSet<ArtifactNode>> resolutionOfDependees;
	private final Computation<MarkInternalDependencies> marker;

	public ArtifactNode(IdentifiesArtifact artifact) {
		this.artifact = requireNonNull(artifact, "The argument 'artifact' must not be null.").coordinates();
		this.dependents = new HashSet<>();

		download = new Computation<>();
		analysis = new Computation<>();
		resolutionOfDependees = new Computation<>();
		marker = new Computation<>();
	}

	@Override
	public ArtifactCoordinates coordinates() {
		return artifact;
	}

	public void addAsDependent(ArtifactNode dependent) {
		dependents.add(dependent);
	}

	public Computation<Path> download() {
		return download;
	}

	public Computation<ImmutableSet<Violation>> analysis() {
		return analysis;
	}

	public Computation<ImmutableSet<ArtifactNode>> resolution() {
		return resolutionOfDependees;
	}

	public Computation<MarkInternalDependencies> marker() {
		return marker;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ArtifactNode that = (ArtifactNode) o;
		return Objects.equals(artifact, that.artifact);
	}

	@Override
	public int hashCode() {
		return Objects.hash(artifact);
	}

	@Override
	public String toString() {
		return "Node: " + artifact;
	}

}
