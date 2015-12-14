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
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

class Analysis {

	// Naming is hard:
	// dependency: dependent -> dependee

	private final AnalysisStore store;

	public Analysis() {
		store = new AnalysisStore();
	}

	public void toAnalyse(ArtifactCoordinates artifact) {
		requireNonNull(artifact, "The argument 'artifact' must not be null.");
		if (store.isNotToAnalyze(artifact) && store.isNotInAnalysis(artifact) && store.isNotAnalyzed(artifact))
			store.addToAnalyze(artifact);
	}

	public ImmutableSet<ArtifactCoordinates> retrieveForAnalysis() {
		ImmutableSet<ArtifactCoordinates> toAnalyze = store.getAllToAnalyze();
		toAnalyze.forEach(this::startAnalysis);
		return toAnalyze;
	}

	private void startAnalysis(ArtifactCoordinates artifactToAnalyze) {
		store.removeToAnalyze(artifactToAnalyze);
		store.addInAnalysis(artifactToAnalyze);
	}

	public ImmutableSet<DeeplyAnalyzedArtifact> analyzed(AnalyzedArtifact analyzedArtifact) {
		requireNonNull(analyzedArtifact, "The argument 'analyzedArtifact' must not be null.");
		updateArtifactsState(analyzedArtifact);
		return deeplyAnalyzeArtifact(analyzedArtifact)
				.map(this::findFinishedDependents)
				.orElse(ImmutableSet.of());
	}

	private void updateArtifactsState(AnalyzedArtifact analyzedArtifact) {
		AnalysisState state = store.getStateInAnalysis(analyzedArtifact.artifact());
		if (state == null)
			throw new IllegalStateException();
		state.analysisDone(analyzedArtifact);
	}

	private Optional<DeeplyAnalyzedArtifact> deeplyAnalyzeArtifact(AnalyzedArtifact analyzedArtifact) {
		boolean dependeesAlreadyDeeplyAnalyzed = enqueueAnalysisForAllDependees(analyzedArtifact);

		if (dependeesAlreadyDeeplyAnalyzed) {
			DeeplyAnalyzedArtifact deeplyAnalyzedArtifact = createDeeplyAnalyzedArtifact(
					analyzedArtifact, analyzedArtifact.dependees(), analyzedArtifact.violations());
			markArtifactAsDeeplyAnalyzed(deeplyAnalyzedArtifact);
			return Optional.of(deeplyAnalyzedArtifact);
		} else
			return Optional.empty();
	}

