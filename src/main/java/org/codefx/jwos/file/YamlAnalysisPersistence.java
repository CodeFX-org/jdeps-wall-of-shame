package org.codefx.jwos.file;// NOT_PUBLISHED

import org.codefx.jwos.analysis.AnalysisPersistence;
import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.FailedArtifact;
import org.codefx.jwos.artifact.FailedProject;
import org.codefx.jwos.artifact.IdentifiesArtifact;
import org.codefx.jwos.artifact.IdentifiesProject;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.codefx.jwos.artifact.ResolvedProject;
import org.codefx.jwos.file.persistence.PersistentAnalysis;
import org.codefx.jwos.file.persistence.PersistentAnalyzedArtifact;
import org.codefx.jwos.file.persistence.PersistentFailedArtifact;
import org.codefx.jwos.file.persistence.PersistentFailedProject;
import org.codefx.jwos.file.persistence.PersistentProjectCoordinates;
import org.codefx.jwos.file.persistence.PersistentResolvedArtifact;
import org.codefx.jwos.file.persistence.PersistentResolvedProject;

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

	private static final YamlPersister PERSISTER = new YamlPersister();

	private final SortedSet<ProjectCoordinates> projects
			= new ConcurrentSkipListSet<>(IdentifiesProject.alphabeticalOrder());
	private final SortedSet<ResolvedProject> resolvedProjects
			= new ConcurrentSkipListSet<>(IdentifiesProject.alphabeticalOrder());
	private final SortedSet<FailedProject> resolutionFailedProjects
			= new ConcurrentSkipListSet<>(IdentifiesProject.alphabeticalOrder());

	private final SortedSet<AnalyzedArtifact> analyzedArtifacts
			= new ConcurrentSkipListSet<>(IdentifiesArtifact.alphabeticalOrder());
	private final SortedSet<FailedArtifact> analysisFailedArtifacts
			= new ConcurrentSkipListSet<>(IdentifiesArtifact.alphabeticalOrder());
	private final SortedSet<ResolvedArtifact> resolvedArtifacts
			= new ConcurrentSkipListSet<>(IdentifiesArtifact.alphabeticalOrder());
	private final SortedSet<FailedArtifact> resolutionFailedArtifacts
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

	private static YamlAnalysisPersistence from(PersistentAnalysis persistent) {
		YamlAnalysisPersistence yaml = new YamlAnalysisPersistence();
		addTo(persistent.projects, PersistentProjectCoordinates::toProject, yaml.projects);
		addTo(persistent.resolvedProjects, PersistentResolvedProject::toProject, yaml.resolvedProjects);
		addTo(persistent.resolutionFailedProjects, PersistentFailedProject::toProject, yaml.resolutionFailedProjects);
		addTo(persistent.analyzedArtifacts, PersistentAnalyzedArtifact::toArtifact, yaml.analyzedArtifacts);
		addTo(persistent.analysisFailedArtifacts, PersistentFailedArtifact::toArtifact, yaml.analysisFailedArtifacts);
		addTo(persistent.resolvedArtifacts, PersistentResolvedArtifact::toArtifact, yaml.resolvedArtifacts);
		addTo(persistent.resolutionFailedArtifacts,
				PersistentFailedArtifact::toArtifact,
				yaml.resolutionFailedArtifacts);
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
		persistent.projects = transformToList(projects, PersistentProjectCoordinates::from);
		persistent.resolvedProjects = transformToList(resolvedProjects, PersistentResolvedProject::from);
		persistent.resolutionFailedProjects = transformToList(resolutionFailedProjects, PersistentFailedProject::from);
		persistent.analyzedArtifacts = transformToList(analyzedArtifacts, PersistentAnalyzedArtifact::from);
		persistent.analysisFailedArtifacts = transformToList(analysisFailedArtifacts, PersistentFailedArtifact::from);
		persistent.resolvedArtifacts = transformToList(resolvedArtifacts, PersistentResolvedArtifact::from);
		persistent.resolutionFailedArtifacts = transformToList(resolutionFailedArtifacts, PersistentFailedArtifact::from);
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
	public void addAnalyzedArtifact(AnalyzedArtifact artifact) {
		analyzedArtifacts.add(artifact);
	}

	@Override
	public void addAnalysisFailure(FailedArtifact artifact) {
		analysisFailedArtifacts.add(artifact);
	}

	@Override
	public void addResolvedArtifact(ResolvedArtifact artifact) {
		resolvedArtifacts.add(artifact);
	}

	@Override
	public void addArtifactResolutionFailure(FailedArtifact artifact) {
		resolutionFailedArtifacts.add(artifact);
	}
}
