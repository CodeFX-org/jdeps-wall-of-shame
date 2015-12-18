package org.codefx.jwos.file;

import com.google.common.collect.ImmutableSet;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.DeeplyAnalyzedArtifact;
import org.codefx.jwos.artifact.IdentifiesArtifact;
import org.codefx.jwos.artifact.InternalDependencies;
import org.codefx.jwos.jdeps.dependency.InternalType;
import org.codefx.jwos.jdeps.dependency.Type;
import org.codefx.jwos.jdeps.dependency.Violation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;
import static java.util.Comparator.comparingInt;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toSet;

public class ResultFile {

	private static final String SEPARATOR_ARTIFACT_MARKER = " <> ";
	private static final String SEPARATOR_IN_VIOLATION = " -> ";
	private static final String VIOLATION_LINE_PREFIX = "\tv: ";
	private static final String DEPENDENCY_LINE_PREFIX = "\td: ";

	private final Path file;
	private final Path tempFile;
	private final SortedSet<DeeplyAnalyzedArtifact> artifacts;

	// CREATE

	private ResultFile(Path file, SortedSet<DeeplyAnalyzedArtifact> artifacts) {
		this.file = requireNonNull(file, "The argument 'file' must not be null.");
		this.tempFile = file.resolveSibling(file.getFileName() + ".tmp");
		this.artifacts = requireNonNull(artifacts, "The argument 'artifacts' must not be null.");
	}

	public static ResultFile readOrEmpty(Path file) throws IOException {
		requireNonNull(file, "The argument 'file' must not be null.");
		return Files.exists(file) ? read(file) : empty(file);
	}

	public static ResultFile read(Path file) throws IOException {
		if (!Files.isRegularFile(file) || !Files.isReadable(file))
			throw new IOException(
					"The file '" + file + "' does not exist or can ont be read. "
							+ "To create a new one call 'ResultFile.empty'.");
		return new ResultFile(file, readFile(file));
	}

	public static ResultFile empty(Path file) throws IOException {
		try {
			Files.createFile(file);
		} catch (IOException ex) {
			throw new IOException("The file '" + file + "' already exists. To read it, call 'ResultFile.read'.", ex);
		}
		return new ResultFile(file, new TreeSet<>(IdentifiesArtifact.alphabeticalOrder()));
	}

	// READ

	private static SortedSet<DeeplyAnalyzedArtifact> readFile(Path file) throws IOException {
		SortedSet<DeeplyAnalyzedArtifact> preliminaryArtifacts = parsePreliminaryArtifacts(file);
		Collection<DeeplyAnalyzedArtifact> artifacts = finalizeArtifacts(preliminaryArtifacts);
		return sortArtifacts(artifacts);
	}

	private static SortedSet<DeeplyAnalyzedArtifact> parsePreliminaryArtifacts(Path file) throws IOException {
		SortedSet<DeeplyAnalyzedArtifact> preliminaryArtifacts =
				new TreeSet<>(comparingInt(artifact -> artifact.dependees().size()));

		ArtifactCoordinates artifact = null;
		InternalDependencies marker = null;
		List<Violation> violations = new ArrayList<>();
		// TODO: this is a lie!
		// these artifacts are not truly "deeply analyzed";
		// on the contrary, these have no violations and dependencies on their own
		List<DeeplyAnalyzedArtifact> dependees = new ArrayList<>();

		for (String line : (Iterable<String>) Files.lines(file)::iterator) {
			if (line.startsWith(VIOLATION_LINE_PREFIX))
				violations.add(parseViolationLine(line));
			else if (line.startsWith(DEPENDENCY_LINE_PREFIX))
				dependees.add(parseDependencyLine(line));
			else {
				// a new artifact begins here; add the former to the set ...
				createArtifact(artifact, marker, violations, dependees).ifPresent(preliminaryArtifacts::add);
				// ... and start a new one
				artifact = parseArtifactString(line);
				marker = parseMarkerString(line);
				violations.clear();
				dependees.clear();
			}
		}
		// create the last artifact
		createArtifact(artifact, marker, violations, dependees).ifPresent(preliminaryArtifacts::add);

		return preliminaryArtifacts;
	}

	private static Violation parseViolationLine(String line) {
		if (!line.startsWith(VIOLATION_LINE_PREFIX))
			throw new IllegalArgumentException();

		int typeStartIndex = VIOLATION_LINE_PREFIX.length();
		int typeEndIndex = line.indexOf(SEPARATOR_IN_VIOLATION);
		Type type = Type.of(line.substring(typeStartIndex, typeEndIndex));

		int internalTypeStartIndex = line.indexOf(SEPARATOR_IN_VIOLATION) + SEPARATOR_IN_VIOLATION.length();
		int internalTypeEndIndex = line.length();
		InternalType internalType = InternalType.of(line.substring(internalTypeStartIndex, internalTypeEndIndex));

		// TODO this creates a nw violation per pair which is not quite the semantic of 'Violation'
		return Violation.buildFor(type, singleton(internalType));
	}

