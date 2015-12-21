package org.codefx.jwos.analysis;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.SetMultimap;
import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.DeeplyAnalyzedArtifact;
import org.codefx.jwos.artifact.IdentifiesArtifact;
import org.codefx.jwos.artifact.InternalDependencies;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.codefx.jwos.jdeps.dependency.Violation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

public class Analysis {

	// Naming is hard:
	// dependency: dependent -> dependee

	private final ArtifactStore store;

	public Analysis(Collection<DeeplyAnalyzedArtifact> formerlyAnalyzedArtifacts) {
		store = new ArtifactStore(formerlyAnalyzedArtifacts);
	}

	/**
	 * Informs this analysis that the specified artifacts needs to be analyzed.
	 *
	 * @param artifact
	 * 		the artifact to analyze
	 *
	 * @return whether the artifact needs to be analyzed (if not, it was already analyzed);
	 * if true, it is assumed that the artifact is queued for analysis
	 */
	public synchronized boolean startAnalysis(ArtifactCoordinates artifact) {
		requireNonNull(artifact, "The argument 'artifact' must not be null.");
		if (!store.isInAnalysis(artifact) && !store.isDeeplyAnalyzed(artifact)) {
			store.addInAnalysis(artifact);
			return true;
		} else
			return false;
	}

	/**
	 * Informs this analysis of the specified artifact's dependees.
	 */
	public synchronized void resolved(ResolvedArtifact resolvedArtifact) {
		requireNonNull(resolvedArtifact, "The argument 'resolvedArtifact' must not be null.");
		store.getStateInAnalysis(resolvedArtifact).resolved(resolvedArtifact);
		markDependeesAsAnalyzedOrWaitedFor(resolvedArtifact);
	}

	private void markDependeesAsAnalyzedOrWaitedFor(ResolvedArtifact resolvedArtifact) {
		resolvedArtifact
				.dependees().stream()
				.forEach(dependee -> {
					if (store.isDeeplyAnalyzed(dependee))
						store.getStateInAnalysis(resolvedArtifact).dependeeAnalyzed(dependee);
					else
						store.waitsFor(resolvedArtifact.artifact(), dependee);
				});
	}

	/**
	 * Informs this analysis that the specified artifact was analyzed.
	 *
	 * @return the set of artifacts, which have been deeply analyzed (i.e. with all their transitive dependencies), by
	 * finishing the specified artifact.
	 */
	public synchronized ImmutableSet<DeeplyAnalyzedArtifact> analyzed(AnalyzedArtifact analyzedArtifact) {
		requireNonNull(analyzedArtifact, "The argument 'analyzedArtifact' must not be null.");
		store.getStateInAnalysis(analyzedArtifact).analyzed(analyzedArtifact);
		return deeplyAnalyzeArtifact(analyzedArtifact)
				.map(this::findFinishedDependents)
				.orElse(ImmutableSet.of());
	}

	private Optional<DeeplyAnalyzedArtifact> deeplyAnalyzeArtifact(AnalyzedArtifact analyzedArtifact) {
		boolean areAllDependeesAnalyzed = store.getStateInAnalysis(analyzedArtifact).areAllDependeesAnalyzed();
		if (areAllDependeesAnalyzed) {
			DeeplyAnalyzedArtifact deeplyAnalyzedArtifact =
					createDeeplyAnalyzedArtifact(analyzedArtifact, analyzedArtifact.violations());
			markArtifactAsDeeplyAnalyzed(deeplyAnalyzedArtifact);
			return Optional.of(deeplyAnalyzedArtifact);
		} else
			return Optional.empty();
	}

