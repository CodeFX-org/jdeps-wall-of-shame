package org.codefx.jwos.file.persistence;

import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.CompletedArtifact;
import org.codefx.jwos.artifact.CompletedArtifact.CompletedArtifactBuilder;
import org.codefx.jwos.artifact.MarkTransitiveInternalDependencies;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.codefx.jwos.Util.transformToImmutableSet;
import static org.codefx.jwos.Util.transformToList;

public class PersistentCompletedArtifact {

	private static final ConcurrentMap<ArtifactCoordinates, CompletedArtifact> COMPLETED_ARTIFACT_CACHE = new ConcurrentHashMap<>();

	public PersistentArtifactCoordinates artifact;
	public MarkTransitiveInternalDependencies analysisResult;
	public String analysisErrorMessage;
	public List<PersistentViolation> violations;
	public String resolutionErrorMessage;
	public List<PersistentCompletedArtifact> dependees;

	public static PersistentCompletedArtifact from(CompletedArtifact artifact) {
		PersistentCompletedArtifact persistent = new PersistentCompletedArtifact();
		persistent.artifact = PersistentArtifactCoordinates.from(artifact.coordinates());
		persistent.analysisResult = artifact.transitiveMarker();
		if (artifact.violations().isLeft())
			persistent.analysisErrorMessage = artifact.violations().getLeft().getMessage();
		else
			persistent.violations = transformToList(artifact.violations().get(), PersistentViolation::from);
		if (artifact.dependees().isLeft())
			persistent.resolutionErrorMessage = artifact.dependees().getLeft().getMessage();
		else
			persistent.dependees = transformToList(artifact.dependees().get(), PersistentCompletedArtifact::from);
		return persistent;
	}

	public CompletedArtifact toArtifact() {
		CompletedArtifactBuilder builder = CompletedArtifact.forArtifact(artifact.toArtifact());
		if (analysisErrorMessage == null)
			builder.withViolations(transformToImmutableSet(violations, PersistentViolation::toViolation));
		else
			builder.violationAnalysisFailedWith(new Exception(analysisErrorMessage));
		if (resolutionErrorMessage == null)
			builder.withDependees(transformToImmutableSet(dependees, PersistentCompletedArtifact::toArtifact));
		else
			builder.dependeeResolutionFailedWith(new Exception(resolutionErrorMessage));
		return builder.build();
	}

}
