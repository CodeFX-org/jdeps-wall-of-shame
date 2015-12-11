package org.codefx.jwos.analysis;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.SetMultimap;
import org.codefx.jwos.jdeps.dependency.Violation;
import org.codefx.jwos.maven.ArtifactCoordinates;
import org.codefx.jwos.maven.IdentifiesArtifact;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public class Analysis {

	// Naming is hard:
	// dependency: dependent -> dependee

	private final Set<ArtifactCoordinates> toAnalyze;
	private final Map<ArtifactCoordinates, AnalysisState> inAnalysis;
	private final Map<ArtifactCoordinates, DeeplyAnalyzedArtifact> analyzed;
	/**
	 * Maps from dependee to dependent, so when dependee is done, dependents can be checked as well
	 */
	private final SetMultimap<ArtifactCoordinates, ArtifactCoordinates> waitingDependentsByDependee;

	public Analysis() {
		toAnalyze = new HashSet<>();
		inAnalysis = new HashMap<>();
		analyzed = new HashMap<>();
		waitingDependentsByDependee = HashMultimap.create();
	}

	public void toAnalyse(Stream<ArtifactCoordinates> artifacts) {
		requireNonNull(artifacts, "The argument 'artifacts' must not be null.");
		artifacts
				.filter(artifact -> !inAnalysis.containsKey(artifact))
				.filter(artifact -> !analyzed.containsKey(artifact))
				.forEach(toAnalyze::add);
	}

	public ImmutableSet<ArtifactCoordinates> retrieveForAnalysis() {
		ImmutableSet<ArtifactCoordinates> toAnalyze = ImmutableSet.copyOf(this.toAnalyze);
		toAnalyze.forEach(this::startAnalysis);
		return toAnalyze;
	}

	private void startAnalysis(ArtifactCoordinates artifactToAnalyze) {
		toAnalyze.remove(artifactToAnalyze);
		inAnalysis.put(artifactToAnalyze, new AnalysisState());
	}

	public ImmutableSet<DeeplyAnalyzedArtifact> analyzed(AnalyzedArtifact analyzedArtifact) {
		requireNonNull(analyzedArtifact, "The argument 'analyzedArtifact' must not be null.");
		updateArtifactsState(analyzedArtifact);
		return deeplyAnalyzeArtifact(analyzedArtifact)
				.map(this::updateWaitingDependents)
				.orElse(ImmutableSet.of());
	}

	private void updateArtifactsState(AnalyzedArtifact analyzedArtifact) {
		AnalysisState state = inAnalysis.get(analyzedArtifact.artifact());
		if (state == null)
			throw new IllegalStateException();
		state.analysisDone(analyzedArtifact);
	}

	private Optional<DeeplyAnalyzedArtifact> deeplyAnalyzeArtifact(AnalyzedArtifact analyzedArtifact) {
		boolean dependeesAlreadyDeeplyAnalyzed = enqueueAnalysisForAllDependees(analyzedArtifact);

		if (dependeesAlreadyDeeplyAnalyzed) {
			DeeplyAnalyzedArtifact deeplyAnalyzedArtifact = createDeeplyAnalyzedArtifact(
					analyzedArtifact, analyzedArtifact.dependees(), analyzedArtifact.violations());
			analyzed.put(deeplyAnalyzedArtifact.artifact(), deeplyAnalyzedArtifact);
			return Optional.of(deeplyAnalyzedArtifact);
		} else
			return Optional.empty();
	}

	private boolean enqueueAnalysisForAllDependees(AnalyzedArtifact analyzedArtifact) {
		boolean allDependeesDeeplyAnalyzed = true;
		for (ArtifactCoordinates dependee : analyzedArtifact.dependees()) {
			boolean deeplyAnalyzed = analyzed.containsKey(dependee);
			if (!deeplyAnalyzed) {
				allDependeesDeeplyAnalyzed = false;
				boolean inAnalysis = this.inAnalysis.containsKey(dependee);
				if (inAnalysis)
					// store this dependency so that it can be looked up later
					waitingDependentsByDependee.put(dependee, analyzedArtifact.artifact());
				else
					// must be analyzed
					toAnalyze.add(dependee);
			}
		}
		return allDependeesDeeplyAnalyzed;
	}

	private DeeplyAnalyzedArtifact createDeeplyAnalyzedArtifact(
			IdentifiesArtifact analyzedArtifact,
			Iterable<? extends IdentifiesArtifact> dependees,
			ImmutableSet<Violation> violations) {
		Builder<DeeplyAnalyzedArtifact> analyzedDependees = ImmutableSet.builder();
		InternalDependencies internal = violations.isEmpty() ? InternalDependencies.NONE : InternalDependencies.DIRECT;
		for (IdentifiesArtifact dependee : dependees) {
			DeeplyAnalyzedArtifact analyzedDependee = analyzed.get(dependee.artifact());
			analyzedDependees.add(analyzedDependee);
			internal = internal.combineWithDependee(analyzedDependee.marker());
		}
		return new DeeplyAnalyzedArtifact(
				analyzedArtifact.artifact(),
				internal,
				violations,
				analyzedDependees.build());
	}

	private ImmutableSet<DeeplyAnalyzedArtifact> updateWaitingDependents(
			DeeplyAnalyzedArtifact deeplyAnalyzedArtifact) {
		ImmutableSet.Builder<DeeplyAnalyzedArtifact> deeplyAnalyzedArtifacts = ImmutableSet.builder();
		deeplyAnalyzedArtifacts.add(deeplyAnalyzedArtifact);

		updateWaitingDependentsRecursively(deeplyAnalyzedArtifact).forEach(deeplyAnalyzedArtifacts::add);

		return deeplyAnalyzedArtifacts.build();
	}

	private Stream<DeeplyAnalyzedArtifact> updateWaitingDependentsRecursively(
			DeeplyAnalyzedArtifact deeplyAnalyzedArtifact) {
		ArtifactCoordinates deeplyAnalyzed = deeplyAnalyzedArtifact.artifact();
		Set<ArtifactCoordinates> waitingDependents = stopWaitingForDependee(deeplyAnalyzed);
		return waitingDependents.stream()
				.peek(dependent -> inAnalysis.get(dependent).dependeeAnalysisDone(deeplyAnalyzed))
				.filter(dependent -> inAnalysis.get(dependent).areAllDependeesAnalyzed())
				.map(dependent -> {
					AnalysisState state = inAnalysis.get(dependent);
					return createDeeplyAnalyzedArtifact(
							dependent, state.analyzedDependees(), state.result().violations());
				})
				.flatMap(this::updateWaitingDependentsRecursively);
	}

	private Set<ArtifactCoordinates> stopWaitingForDependee(ArtifactCoordinates deeplyAnalyzed) {
		return waitingDependentsByDependee.removeAll(deeplyAnalyzed);
	}

	private static class AnalysisState {

		private Optional<AnalyzedArtifact> result;
		private final Set<ArtifactCoordinates> dependeesToAnalyse;
		private final Set<ArtifactCoordinates> analyzedDependees;

		private AnalysisState() {
			this.result = Optional.empty();
			this.dependeesToAnalyse = new HashSet<>();
			this.analyzedDependees = new HashSet<>();
		}

		public void analysisDone(AnalyzedArtifact result) {
			requireNonNull(result, "The argument 'result' must not be null.");
			this.result = Optional.of(result);
			this.dependeesToAnalyse.addAll(result.dependees());
		}

		public void dependeeAnalysisDone(ArtifactCoordinates dependee) {
			requireNonNull(dependee, "The argument 'dependee' must not be null.");
			if (!dependeesToAnalyse.contains(dependee))
				throw new IllegalStateException();
			dependeesToAnalyse.remove(dependee);
			analyzedDependees.add(dependee);
		}

		public boolean areAllDependeesAnalyzed() {
			return dependeesToAnalyse.isEmpty();
		}

		public AnalyzedArtifact result() {
			return result.orElseThrow(IllegalStateException::new);
		}

		public ImmutableSet<ArtifactCoordinates> analyzedDependees() {
			return ImmutableSet.copyOf(analyzedDependees);
		}

	}
}
