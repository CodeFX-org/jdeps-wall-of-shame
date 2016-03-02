package org.codefx.jwos.file.persistence;

import org.codefx.jwos.artifact.AnalyzedArtifact;

import java.util.List;

import static org.codefx.jwos.Util.transformToImmutableSet;
import static org.codefx.jwos.Util.transformToList;

public class PersistentAnalyzedArtifact {

	public PersistentArtifactCoordinates artifact;
	public List<PersistentViolation> violations;

	public static PersistentAnalyzedArtifact from(AnalyzedArtifact artifact) {
		PersistentAnalyzedArtifact persistent = new PersistentAnalyzedArtifact();
		persistent.artifact = PersistentArtifactCoordinates.from(artifact.coordinates());
		persistent.violations = transformToList(artifact.violations(), PersistentViolation::from);
		return persistent;
	}

	public AnalyzedArtifact toArtifact() {
		return new AnalyzedArtifact(
				artifact.toArtifact(),
				transformToImmutableSet(violations, PersistentViolation::toViolation)
		);
	}

}
