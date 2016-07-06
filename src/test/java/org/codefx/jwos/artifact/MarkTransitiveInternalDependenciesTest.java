package org.codefx.jwos.artifact;

import org.junit.gen5.api.Test;
import org.junit.gen5.junit4.runner.JUnit5;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codefx.jwos.artifact.MarkTransitiveInternalDependencies.INDIRECT;
import static org.codefx.jwos.artifact.MarkTransitiveInternalDependencies.UNKNOWN;
import static org.codefx.jwos.artifact.MarkTransitiveInternalDependencies.NONE;
import static org.codefx.jwos.artifact.MarkTransitiveInternalDependencies.DIRECT;
import static org.codefx.jwos.artifact.MarkTransitiveInternalDependencies.fromDependees;

// NOT_PUBLISHED
@RunWith(JUnit5.class)
public class MarkTransitiveInternalDependenciesTest {

	// UNRESOLVED DEPENDEES

	@Test
	void fromDependees_artifactHasUnknownDependenciesAndUnresolvedDependees() {
		MarkTransitiveInternalDependencies marker = fromDependees(MarkInternalDependencies.UNKNOWN, unresolved());
		assertThat(marker).isSameAs(UNKNOWN);
	}

	@Test
	void fromDependees_artifactHasNoDependenciesAndUnresolvedDependees() {
		// if the marked artifact has no dependees its own marker must be unchanged
		MarkTransitiveInternalDependencies marker = fromDependees(MarkInternalDependencies.NONE, unresolved());
		assertThat(marker).isSameAs(UNKNOWN);
	}

	@Test
	void fromDependees_artifactHasDirectDependenciesAndUnresolvedDependees() {
		// if the marked artifact has no dependees its own marker must be unchanged
		MarkTransitiveInternalDependencies marker = fromDependees(MarkInternalDependencies.DIRECT, unresolved());
		assertThat(marker).isSameAs(DIRECT);
	}

	// NO DEPENDEES

	@Test
	void fromDependees_artifactHasUnknownDependenciesAndNoDependees() {
		// if the marked artifact has no dependees its own marker must be unchanged
		MarkTransitiveInternalDependencies marker = fromDependees(MarkInternalDependencies.UNKNOWN, none());
		assertThat(marker).isSameAs(UNKNOWN);
	}

	@Test
	void fromDependees_artifactHasNoDependenciesAndNoDependees() {
		// if the marked artifact has no dependees its own marker must be unchanged
		MarkTransitiveInternalDependencies marker = fromDependees(MarkInternalDependencies.NONE, none());
		assertThat(marker).isSameAs(NONE);
	}

	@Test
	void fromDependees_artifactHasDirectDependenciesAndNoDependees() {
		// if the marked artifact has no dependees its own marker must be unchanged
		MarkTransitiveInternalDependencies marker = fromDependees(MarkInternalDependencies.DIRECT, none());
		assertThat(marker).isSameAs(DIRECT);
	}

	// DEPENDEES

	// artifact has unknown dependencies

	@Test
	void fromDependees_artifactHasUnknownDependenciesAndDependeesWithoutInternalDependencies() {
		MarkTransitiveInternalDependencies marker = fromDependees(
				MarkInternalDependencies.UNKNOWN,
				on(UNKNOWN, NONE));
		assertThat(marker).isSameAs(UNKNOWN);
	}

	@Test
	void fromDependees_artifactHasUnknownDependenciesAndDependeesWithIndirectDependencies() {
		MarkTransitiveInternalDependencies marker = fromDependees(
				MarkInternalDependencies.UNKNOWN,
				on(UNKNOWN, INDIRECT, NONE));
		assertThat(marker).isSameAs(INDIRECT);
	}

	@Test
	void fromDependees_artifactHasUnknownDependenciesAndDependeesWithDirectDependencies() {
		MarkTransitiveInternalDependencies marker = fromDependees(
				MarkInternalDependencies.UNKNOWN,
				on(UNKNOWN, INDIRECT, DIRECT, NONE));
		assertThat(marker).isSameAs(INDIRECT);
	}

	// artifact has no dependencies

	@Test
	void fromDependees_artifactHasNoDependenciesAndDependeesWithUnknownDependencies() {
		MarkTransitiveInternalDependencies marker = fromDependees(
				MarkInternalDependencies.NONE,
				on(NONE, UNKNOWN));
		assertThat(marker).isSameAs(UNKNOWN);
	}

	@Test
	void fromDependees_artifactHasNoDependenciesAndDependeesWithNoDependencies() {
		MarkTransitiveInternalDependencies marker = fromDependees(
				MarkInternalDependencies.NONE,
				on(NONE));
		assertThat(marker).isSameAs(NONE);
	}

	@Test
	void fromDependees_artifactHasNoDependenciesAndDependeesWithIndirectDependencies() {
		MarkTransitiveInternalDependencies marker = fromDependees(
				MarkInternalDependencies.NONE,
				on(UNKNOWN, INDIRECT, NONE));
		assertThat(marker).isSameAs(INDIRECT);
	}

	@Test
	void fromDependees_artifactHasNoDependenciesAndDependeesWithDirectDependencies() {
		MarkTransitiveInternalDependencies marker = fromDependees(
				MarkInternalDependencies.NONE,
				on(UNKNOWN, INDIRECT, DIRECT, NONE));
		assertThat(marker).isSameAs(INDIRECT);
	}

	// artifact has dependencies

	@Test
	void fromDependees_artifactHasDirectDependenciesAndDependees() {
		// if the marked artifact has direct dependencies, the dependees do not matter for the final result
		MarkTransitiveInternalDependencies marker = fromDependees(MarkInternalDependencies.DIRECT, all());
		assertThat(marker).isSameAs(DIRECT);
	}

	/*
	 * UTILITIES
	 */

	private static Optional<Stream<MarkTransitiveInternalDependencies>> unresolved() {
		return Optional.empty();
	}

	private static Optional<Stream<MarkTransitiveInternalDependencies>> none() {
		return Optional.of(Stream.empty());
	}

	private static Optional<Stream<MarkTransitiveInternalDependencies>> on(
			MarkTransitiveInternalDependencies... dependencies) {
		return Optional.of(Arrays.stream(dependencies));
	}

	private static Optional<Stream<MarkTransitiveInternalDependencies>> all() {
		return on(MarkTransitiveInternalDependencies.values());
	}

}
