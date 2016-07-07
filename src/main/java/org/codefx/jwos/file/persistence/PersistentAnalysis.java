package org.codefx.jwos.file.persistence;

import java.util.ArrayList;
import java.util.List;

public class PersistentAnalysis {

	// The "step_#_" prefixes are used because these steps are mostly ordered but they get ordered differently
	// (alphabetically according to the field name) in YAML

	public List<PersistentProjectCoordinates> step_1_projects = new ArrayList<>();
	public List<PersistentResolvedProject> step_2_resolvedProjects = new ArrayList<>();
	public List<PersistentFailedProject> step_2_resolutionFailedProjects = new ArrayList<>();

	public List<PersistentDownloadedArtifact> step_3_downloadedArtifacts = new ArrayList<>();
	public List<PersistentFailedArtifact> step_3_downloadFailedArtifacts = new ArrayList<>();
	public List<PersistentAnalyzedArtifact> step_4_analyzedArtifacts = new ArrayList<>();
	public List<PersistentFailedArtifact> step_4_analysisFailedArtifacts = new ArrayList<>();
	public List<PersistentResolvedArtifact> step_5_resolvedArtifacts = new ArrayList<>();
	public List<PersistentFailedArtifact> step_5_resolutionFailedArtifacts = new ArrayList<>();

	public List<PersistentCompletedArtifact> step_6_completedArtifacts = new ArrayList<>();

}
