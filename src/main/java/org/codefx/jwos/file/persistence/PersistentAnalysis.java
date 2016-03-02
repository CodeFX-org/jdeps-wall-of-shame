package org.codefx.jwos.file.persistence;// NOT_PUBLISHED

import java.util.List;

public class PersistentAnalysis {

	// The "step_#_" prefixes are used because these steps are mostly ordered but they get ordered differently
	// (alphabetically according to the field name) in YAML

	public List<PersistentProjectCoordinates> step_1_projects;
	public List<PersistentResolvedProject> step_2_resolvedProjects;
	public List<PersistentFailedProject> step_2_resolutionFailedProjects;

	public List<PersistentAnalyzedArtifact> step_3_analyzedArtifacts;
	public List<PersistentFailedArtifact> step_3_analysisFailedArtifacts;
	public List<PersistentResolvedArtifact> step_4_resolvedArtifacts;
	public List<PersistentFailedArtifact> step_4_resolutionFailedArtifacts;

	public List<PersistentDeeplyAnalyzedArtifact> step_5_deeplyAnalyzedArtifacts;

}
