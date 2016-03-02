package org.codefx.jwos.file.persistence;// NOT_PUBLISHED

import java.util.List;

public class PersistentAnalysis {

	public List<PersistentProjectCoordinates> projects;
	public List<PersistentResolvedProject> resolvedProjects;
	public List<PersistentFailedProject> resolutionFailedProjects;

	public List<PersistentAnalyzedArtifact> analyzedArtifacts;
	public List<PersistentFailedArtifact> analysisFailedArtifacts;
	public List<PersistentResolvedArtifact> resolvedArtifacts;
	public List<PersistentFailedArtifact> resolutionFailedArtifacts;

}