	private boolean enqueueAnalysisForAllDependees(AnalyzedArtifact analyzedArtifact) {
		boolean allDependeesDeeplyAnalyzed = true;
		for (ArtifactCoordinates dependee : analyzedArtifact.dependees()) {
			if (store.isNotAnalyzed(dependee)) {
				allDependeesDeeplyAnalyzed = false;
				store.waitsFor(analyzedArtifact.artifact(), dependee);
				if (store.isNotInAnalysis(dependee) && store.isNotToAnalyze(dependee))
					// must be analyzed
					store.addToAnalyze(dependee);
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
			DeeplyAnalyzedArtifact analyzedDependee = store.getAnalysisResult(dependee.artifact());
			analyzedDependees.add(analyzedDependee);
			internal = internal.combineWithDependee(analyzedDependee.marker());
		}
		return new DeeplyAnalyzedArtifact(
				analyzedArtifact.artifact(),
				internal,
				violations,
				analyzedDependees.build());
	}

	private void markArtifactAsDeeplyAnalyzed(DeeplyAnalyzedArtifact deeplyAnalyzedArtifact) {
		store.removeInAnalysis(deeplyAnalyzedArtifact.artifact());
		store.addAnalyzed(deeplyAnalyzedArtifact);
	}

	private ImmutableSet<DeeplyAnalyzedArtifact> findFinishedDependents(
			DeeplyAnalyzedArtifact deeplyAnalyzedArtifact) {
		ImmutableSet.Builder<DeeplyAnalyzedArtifact> deeplyAnalyzedArtifacts = ImmutableSet.builder();
		findFinishedDependentsRecursively(deeplyAnalyzedArtifact).forEach(deeplyAnalyzedArtifacts::add);
		return deeplyAnalyzedArtifacts.build();
	}

	private Stream<DeeplyAnalyzedArtifact> findFinishedDependentsRecursively(
			DeeplyAnalyzedArtifact deeplyAnalyzedArtifact) {
		ArtifactCoordinates deeplyAnalyzed = deeplyAnalyzedArtifact.artifact();
		Set<ArtifactCoordinates> waitingDependents = store.stopWaitingFor(deeplyAnalyzed);
		return concat(
				// since the recursion uses flat map, the "root" for this call has to be present in the returned stream;
				// otherwise finished dependencies get flat mapped to empty streams and are lost
				of(deeplyAnalyzedArtifact),
				waitingDependents.stream()
						.peek(dependent -> store.getStateInAnalysis(dependent).dependeeAnalysisDone(deeplyAnalyzed))
						.filter(dependent -> store.getStateInAnalysis(dependent).areAllDependeesAnalyzed())
						.map(dependent -> {
							AnalysisState state = store.getStateInAnalysis(dependent);
							return createDeeplyAnalyzedArtifact(
									dependent, state.analyzedDependees(), state.result().violations());
						})
						.peek(this::markArtifactAsDeeplyAnalyzed)
						.flatMap(this::findFinishedDependentsRecursively));
	}

	private static class AnalysisStore {

		// Remember:
		// dependency: dependent -> dependee

		private final Set<ArtifactCoordinates> toAnalyze;
		private final Map<ArtifactCoordinates, AnalysisState> inAnalysis;
		private final Map<ArtifactCoordinates, DeeplyAnalyzedArtifact> analyzed;
		/**
		 * Maps from dependee to dependent, so when dependee is done, dependents can be checked as well
		 */
		private final SetMultimap<ArtifactCoordinates, ArtifactCoordinates> waitingDependentsByDependee;

		public AnalysisStore() {
			toAnalyze = new HashSet<>();
			inAnalysis = new HashMap<>();
			analyzed = new HashMap<>();
			waitingDependentsByDependee = HashMultimap.create();
		}

		// to analyze

		public void addToAnalyze(ArtifactCoordinates artifact) {
			toAnalyze.add(artifact);
		}

		public void removeToAnalyze(ArtifactCoordinates artifact) {
			toAnalyze.remove(artifact);
		}

		public ImmutableSet<ArtifactCoordinates> getAllToAnalyze() {
			return ImmutableSet.copyOf(this.toAnalyze);
		}

		public boolean isNotToAnalyze(ArtifactCoordinates artifact) {
			return !this.inAnalysis.containsKey(artifact);
		}

		// in analysis

		public void addInAnalysis(ArtifactCoordinates artifact) {
			inAnalysis.put(artifact, new AnalysisState());
		}

		public void removeInAnalysis(ArtifactCoordinates artifact) {
			inAnalysis.remove(artifact);
		}

		public AnalysisState getStateInAnalysis(ArtifactCoordinates artifact) {
			return inAnalysis.get(artifact);
		}

		public boolean isNotInAnalysis(ArtifactCoordinates artifact) {
			return !this.inAnalysis.containsKey(artifact);
		}

		// analyzed

		public void addAnalyzed(DeeplyAnalyzedArtifact artifact) {
			analyzed.put(artifact.artifact(), artifact);
		}

		public DeeplyAnalyzedArtifact getAnalysisResult(ArtifactCoordinates artifact) {
			return analyzed.get(artifact);
		}

		public boolean isNotAnalyzed(ArtifactCoordinates artifact) {
			return !analyzed.containsKey(artifact);
		}

		// waiting

		public void waitsFor(ArtifactCoordinates dependent, ArtifactCoordinates dependee) {
			waitingDependentsByDependee.put(dependee, dependent);
		}

		public ImmutableSet<ArtifactCoordinates> stopWaitingFor(ArtifactCoordinates dependee) {
			return ImmutableSet.copyOf(waitingDependentsByDependee.removeAll(dependee));
		}

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
