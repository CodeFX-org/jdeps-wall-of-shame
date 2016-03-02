package org.codefx.jwos.file.persistence;// NOT_PUBLISHED

import org.codefx.jwos.artifact.AnalyzedArtifact;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.codefx.jwos.Util.toImmutableSet;

public class PersistentAnalyzedArtifact {

	public PersistentArtifactCoordinates artifact;
	public List<PersistentViolation> violations;

	public static PersistentAnalyzedArtifact from(AnalyzedArtifact artifact) {
		PersistentAnalyzedArtifact persistent = new PersistentAnalyzedArtifact();
		persistent.artifact = PersistentArtifactCoordinates.from(artifact.coordinates());
		persistent.violations = artifact
				.violations().stream()
				.map(PersistentViolation::from)
				.collect(toList());
		return persistent;
	}

	public AnalyzedArtifact toArtifact() {
		return new AnalyzedArtifact(
				artifact.toArtifact(),
				violations.stream()
						.map(PersistentViolation::toViolation)
						.collect(toImmutableSet())
		);
	}

}
