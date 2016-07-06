package org.codefx.jwos.file.persistence;

import java.util.List;

public class PersistentAnalysis {

	// The "step_#_" prefixes are used because these steps are mostly ordered but they get ordered differently
	// (alphabetically according to the field name) in YAML

	public List<PersistentProjectCoordinates> step_1_projects;
	public List<PersistentResolvedProject> step_2_resolvedProjects;
	public List<PersistentFailedProject> step_2_resolutionFailedProjects;

	public List<PersistentDownloadedArtifact> step_3_downloadedArtifacts;
	public List<PersistentFailedArtifact> step_3_downloadFailedArtifacts;
	public List<PersistentAnalyzedArtifact> step_4_analyzedArtifacts;
	public List<PersistentFailedArtifact> step_4_analysisFailedArtifacts;
	public List<PersistentResolvedArtifact> step_5_resolvedArtifacts;
	public List<PersistentFailedArtifact> step_5_resolutionFailedArtifacts;

	public List<PersistentCompletedArtifact> step_6_completedArtifacts;

}
