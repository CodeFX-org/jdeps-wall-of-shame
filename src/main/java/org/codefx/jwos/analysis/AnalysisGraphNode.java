package org.codefx.jwos.analysis;

import com.google.common.collect.ImmutableSet;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.IdentifiesArtifact;
import org.codefx.jwos.artifact.MarkInternalDependencies;
import org.codefx.jwos.jdeps.dependency.Violation;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

class AnalysisGraphNode implements IdentifiesArtifact {

	// Naming is hard: dependent -> dependee (so dependents are parents, dependees are children)

	private final ArtifactCoordinates artifact;
	private final Set<AnalysisGraphNode> dependents;

	private Optional<Path> jarFile;
	private Optional<ImmutableSet<Violation>> violations;
	private Optional<ImmutableSet<AnalysisGraphNode>> dependees;
	private Optional<MarkInternalDependencies> internalDependenciesMarker;

	public AnalysisGraphNode(IdentifiesArtifact artifact) {
		this.artifact = requireNonNull(artifact, "The argument 'artifact' must not be null.").coordinates();
		this.dependents = new HashSet<>();

		jarFile = Optional.empty();
		violations = Optional.empty();
		dependees = Optional.empty();
		internalDependenciesMarker = Optional.empty();
	}

	@Override
	public ArtifactCoordinates coordinates() {
		return artifact;
	}

	private void addAsDependent(AnalysisGraphNode dependent) {
		dependents.add(dependent);
	}

	public boolean wasDownloaded() {
		return jarFile.isPresent();
	}
	
	public void downloaded(Path jarFile) {
		requireNonNull(jarFile, "The argument 'jarFile' must not be null.");
		throwIfPresent(this.jarFile, "JAR for %s was already downloaded: %s.");
		this.jarFile = Optional.of(jarFile);
	}
	
	public Path jarFile() {
		throwIfPresent(this.jarFile, "JAR for %s was not yet downloaded.");
		return jarFile.get();
	}

	public boolean wasAnalyzed() {
		return violations.isPresent();
	}

	public void analyzed(ImmutableSet<Violation> violations) {
		requireNonNull(violations, "The argument 'violations' must not be null.");
		throwIfPresent(this.violations, "Violations for %s were already analyzed: %s.");
		this.violations = Optional.of(violations);
	}

	private ImmutableSet<Violation> violations() {
		throwIfNotPresent(this.violations, "Violations for %s were not yet analyzed.");
		return violations.get();
	}

	public boolean wasResolved() {
		return dependees.isPresent();
	}

	public void resolved(ImmutableSet<AnalysisGraphNode> dependees) {
		requireNonNull(dependees, "The argument 'dependees' must not be null.");
		throwIfPresent(this.dependees, "Dependencies for %s were already resolved: %s.");
		this.dependees = Optional.of(dependees);

		dependees.forEach(dependee -> dependee.addAsDependent(this));
	}

	private ImmutableSet<AnalysisGraphNode> dependees() {
		throwIfNotPresent(this.violations, "Dependencies of %s were not yet resolved.");
		return dependees.get();
	}

	private void throwIfPresent(Optional<?> optional, String exceptionMessageFormat) {
		optional.ifPresent(value -> {
			throw new IllegalStateException(format(exceptionMessageFormat, artifact, value));
		});
	}

	private void throwIfNotPresent(Optional<?> optional, String exceptionMessageFormat) {
		if (!optional.isPresent())
			throw new IllegalStateException(format(exceptionMessageFormat, artifact));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		AnalysisGraphNode that = (AnalysisGraphNode) o;
		return Objects.equals(artifact, that.artifact);
	}

	@Override
	public int hashCode() {
		return Objects.hash(artifact);
	}

	@Override
	public String toString() {
		return "Node: " + artifact;
	}
}
