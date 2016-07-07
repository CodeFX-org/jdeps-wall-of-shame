package org.codefx.jwos.file.persistence;

import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.CompletedArtifact;
import org.codefx.jwos.artifact.CompletedArtifact.CompletedArtifactBuilder;
import org.codefx.jwos.artifact.MarkTransitiveInternalDependencies;
import org.codefx.jwos.jdeps.dependency.Violation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;
import static org.codefx.jwos.Util.transformToImmutableSet;
import static org.codefx.jwos.Util.transformToList;

public class PersistentCompletedArtifact {

	private static final ConcurrentMap<ArtifactCoordinates, CompletedArtifact> COMPLETED_ARTIFACT_CACHE = new ConcurrentHashMap<>();

	public PersistentArtifactCoordinates coordinates;
	public MarkTransitiveInternalDependencies analysisResult;

	public String analysisErrorMessage;
	public List<PersistentViolation> violations;

	public String resolutionErrorMessage;
	// Completed artifacts reference other completed artifacts. If persistent artifacts would do the same, the same
	// completed artifact could get persisted several times (because it could be references by several other artifacts),
	// leading to redundancy and a bloated file.
	public List<PersistentArtifactCoordinates> dependees;

	public static PersistentCompletedArtifact from(CompletedArtifact artifact) {
		PersistentCompletedArtifact persistent = new PersistentCompletedArtifact();
		persistent.coordinates = PersistentArtifactCoordinates.from(artifact.coordinates());
		persistent.analysisResult = artifact.transitiveMarker();
		if (artifact.violations().isLeft())
			persistent.analysisErrorMessage = artifact.violations().getLeft().getMessage();
		else
			persistent.violations = transformToList(artifact.violations().get(), PersistentViolation::from);
		if (artifact.dependees().isLeft())
			persistent.resolutionErrorMessage = artifact.dependees().getLeft().getMessage();
		else
			persistent.dependees = transformToList(
					artifact.dependees().get(),
					persistentArtifact -> PersistentArtifactCoordinates.from(persistentArtifact.coordinates()));
		return persistent;
	}

	public static Stream<CompletedArtifact> toArtifacts(Stream<PersistentCompletedArtifact> persistentArtifacts) {
		return new Loader().toArtifacts(persistentArtifacts);
	}

	private static class Loader {

		public Stream<CompletedArtifact> toArtifacts(Stream<PersistentCompletedArtifact> persistentArtifacts) {
			return finalizeArtifacts(createArtifacts(persistentArtifacts));
		}

		private Map<ArtifactCoordinates, MutableCompletedArtifact> createArtifacts(
				Stream<PersistentCompletedArtifact> persistentArtifacts) {
			return persistentArtifacts
					.map(MutableCompletedArtifact::new)
					.collect(toMap(MutableCompletedArtifact::coordinates, identity()));
		}

		private Stream<CompletedArtifact> finalizeArtifacts(
				Map<ArtifactCoordinates, MutableCompletedArtifact> artifacts) {
			return artifacts.values().stream()
					.map(artifact -> artifact.complete(artifacts::get));
		}

		private static class MutableCompletedArtifact {

			final ArtifactCoordinates coordinates;
			final String analysisErrorMessage;
			final List<Violation> violations;
			final String resolutionErrorMessage;
			final List<ArtifactCoordinates> dependees;

			CompletedArtifact completed;

			MutableCompletedArtifact(PersistentCompletedArtifact artifact) {
				this.coordinates = artifact.coordinates.toArtifact();
				this.analysisErrorMessage = artifact.analysisErrorMessage;
				this.violations = artifact.violations == null
						? new ArrayList<>()
						: artifact
						.violations.stream()
						.map(PersistentViolation::toViolation)
						.collect(toCollection(ArrayList::new));
				this.resolutionErrorMessage = artifact.resolutionErrorMessage;
				this.dependees = artifact.dependees == null
						? new ArrayList<>()
						: artifact
						.dependees.stream()
						.map(PersistentArtifactCoordinates::toArtifact)
						.collect(toCollection(ArrayList::new));
			}

			CompletedArtifact complete(Function<ArtifactCoordinates, MutableCompletedArtifact> findDependees) {
				if (completed == null)
					completed = completeRecursively(findDependees);

				return completed;
			}

			private CompletedArtifact completeRecursively(
					Function<ArtifactCoordinates, MutableCompletedArtifact> findDependees) {
				CompletedArtifactBuilder builder = CompletedArtifact.forArtifact(coordinates);
				if (analysisErrorMessage == null)
					builder.withViolations(transformToImmutableSet(violations, identity()));
				else
					builder.violationAnalysisFailedWith(new Exception(analysisErrorMessage));
				if (resolutionErrorMessage == null)
					builder.withDependees(transformToImmutableSet(
							dependees,
							// recursively complete dependees
							artifact -> findDependees.apply(artifact).complete(findDependees)));
				else
					builder.dependeeResolutionFailedWith(new Exception(resolutionErrorMessage));
				return builder.build();
			}

			public ArtifactCoordinates coordinates() {
				return coordinates;
			}
		}

	}

}
