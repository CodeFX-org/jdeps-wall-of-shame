package org.codefx.jwos.analysis;

import com.google.common.collect.ImmutableSet;
import org.codefx.jwos.analysis.task.Task;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.CompletedArtifact;
import org.codefx.jwos.artifact.IdentifiesArtifact;
import org.codefx.jwos.jdeps.dependency.Violation;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

class ArtifactNode implements IdentifiesArtifact {

	// Naming is hard: dependent -> dependee (so dependents are parents, dependees are children)

	private final ArtifactCoordinates artifact;
	private final Set<ArtifactNode> dependents;

	private final Task<Path> download;
	private final Task<ImmutableSet<Violation>> analysis;
	private final Task<ImmutableSet<ArtifactNode>> resolutionOfDependees;
	private final Task<CompletedArtifact> completion;
	private final Task<Void> output;

	public ArtifactNode(IdentifiesArtifact artifact) {
		this.artifact = requireNonNull(artifact, "The argument 'artifact' must not be null.").coordinates();
		this.dependents = new HashSet<>();

		download = new Task<>();
		analysis = new Task<>();
		resolutionOfDependees = new Task<>();
		completion = new Task<>();
		output = new Task<>();
	}

	@Override
	public ArtifactCoordinates coordinates() {
		return artifact;
	}

	public void addAsDependent(ArtifactNode dependent) {
		dependents.add(dependent);
	}

	public Stream<ArtifactNode> dependents() {
		return dependents.stream();
	}

	public Task<Path> download() {
		return download;
	}

	public Task<ImmutableSet<Violation>> analysis() {
		return analysis;
	}

	public Task<ImmutableSet<ArtifactNode>> resolution() {
		return resolutionOfDependees;
	}

	public Task<CompletedArtifact> completion() {
		return completion;
	}

	public Task<Void> output() {
		return output;
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
