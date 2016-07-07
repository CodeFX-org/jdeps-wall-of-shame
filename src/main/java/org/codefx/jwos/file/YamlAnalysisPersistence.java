package org.codefx.jwos.file;

import org.codefx.jwos.analysis.AnalysisPersistence;
import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.CompletedArtifact;
import org.codefx.jwos.artifact.DownloadedArtifact;
import org.codefx.jwos.artifact.FailedArtifact;
import org.codefx.jwos.artifact.FailedProject;
import org.codefx.jwos.artifact.IdentifiesArtifact;
import org.codefx.jwos.artifact.IdentifiesProject;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.codefx.jwos.artifact.ResolvedProject;
import org.codefx.jwos.file.persistence.PersistentAnalysis;
import org.codefx.jwos.file.persistence.PersistentAnalyzedArtifact;
import org.codefx.jwos.file.persistence.PersistentCompletedArtifact;
import org.codefx.jwos.file.persistence.PersistentDownloadedArtifact;
import org.codefx.jwos.file.persistence.PersistentFailedArtifact;
import org.codefx.jwos.file.persistence.PersistentFailedProject;
import org.codefx.jwos.file.persistence.PersistentProjectCoordinates;
import org.codefx.jwos.file.persistence.PersistentResolvedArtifact;
import org.codefx.jwos.file.persistence.PersistentResolvedProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Collection;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;

import static java.util.Collections.unmodifiableSet;
import static org.codefx.jwos.Util.transformToList;

/**
 * An {@link AnalysisPersistence} that uses YAML to store results.
 * <p>
 * This implementation is not thread-safe.
 */
public class YamlAnalysisPersistence implements AnalysisPersistence {

	private static final Logger LOGGER = LoggerFactory.getLogger("Persistence");

	private static final YamlPersister PERSISTER = new YamlPersister();

	private final SortedSet<ProjectCoordinates> projects
			= new ConcurrentSkipListSet<>(IdentifiesProject.alphabeticalOrder());
	private final SortedSet<ResolvedProject> resolvedProjects
			= new ConcurrentSkipListSet<>(IdentifiesProject.alphabeticalOrder());
	private final SortedSet<FailedProject> resolutionFailedProjects
			= new ConcurrentSkipListSet<>(IdentifiesProject.alphabeticalOrder());

	private final SortedSet<DownloadedArtifact> downloadedArtifacts
			= new ConcurrentSkipListSet<>(IdentifiesArtifact.alphabeticalOrder());
	private final SortedSet<FailedArtifact> downloadFailedArtifacts
			= new ConcurrentSkipListSet<>(IdentifiesArtifact.alphabeticalOrder());
	private final SortedSet<AnalyzedArtifact> analyzedArtifacts
			= new ConcurrentSkipListSet<>(IdentifiesArtifact.alphabeticalOrder());
	private final SortedSet<FailedArtifact> analysisFailedArtifacts
			= new ConcurrentSkipListSet<>(IdentifiesArtifact.alphabeticalOrder());
	private final SortedSet<ResolvedArtifact> resolvedArtifacts
			= new ConcurrentSkipListSet<>(IdentifiesArtifact.alphabeticalOrder());
	private final SortedSet<FailedArtifact> resolutionFailedArtifacts
			= new ConcurrentSkipListSet<>(IdentifiesArtifact.alphabeticalOrder());
	private final SortedSet<CompletedArtifact> completedArtifacts
			= new ConcurrentSkipListSet<>(IdentifiesArtifact.alphabeticalOrder());

	// CREATION & PERSISTENCE

	private YamlAnalysisPersistence() {
		// private constructor to enforce use of static factory methods
	}

	public static YamlAnalysisPersistence empty() {
		return new YamlAnalysisPersistence();
	}

	public static YamlAnalysisPersistence fromString(String yamlString) {
		if (yamlString.isEmpty())
			return empty();

		PersistentAnalysis persistent = PERSISTER.read(yamlString, PersistentAnalysis.class);
		return from(persistent);
	}

	public static YamlAnalysisPersistence fromStream(InputStream yamlStream) {
		LOGGER.debug("Parsing result file...");
		PersistentAnalysis persistent = PERSISTER.read(yamlStream, PersistentAnalysis.class);
		if (persistent == null)
			return new YamlAnalysisPersistence();
		else
			return from(persistent);
	}

	private static YamlAnalysisPersistence from(PersistentAnalysis persistent) {
		YamlAnalysisPersistence yaml = new YamlAnalysisPersistence();
		addTo(persistent.step_1_projects, PersistentProjectCoordinates::toProject, yaml.projects);
		addTo(persistent.step_2_resolvedProjects, PersistentResolvedProject::toProject, yaml.resolvedProjects);
		addTo(persistent.step_2_resolutionFailedProjects, PersistentFailedProject::toProject, yaml.resolutionFailedProjects);
		addTo(persistent.step_3_downloadedArtifacts, PersistentDownloadedArtifact::toArtifact, yaml.downloadedArtifacts);
		addTo(persistent.step_3_downloadFailedArtifacts, PersistentFailedArtifact::toArtifact, yaml.downloadFailedArtifacts);
		addTo(persistent.step_4_analyzedArtifacts, PersistentAnalyzedArtifact::toArtifact, yaml.analyzedArtifacts);
		addTo(persistent.step_4_analysisFailedArtifacts, PersistentFailedArtifact::toArtifact, yaml.analysisFailedArtifacts);
		addTo(persistent.step_5_resolvedArtifacts, PersistentResolvedArtifact::toArtifact, yaml.resolvedArtifacts);
		addTo(persistent.step_5_resolutionFailedArtifacts, PersistentFailedArtifact::toArtifact, yaml.resolutionFailedArtifacts);
		PersistentCompletedArtifact
				.toArtifacts(persistent.step_6_completedArtifacts.stream())
				.forEach(yaml.completedArtifacts::add);

		return yaml;
	}

