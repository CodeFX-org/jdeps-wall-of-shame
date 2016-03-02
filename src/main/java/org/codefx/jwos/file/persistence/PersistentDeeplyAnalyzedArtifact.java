package org.codefx.jwos.file.persistence;

import org.codefx.jwos.artifact.DeeplyAnalyzedArtifact;
import org.codefx.jwos.artifact.MarkInternalDependencies;

import java.util.List;

import static org.codefx.jwos.Util.transformToImmutableSet;
import static org.codefx.jwos.Util.transformToList;

public class PersistentDeeplyAnalyzedArtifact {

	public PersistentArtifactCoordinates artifact;
	public MarkInternalDependencies analysisResult;
	public List<PersistentViolation> violations;
	public List<PersistentDeeplyAnalyzedArtifact> dependees;

	public static PersistentDeeplyAnalyzedArtifact from(DeeplyAnalyzedArtifact artifact) {
		PersistentDeeplyAnalyzedArtifact persistent = new PersistentDeeplyAnalyzedArtifact();
		persistent.artifact = PersistentArtifactCoordinates.from(artifact.coordinates());
		persistent.analysisResult = artifact.marker();
		persistent.violations = transformToList(artifact.violations(), PersistentViolation::from);
		persistent.dependees = transformToList(artifact.dependees(), PersistentDeeplyAnalyzedArtifact::from);
		return persistent;
	}

	public DeeplyAnalyzedArtifact toArtifact() {
		return new DeeplyAnalyzedArtifact(
				artifact.toArtifact(),
				analysisResult,
				transformToImmutableSet(violations, PersistentViolation::toViolation),
				transformToImmutableSet(dependees, PersistentDeeplyAnalyzedArtifact::toArtifact)
		);
	}

}
