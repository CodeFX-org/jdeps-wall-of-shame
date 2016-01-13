package org.codefx.jwos.file;

import org.codefx.jwos.artifact.DeeplyAnalyzedArtifact;
import org.codefx.jwos.artifact.IdentifiesArtifact;
import org.codefx.jwos.jdeps.dependency.InternalType;
import org.codefx.jwos.jdeps.dependency.Type;
import org.codefx.jwos.jdeps.dependency.Violation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * An individual file of the Wall.
 * <p>
 * Artifacts can be {@link #addArtifact(DeeplyAnalyzedArtifact) added} and the file can be {@link #write() written}.
 */
class Brick {

	private static final String DEPENDANT = "\t<tr><th class=\"dt\" colspan=\"2\">%s</th></tr>";
	private static final String FIRST_VIOLATION = "\t<tr><td class=\"vdt1 vdt\">%s</td><td class=\"vde1 vde\">%s</td></tr>";
	private static final String OTHER_VIOLATION = "\t<tr><td class=\"vdt\">%s</td><td class=\"vde\">%s</td></tr>";
	private static final String OTHER_VIOLATION_OF_MANY = "\t<tr class=\"vdx\"><td class=\"vdt\"/><td class=\"vde\">%2$s</td></tr>";
	private static final String FIRST_DEPENDEE = "\t<tr><td class=\"de1 de\" colspan=\"2\">%s</td></tr>";
	private static final String OTHER_DEPENDEE = "\t<tr><td class=\"de\" colspan=\"2\">%s</td></tr>";

	private final SortedSet<DeeplyAnalyzedArtifact> artifacts;

	private final List<String> frontMatter;

	private final Path file;
	private final Path tempFile;

	private Brick(SortedSet<DeeplyAnalyzedArtifact> artifacts, List<String> frontMatter, Path file, Path tempFile) {
		this.artifacts = artifacts;
		this.frontMatter = frontMatter;
		this.file = file;
		this.tempFile = tempFile;
	}

	public static Brick of(Path frontMatterFile, Path postFile) throws IOException {
		requireNonNull(frontMatterFile, "The argument 'frontMatterFile' must not be null.");
		requireNonNull(postFile, "The argument 'file' must not be null.");
		return new Brick(
				new TreeSet<>(IdentifiesArtifact.alphabeticalOrder()),
				Files.lines(frontMatterFile).collect(toList()),
				postFile,
				postFile.resolveSibling(postFile.getFileName() + ".tmp")
		);
	}

	private static void writeArtifact(BufferedWriter writer, DeeplyAnalyzedArtifact artifact) {
		writeLine(writer, "<table class=\"artifacts\">");
		writeDependent(writer, artifact);
		writeViolations(writer, artifact);
		writeDependees(writer, artifact);
		writeLine(writer, "</table>");
	}

	private static void writeDependent(BufferedWriter writer, DeeplyAnalyzedArtifact artifact) {
		writeLine(writer, DEPENDANT, artifact.coordinates().toString());
	}

	private static void writeViolations(BufferedWriter writer, DeeplyAnalyzedArtifact artifact) {
		artifact.violations().stream()
				.findFirst()
				.ifPresent(violation -> writeViolation(writer, FIRST_VIOLATION, OTHER_VIOLATION_OF_MANY, violation));
		artifact.violations().stream()
				.skip(1)
				.forEach(violation -> writeViolation(writer, OTHER_VIOLATION, OTHER_VIOLATION_OF_MANY, violation));
	}

	private static void writeViolation(
			BufferedWriter writer,
			String firstViolationFormat,
			String otherViolationOfManyFormat,
			Violation violation) {
		violation.getInternalDependencies().stream()
				.findFirst()
				.ifPresent(dependee ->
						writeViolationLine(writer, firstViolationFormat, violation.getDependent(), dependee));
		violation.getInternalDependencies().stream()
				.skip(1)
				.forEach(dependee ->
						writeViolationLine(writer, otherViolationOfManyFormat, violation.getDependent(), dependee));
	}

	private static void writeViolationLine(
			BufferedWriter writer, String format, Type dependent, InternalType dependee) {
		writeLine(writer, format, dependent.getClassName(), dependee.getFullyQualifiedName());
	}

	private static void writeDependees(BufferedWriter writer, DeeplyAnalyzedArtifact artifact) {
		artifact.dependees().stream()
				.findFirst()
				.ifPresent(dependee -> writeDependee(writer, FIRST_DEPENDEE, dependee));
		artifact.dependees().stream()
				.skip(1)
				.forEach(dependee -> writeDependee(writer, OTHER_DEPENDEE, dependee));
	}

	private static void writeDependee(BufferedWriter writer, String format, DeeplyAnalyzedArtifact dependee) {
		writeLine(writer, format, dependee.coordinates().toString().replace(":", " : "));
	}

	private static void writeLine(BufferedWriter writer, String format, Object... args) {
		try {
			writer.append(format(format, args));
			writer.newLine();
		} catch (IOException ex) {
			throw new RuntimeIOException(ex);
		}
	}

	public void addArtifact(DeeplyAnalyzedArtifact artifact) {
		artifacts.add(artifact);
	}

	public void write() throws IOException {
		deleteTempFileIfExists();
		writeTempFile();
		replaceFileWithTempFile();
	}

	private void deleteTempFileIfExists() throws IOException {
		Files.deleteIfExists(tempFile);
	}

	private void writeTempFile() throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
			writeFrontMatterToWriter(writer);
			writeArtifactsToWriter(writer);
			// TODO: revert to IOException
		}
	}

	private void writeFrontMatterToWriter(BufferedWriter writer) {
		// TODO moar streams
		for (String frontMatterLine : frontMatter)
			writeLine(writer, frontMatterLine);
	}

	private void writeArtifactsToWriter(BufferedWriter writer) {
		// TODO moar streams
		for (DeeplyAnalyzedArtifact artifact : artifacts)
			writeArtifact(writer, artifact);
	}

	private void replaceFileWithTempFile() throws IOException {
		Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

	private static class RuntimeIOException extends RuntimeException {
		public RuntimeIOException(IOException cause) {
			super(cause);
		}
	}

}
