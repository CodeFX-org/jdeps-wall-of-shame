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

class AnalysisNode implements IdentifiesArtifact {

	// Naming is hard: dependent -> dependee (so dependents are parents, dependees are children)

	private final ArtifactCoordinates artifact;
	private final Set<AnalysisNode> dependents;

	private final Computation<Path> jarFile;
	private final Computation<ImmutableSet<Violation>> violations;
	private final Computation<ImmutableSet<AnalysisNode>> dependees;
	private final Computation<MarkInternalDependencies> marker;

	public AnalysisNode(IdentifiesArtifact artifact) {
		this.artifact = requireNonNull(artifact, "The argument 'artifact' must not be null.").coordinates();
		this.dependents = new HashSet<>();

		jarFile = new Computation<>();
		violations = new Computation<>();
		dependees = new Computation<>();
		marker = new Computation<>();
	}

	@Override
	public ArtifactCoordinates coordinates() {
		return artifact;
	}

	public void addAsDependent(AnalysisNode dependent) {
		dependents.add(dependent);
	}

	public Computation<Path> jarFile() {
		return jarFile;
	}

	public Computation<ImmutableSet<Violation>> violations() {
		return violations;
	}

	public Computation<ImmutableSet<AnalysisNode>> dependees() {
		return dependees;
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
		AnalysisNode that = (AnalysisNode) o;
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
