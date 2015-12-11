package org.codefx.jwos.analysis;

import com.google.common.collect.ImmutableSet;
import org.codefx.jwos.jdeps.dependency.Violation;
import org.codefx.jwos.maven.ArtifactCoordinates;
import org.codefx.jwos.maven.IdentifiesArtifact;

import static java.util.Objects.requireNonNull;

public class DeeplyAnalyzedArtifact implements IdentifiesArtifact {

	private final ArtifactCoordinates artifact;
	private final InternalDependencies marker;
	private final ImmutableSet<Violation> violations;
	private final ImmutableSet<DeeplyAnalyzedArtifact> dependees;

	public DeeplyAnalyzedArtifact(
			ArtifactCoordinates artifact,
			InternalDependencies marker,
			ImmutableSet<Violation> violations,
			ImmutableSet<DeeplyAnalyzedArtifact> dependees) {
		this.artifact = requireNonNull(artifact, "The argument 'artifact' must not be null.");
		this.marker = requireNonNull(marker, "The argument 'marker' must not be null.");
		this.violations = requireNonNull(violations, "The argument 'violations' must not be null.");
		this.dependees = requireNonNull(dependees, "The argument 'dependees' must not be null.");
	}

	@Override
	public ArtifactCoordinates artifact() {
		return artifact;
	}

	public InternalDependencies marker() {
		return marker;
	}

	public ImmutableSet<Violation> violations() {
		return violations;
	}

	public ImmutableSet<DeeplyAnalyzedArtifact> dependees() {
		return dependees;
	}

}
