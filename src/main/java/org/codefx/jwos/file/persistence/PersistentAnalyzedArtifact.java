package org.codefx.jwos.file.persistence;// NOT_PUBLISHED

import org.codefx.jwos.artifact.AnalyzedArtifact;

import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.codefx.jwos.Util.toImmutableSet;

public class PersistentAnalyzedArtifact {

	public PersistentArtifactCoordinates coordinates;
	public Set<PersistentViolation> violations;

	public static PersistentAnalyzedArtifact from(AnalyzedArtifact artifact) {
		PersistentAnalyzedArtifact persistent = new PersistentAnalyzedArtifact();
		persistent.coordinates = PersistentArtifactCoordinates.from(artifact.coordinates());
		persistent.violations = artifact
				.violations().stream()
				.map(PersistentViolation::from)
				.collect(toSet());
		return persistent;
	}

	public AnalyzedArtifact toArtifact() {
		return new AnalyzedArtifact(
				coordinates.toArtifact(),
				violations.stream()
						.map(PersistentViolation::toViolation)
						.collect(toImmutableSet())
		);
	}

}