	private DeeplyAnalyzedArtifact createDeeplyAnalyzedArtifact(
			IdentifiesArtifact analyzedArtifact,
			ImmutableSet<Violation> violations) {
		Builder<DeeplyAnalyzedArtifact> analyzedDependees = ImmutableSet.builder();
		InternalDependencies internal = violations.isEmpty() ? InternalDependencies.NONE : InternalDependencies.DIRECT;
		for (IdentifiesArtifact dependee : store.getStateInAnalysis(analyzedArtifact).analyzedDependees()) {
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
		store.addDeeplyAnalyzed(deeplyAnalyzedArtifact);
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
				// since the recursion uses flat map, the "root" for this call has to be present in the returned
				// stream; otherwise finished dependencies get flat mapped to empty streams and are lost
				of(deeplyAnalyzedArtifact),
				waitingDependents.stream()
						// update the dependent's analysis state and check whether all dependees are analyzed
						.peek(dependent -> store.getStateInAnalysis(dependent).dependeeAnalyzed(deeplyAnalyzed))
						.filter(dependent -> store.getStateInAnalysis(dependent).areAllDependeesAnalyzed())
						// create and mark a new deeply analyzed artifact
						.map(dependent -> createDeeplyAnalyzedArtifact(
								dependent, store.getStateInAnalysis(dependent).result().violations()))
						.peek(this::markArtifactAsDeeplyAnalyzed)
						// recurse to find the artifacts that depend on the newly finished artifacts
						.flatMap(this::findFinishedDependentsRecursively));
	}

	// TODO provide a method to identify and remove circles

	private static class ArtifactStore {

		// Remember:
		// dependency: dependent -> dependee

		private final Map<ArtifactCoordinates, AnalysisState> inAnalysis;
		private final Map<ArtifactCoordinates, DeeplyAnalyzedArtifact> deeplyAnalyzed;
		/**
		 * Maps from dependee to dependent, so when dependee is done, dependents can be checked as well
		 */
		private final SetMultimap<ArtifactCoordinates, ArtifactCoordinates> waitingDependentsByDependee;

		public ArtifactStore(Collection<DeeplyAnalyzedArtifact> formerlyAnalyzedArtifacts) {
			inAnalysis = new HashMap<>();
			deeplyAnalyzed =
					formerlyAnalyzedArtifacts.stream().collect(toMap(DeeplyAnalyzedArtifact::artifact, identity()));
			waitingDependentsByDependee = HashMultimap.create();
		}

		// all

		// in analysis

		public void addInAnalysis(ArtifactCoordinates artifact) {
			inAnalysis.put(artifact, new AnalysisState());
		}

		public void removeInAnalysis(ArtifactCoordinates artifact) {
			inAnalysis.remove(artifact);
		}

		public AnalysisState getStateInAnalysis(IdentifiesArtifact artifact) {
			return Optional.of(artifact)
					.map(IdentifiesArtifact::artifact)
					.map(inAnalysis::get)
					.orElseThrow(() ->
							new IllegalStateException(format("No analysis state for artifact %s.", artifact)));
		}

		public boolean isInAnalysis(ArtifactCoordinates artifact) {
			return this.inAnalysis.containsKey(artifact);
		}

		// deeplyAnalyzed

		public void addDeeplyAnalyzed(DeeplyAnalyzedArtifact artifact) {
			deeplyAnalyzed.put(artifact.artifact(), artifact);
		}

		public DeeplyAnalyzedArtifact getAnalysisResult(ArtifactCoordinates artifact) {
			return deeplyAnalyzed.get(artifact);
		}

		public boolean isDeeplyAnalyzed(ArtifactCoordinates artifact) {
			return deeplyAnalyzed.containsKey(artifact);
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

		public void resolved(ResolvedArtifact result) {
			requireNonNull(result, "The argument 'result' must not be null.");
			this.dependeesToAnalyse.addAll(result.dependees());
		}

		public void analyzed(AnalyzedArtifact result) {
			requireNonNull(result, "The argument 'result' must not be null.");
			this.result = Optional.of(result);
		}

		public void dependeeAnalyzed(ArtifactCoordinates dependee) {
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
