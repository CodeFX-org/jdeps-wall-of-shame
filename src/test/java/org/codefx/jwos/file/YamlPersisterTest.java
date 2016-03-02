package org.codefx.jwos.file;// NOT_PUBLISHED

import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.FailedArtifact;
import org.codefx.jwos.artifact.FailedProject;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.codefx.jwos.artifact.ResolvedProject;
import org.codefx.jwos.jdeps.dependency.InternalType;
import org.codefx.jwos.jdeps.dependency.Type;
import org.codefx.jwos.jdeps.dependency.Violation;
import org.junit.gen5.api.BeforeEach;
import org.junit.gen5.api.DisplayName;
import org.junit.gen5.api.Test;

import static com.google.common.collect.ImmutableSet.of;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("YAML persister")
class YamlPersisterTest {

	private YamlPersister persister;

	@BeforeEach
	void createPersister() {
		persister = new YamlPersister();
	}

	// PROJECTS

	@Test
	@DisplayName("can dump and load projects")
	void persistProject() {
		ProjectCoordinates project = ProjectCoordinates.from("org.group", "theProject");

		String projectAsYaml = persister.writeProject(project);
		ProjectCoordinates loadedProject = persister.readProject(projectAsYaml);

		assertThat(loadedProject).isEqualTo(project);
	}

	@Test
	@DisplayName("can dump and load failed projects")
	void persistFailedProject() {
		FailedProject project = new FailedProject(
				ProjectCoordinates.from("org.group", "theProject"),
				new Exception("error message")
		);

		String projectAsYaml = persister.writeFailedProject(project);
		FailedProject loadedProject = persister.readFailedProject(projectAsYaml);
		
		assertThat(loadedProject).isEqualTo(project);
	}

	@Test
	@DisplayName("can dump and load resolved projects")
	void persistResolvedProject() {
		ResolvedProject project = new ResolvedProject(
				ProjectCoordinates.from("project", "artifact"),
				of(
						ArtifactCoordinates.from("project", "artifact", "v1"),
						ArtifactCoordinates.from("project", "artifact", "v2"))
		);

		String artifactAsYaml = persister.writeResolvedProject(project);
		ResolvedProject loadedProject = persister.readResolvedProject(artifactAsYaml);
		
		assertThat(loadedProject).isEqualTo(project);
	}

	// ARTIFACTS

	@Test
	@DisplayName("can dump and load artifacts")
	void persistArtifact() {
		ArtifactCoordinates artifact = ArtifactCoordinates.from("org.group", "theArtifact", "v1.Foo");

		String artifactAsYaml = persister.writeArtifact(artifact);
		ArtifactCoordinates loadedArtifact = persister.readArtifact(artifactAsYaml);
		
		assertThat(loadedArtifact).isEqualTo(artifact);
	}

	@Test
	@DisplayName("can dump and load failed artifacts")
	void persistFailedArtifact() {
		FailedArtifact artifact = new FailedArtifact(
				ArtifactCoordinates.from("org.group", "theArtifact", "v1.Foo"),
				new Exception("error message")
		);

		String artifactAsYaml = persister.writeFailedArtifact(artifact);
		FailedArtifact loadedArtifact = persister.readFailedArtifact(artifactAsYaml);
		
		assertThat(loadedArtifact).isEqualTo(artifact);
	}

	@Test
	@DisplayName("can dump and load resolved artifacts")
	void persistResolvedArtifact() {
		ResolvedArtifact artifact = new ResolvedArtifact(
				ArtifactCoordinates.from("artifact.group", "artifact", "version"),
				of(
						ArtifactCoordinates.from("dependee1.group", "dep", "v1"),
						ArtifactCoordinates.from("dependee2.group", "dependee", "v2"))
		);

		String artifactAsYaml = persister.writeResolvedArtifact(artifact);
		ResolvedArtifact loadedArtifact = persister.readResolvedArtifact(artifactAsYaml);
		
		assertThat(loadedArtifact).isEqualTo(artifact);
	}

	@Test
	@DisplayName("can dump and load analyzed artifacts")
	void persistAnalyzedArtifact() {
		AnalyzedArtifact artifact = new AnalyzedArtifact(
				ArtifactCoordinates.from("artifact.group", "artifact", "version"),
				of(
						Violation.buildFor(
								Type.of("artifact.package", "Class"),
								of(
										InternalType.of("sun.misc", "Unsafe", "internal", "JDK-internal"),
										InternalType.of("sun.misc", "BASE64Encoder", "internal", "JDK-internal"))),
						Violation.buildFor(
								Type.of("artifact.package", "Class"),
								of(
										InternalType.of("sun.misc", "Unsafe", "internal", "JDK-internal"),
										InternalType.of("sun.misc", "BASE64Encoder", "internal", "JDK-internal"))))
		);

		String artifactAsYaml = persister.writeAnalyzedArtifact(artifact);
		AnalyzedArtifact loadedArtifact = persister.readAnalyzedArtifact(artifactAsYaml);
		
		assertThat(loadedArtifact).isEqualTo(artifact);
	}

}