	private static DeeplyAnalyzedArtifact parseDependencyLine(String line) {
		if (!line.startsWith(DEPENDENCY_LINE_PREFIX))
			throw new IllegalArgumentException();

		ArtifactCoordinates dependee =
				parseArtifactString(line.substring(DEPENDENCY_LINE_PREFIX.length(), line.length()));
		InternalDependencies marker = parseMarkerString(line);
		return new DeeplyAnalyzedArtifact(dependee, marker, ImmutableSet.of(), ImmutableSet.of());
	}

	private static ArtifactCoordinates parseArtifactString(String line) {
		String[] artifactAndMarker = line.trim().split(SEPARATOR_ARTIFACT_MARKER);
		String[] coordinates = artifactAndMarker[0].split(":");
		return ArtifactCoordinates.from(coordinates[0], coordinates[1], coordinates[2]);
	}

	private static InternalDependencies parseMarkerString(String line) {
		String[] artifactAndMarker = line.trim().split(SEPARATOR_ARTIFACT_MARKER);
		return InternalDependencies.valueOf(artifactAndMarker[1]);
	}

	private static Optional<DeeplyAnalyzedArtifact> createArtifact(
			ArtifactCoordinates artifact, InternalDependencies marker, List<Violation> violations,
			List<DeeplyAnalyzedArtifact> dependees) {
		if (artifact == null)
			return Optional.empty();

		return Optional.of(new DeeplyAnalyzedArtifact(
				artifact, marker, ImmutableSet.copyOf(violations), ImmutableSet.copyOf(dependees)));
	}

	private static Collection<DeeplyAnalyzedArtifact> finalizeArtifacts(
			SortedSet<DeeplyAnalyzedArtifact> preliminaryArtifacts) {

		// if the artifacts are sorted by the number of their dependees
		// (and assuming that file contains a full entry for each artifact that is listed as a dependee),
		// the final set of artifacts can be created by iterating over the preliminary set
		// since then the each dependee's artifact was already finalized

		Map<ArtifactCoordinates, DeeplyAnalyzedArtifact> artifacts = new HashMap<>();
		for (DeeplyAnalyzedArtifact preliminaryArtifact : preliminaryArtifacts) {
			ImmutableSet<DeeplyAnalyzedArtifact> dependees = preliminaryArtifact
					.dependees().stream()
					.map(IdentifiesArtifact::artifact)
					.map(artifacts::get)
					.collect(collectingAndThen(toSet(), ImmutableSet::copyOf));
			artifacts.put(
					preliminaryArtifact.artifact(),
					new DeeplyAnalyzedArtifact(
							preliminaryArtifact.artifact(),
							preliminaryArtifact.marker(),
							preliminaryArtifact.violations(),
							dependees));
		}

		return artifacts.values();
	}

	private static SortedSet<DeeplyAnalyzedArtifact> sortArtifacts(Collection<DeeplyAnalyzedArtifact> artifacts) {
		TreeSet<DeeplyAnalyzedArtifact> sortedArtifacts = new TreeSet<>(IdentifiesArtifact.alphabeticalOrder());
		sortedArtifacts.addAll(artifacts);
		return sortedArtifacts;
	}

	// WRITE

	public void addArtifacts(Collection<DeeplyAnalyzedArtifact> artifacts) {
		this.artifacts.addAll(artifacts);
	}

	public void addArtifacts(DeeplyAnalyzedArtifact... artifacts) {
		Arrays.stream(artifacts).forEach(this.artifacts::add);
	}

	public void write() throws IOException {
		deleteTempFileIfExists();
		writeArtifactsToTempFile();
		replaceFileWithTempFile();
	}

	private void deleteTempFileIfExists() throws IOException {
		Files.deleteIfExists(tempFile);
	}

	private void writeArtifactsToTempFile() throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
			for (DeeplyAnalyzedArtifact artifact : artifacts)
				writeArtifact(artifact, writer);
		}
	}

	private static void writeArtifact(DeeplyAnalyzedArtifact artifact, BufferedWriter writer) throws IOException {
		writeArtifactLine(false, artifact, writer);
		for (Violation violation : artifact.violations())
			for (InternalType dependeeType : violation.getInternalDependencies())
				writeViolationLine(violation.getDependent(), dependeeType, writer);
		for (DeeplyAnalyzedArtifact dependee : artifact.dependees())
			writeArtifactLine(true, dependee, writer);
	}

	private static void writeArtifactLine(
			boolean indented, DeeplyAnalyzedArtifact artifact, BufferedWriter writer) throws IOException {
		if (indented)
			writer.append(DEPENDENCY_LINE_PREFIX);
		writer.append(artifact.artifact().toString());
		writer.append(SEPARATOR_ARTIFACT_MARKER);
		writer.append(artifact.marker().toString());
		writer.newLine();
	}

	private static void writeViolationLine(Type dependent, InternalType dependee, BufferedWriter writer)
			throws IOException {
		writer.append(VIOLATION_LINE_PREFIX);
		writer.append(dependent.getFullyQualifiedName());
		writer.append(SEPARATOR_IN_VIOLATION);
		writer.append(dependee.getFullyQualifiedName());
		writer.newLine();
	}

	private void replaceFileWithTempFile() throws IOException {
		Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

	// ACCESS

	public Set<DeeplyAnalyzedArtifact> unmodifiableArtifacts() {
		return unmodifiableSet(artifacts);
	}

}