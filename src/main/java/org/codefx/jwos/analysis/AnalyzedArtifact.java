package org.codefx.jwos.analysis;

import com.google.common.collect.ImmutableSet;
import org.codefx.jwos.jdeps.dependency.Violation;
import org.codefx.jwos.maven.ArtifactCoordinates;
import org.codefx.jwos.maven.IdentifiesArtifact;

import static java.util.Objects.requireNonNull;

public class AnalyzedArtifact implements IdentifiesArtifact {

	private final ArtifactCoordinates artifact;
	private final ImmutableSet<Violation> violations;
	private final ImmutableSet<ArtifactCoordinates> dependees;

	public AnalyzedArtifact(
			ArtifactCoordinates artifact,
			ImmutableSet<Violation> violations,
			ImmutableSet<ArtifactCoordinates> dependees) {
		this.artifact = requireNonNull(artifact, "The argument 'artifact' must not be null.");
		this.violations = requireNonNull(violations, "The argument 'violations' must not be null.");
		this.dependees = requireNonNull(dependees, "The argument 'dependees' must not be null.");
	}

	@Override
	public ArtifactCoordinates artifact() {
		return artifact;
	}

	public ImmutableSet<Violation> violations() {
		return violations;
	}

	public ImmutableSet<ArtifactCoordinates> dependees() {
		return dependees;
	}
	
}