	private static <P, T> void addTo(Collection<P> source, Function<P, T> transform, Collection<T> target) {
		source.stream()
				.map(transform)
				.forEach(target::add);
	}

	public String toYaml() {
		PersistentAnalysis persistent = toPersistentAnalysis();
		return PERSISTER.write(persistent);
	}

	private PersistentAnalysis toPersistentAnalysis() {
		PersistentAnalysis persistent = new PersistentAnalysis();
		persistent.step_1_projects = transformToList(projects, PersistentProjectCoordinates::from);
		persistent.step_2_resolvedProjects = transformToList(resolvedProjects, PersistentResolvedProject::from);
		persistent.step_2_resolutionFailedProjects = transformToList(resolutionFailedProjects, PersistentFailedProject::from);
		persistent.step_3_downloadedArtifacts = transformToList(downloadedArtifacts, PersistentDownloadedArtifact::from);
		persistent.step_3_downloadFailedArtifacts = transformToList(downloadFailedArtifacts, PersistentFailedArtifact::from);
		persistent.step_4_analyzedArtifacts = transformToList(analyzedArtifacts, PersistentAnalyzedArtifact::from);
		persistent.step_4_analysisFailedArtifacts = transformToList(analysisFailedArtifacts, PersistentFailedArtifact::from);
		persistent.step_5_resolvedArtifacts = transformToList(resolvedArtifacts, PersistentResolvedArtifact::from);
		persistent.step_5_resolutionFailedArtifacts = transformToList(resolutionFailedArtifacts, PersistentFailedArtifact::from);
		persistent.step_6_completedArtifacts = transformToList(completedArtifacts, PersistentCompletedArtifact::from);
		return persistent;
	}

	// IMPLEMENTATION OF 'AnalysisPersistence'

	@Override
	public Collection<ProjectCoordinates> projectsUnmodifiable() {
		return unmodifiableSet(projects);
	}

	@Override
	public Collection<ResolvedProject> resolvedProjectsUnmodifiable() {
		return unmodifiableSet(resolvedProjects);
	}

	@Override
	public Collection<FailedProject> projectResolutionErrorsUnmodifiable() {
		return unmodifiableSet(resolutionFailedProjects);
	}

	@Override
	public Collection<DownloadedArtifact> downloadedArtifactsUnmodifiable() {
		return unmodifiableSet(downloadedArtifacts);
	}

	@Override
	public Collection<FailedArtifact> artifactDownloadErrorsUnmodifiable() {
		return unmodifiableSet(downloadFailedArtifacts);
	}

	@Override
	public Collection<AnalyzedArtifact> analyzedArtifactsUnmodifiable() {
		return unmodifiableSet(analyzedArtifacts);
	}

	@Override
	public Collection<FailedArtifact> artifactAnalysisErrorsUnmodifiable() {
		return unmodifiableSet(analysisFailedArtifacts);
	}

	@Override
	public Collection<ResolvedArtifact> resolvedArtifactsUnmodifiable() {
		return unmodifiableSet(resolvedArtifacts);
	}

	@Override
	public Collection<FailedArtifact> artifactResolutionErrorsUnmodifiable() {
		return unmodifiableSet(resolutionFailedArtifacts);
	}

	@Override
	public void addProject(ProjectCoordinates project) {
		projects.add(project);
	}

	@Override
	public void addResolvedProject(ResolvedProject project) {
		resolvedProjects.add(project);
	}

	@Override
	public void addProjectResolutionError(FailedProject project) {
		resolutionFailedProjects.add(project);
	}

	@Override
	public void addDownloadedArtifact(DownloadedArtifact artifact) {
		downloadedArtifacts.add(artifact);
	}

	@Override
	public void addDownloadError(FailedArtifact artifact) {
		downloadFailedArtifacts.add(artifact);
	}

	@Override
	public void addAnalyzedArtifact(AnalyzedArtifact artifact) {
		analyzedArtifacts.add(artifact);
	}

	@Override
	public void addAnalysisError(FailedArtifact artifact) {
		analysisFailedArtifacts.add(artifact);
	}

	@Override
	public void addResolvedArtifact(ResolvedArtifact artifact) {
		resolvedArtifacts.add(artifact);
	}

	@Override
	public void addArtifactResolutionError(FailedArtifact artifact) {
		resolutionFailedArtifacts.add(artifact);
	}

	@Override
	public void addResult(CompletedArtifact artifact) {
		completedArtifacts.add(artifact);
	}

}
