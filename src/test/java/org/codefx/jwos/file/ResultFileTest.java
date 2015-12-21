package org.codefx.jwos.file;

import com.google.common.collect.ImmutableSet;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.DeeplyAnalyzedArtifact;
import org.codefx.jwos.artifact.MarkInternalDependencies;
import org.codefx.jwos.jdeps.dependency.InternalType;
import org.codefx.jwos.jdeps.dependency.Type;
import org.codefx.jwos.jdeps.dependency.Violation;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class ResultFileTest {

	private static final String NEW_LINE = System.getProperty("line.separator");

	private Path resultFile;

	@Before
	public void createTempFile() throws IOException {
		resultFile = File.createTempFile("jwos-result-file", ".tmp").toPath();
		Files.delete(resultFile);
	}

	@Test
	public void write_emptyResult_emptyFile() throws Exception {
		ResultFile result = ResultFile.empty(resultFile);

		result.write();

		List<String> lines = Files.lines(resultFile).collect(toList());
		assertThat(lines).isEmpty();
	}

	@Test
	public void write_resultWithArtifact_fileContainsArtifact() throws Exception {
		ResultFile result = ResultFile.empty(resultFile);
		DeeplyAnalyzedArtifact artifact = new DeeplyAnalyzedArtifact(
				ArtifactCoordinates.from("group.id", "art.id", "1.0"),
				MarkInternalDependencies.DIRECT,
				ImmutableSet.of(
						Violation.buildFor(
								Type.of("com.foo", "Bar"),
								singleton(InternalType.of("sun.misc.Unsafe")))),
				ImmutableSet.of(
						simpleArtifact(ArtifactCoordinates.from("gang.id", "garbage.id", "2.0")))
		);
		result.addArtifacts(artifact);

		result.write();

		List<String> lines = Files.lines(resultFile).collect(toList());
		assertThat(lines).containsExactly(
				"group.id:art.id:1.0 <> DIRECT",
				"\tv: com.foo.Bar -> sun.misc.Unsafe",
				"\td: gang.id:garbage.id:2.0 <> NONE"
		);
	}

	@Test
	public void write_resultWithArtifacts_fileContainsArtifacts() throws Exception {
		ResultFile result = ResultFile.empty(resultFile);
		DeeplyAnalyzedArtifact one = new DeeplyAnalyzedArtifact(
				ArtifactCoordinates.from("group.id", "art.id", "1.0"),
				MarkInternalDependencies.DIRECT,
				ImmutableSet.of(
						Violation.buildFor(
								Type.of("com.foo", "Bar"),
								singleton(InternalType.of("sun.misc.Unsafe")))),
				ImmutableSet.of(
						simpleArtifact(ArtifactCoordinates.from("com.id", "stuff.id", "2.0")))
		);
		DeeplyAnalyzedArtifact two = new DeeplyAnalyzedArtifact(
				ArtifactCoordinates.from("gang.id", "garbage.id", "2.0"),
				MarkInternalDependencies.INDIRECT,
				ImmutableSet.of(),
				ImmutableSet.of(one)
		);
		result.addArtifacts(one, two);

		result.write();

		List<String> lines = Files.lines(resultFile).collect(toList());
		// expect not only content but also alphabetical order
		assertThat(lines).containsExactly(
				"gang.id:garbage.id:2.0 <> INDIRECT",
				"\td: group.id:art.id:1.0 <> DIRECT",
				"group.id:art.id:1.0 <> DIRECT",
				"\tv: com.foo.Bar -> sun.misc.Unsafe",
				"\td: com.id:stuff.id:2.0 <> NONE"
		);
	}

	@Test
	public void read_emptyFile_emptyResult() throws Exception {
		ResultFile result = ResultFile.empty(resultFile);

		assertThat(result.analyzedArtifactsUnmodifiable()).isEmpty();
	}

	@Test
	public void read_fileWithArtifact_resultContainsArtifact() throws Exception {
		// the artifact can have no dependencies because those would also have to be in the file
		Files.newBufferedWriter(resultFile)
				.append("group.id:art.id:1.0 <> DIRECT" + NEW_LINE)
				.append("\tv: com.foo.Bar -> sun.misc.Unsafe" + NEW_LINE)
				.flush();

		ResultFile result = ResultFile.read(resultFile);

		assertThat(result.analyzedArtifactsUnmodifiable()).hasSize(1);
		DeeplyAnalyzedArtifact parsedArtifact = result.analyzedArtifactsUnmodifiable().iterator().next();
		DeeplyAnalyzedArtifact expectedArtifact = new DeeplyAnalyzedArtifact(
				ArtifactCoordinates.from("group.id", "art.id", "1.0"),
				MarkInternalDependencies.DIRECT,
				ImmutableSet.of(
						Violation.buildFor(
								Type.of("com.foo", "Bar"),
								singleton(InternalType.of("sun.misc.Unsafe")))),
				ImmutableSet.of()
		);
		assertThatArtifactsEqual(expectedArtifact, parsedArtifact);
	}

	@Test
	public void read_fileWithArtifacts_resultContainsArtifacts() throws Exception {
		Files.newBufferedWriter(resultFile)
				.append("group.id:art.id:1.0 <> DIRECT" + NEW_LINE)
				.append("\tv: com.foo.Bar -> sun.misc.Unsafe" + NEW_LINE)
				.append("gang.id:garbage.id:2.0 <> INDIRECT" + NEW_LINE)
				.append("\td: group.id:art.id:1.0 <> NONE" + NEW_LINE)
				.flush();

		ResultFile result = ResultFile.read(resultFile);

		assertThat(result.analyzedArtifactsUnmodifiable()).hasSize(2);
		DeeplyAnalyzedArtifact expectedOne = new DeeplyAnalyzedArtifact(
				ArtifactCoordinates.from("group.id", "art.id", "1.0"),
				MarkInternalDependencies.DIRECT,
				ImmutableSet.of(
						Violation.buildFor(
								Type.of("com.foo", "Bar"),
								singleton(InternalType.of("sun.misc.Unsafe")))),
				ImmutableSet.of()
		);
		DeeplyAnalyzedArtifact expectedTwo = new DeeplyAnalyzedArtifact(
				ArtifactCoordinates.from("gang.id", "garbage.id", "2.0"),
				MarkInternalDependencies.INDIRECT,
				ImmutableSet.of(),
				ImmutableSet.of(expectedOne)
		);
		Iterator<DeeplyAnalyzedArtifact> parsed = result.analyzedArtifactsUnmodifiable().iterator();
		// because of alphabetical ordering, 'two' should come before 'one'
		DeeplyAnalyzedArtifact parsedTwo = parsed.next();
		DeeplyAnalyzedArtifact parsedOne = parsed.next();

		assertThatArtifactsEqual(expectedOne, parsedOne);
		assertThatArtifactsEqual(expectedTwo, parsedTwo);
	}

	private static DeeplyAnalyzedArtifact simpleArtifact(ArtifactCoordinates coordinates) {
		return simpleArtifact(coordinates, MarkInternalDependencies.NONE);
	}

	private static DeeplyAnalyzedArtifact simpleArtifact(
			ArtifactCoordinates coordinates, MarkInternalDependencies marker) {
		return new DeeplyAnalyzedArtifact(
				coordinates,
				marker,
				ImmutableSet.of(),
				ImmutableSet.of());
	}

	private void assertThatArtifactsEqual(DeeplyAnalyzedArtifact expected, DeeplyAnalyzedArtifact parsed) {
		assertThat(parsed.coordinates()).isEqualTo(expected.coordinates());
		assertThat(parsed.marker()).isEqualTo(expected.marker());
		assertThat(parsed.violations()).isEqualTo(expected.violations());
		assertThat(parsed.dependees()).isEqualTo(expected.dependees());
	}

}