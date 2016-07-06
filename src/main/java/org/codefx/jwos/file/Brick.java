package org.codefx.jwos.file;

import com.google.common.collect.ImmutableSet;
import javaslang.control.Either;
import org.codefx.jwos.artifact.CompletedArtifact;
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
 * Artifacts can be {@link #addArtifact(CompletedArtifact) added} and the file can be {@link #write() written}.
 */
class Brick {

	private static final String DEPENDANT = "\t<tr><th class=\"dt\" colspan=\"2\"><a id=\"%s\">%s<a></th></tr>";
	private static final String FAILED_ANALYSIS = "\t<tr><td class=\"vdf\" colspan=\"2\">%s</td></tr>";
	private static final String FIRST_VIOLATION = "\t<tr><td class=\"vdt1 vdt\">%s</td><td class=\"vde1 vde\">%s</td></tr>";
	private static final String OTHER_VIOLATION = "\t<tr><td class=\"vdt\">%s</td><td class=\"vde\">%s</td></tr>";
	private static final String OTHER_VIOLATION_OF_MANY = "\t<tr class=\"vdx\"><td class=\"vdt\"/><td class=\"vde\">%2$s</td></tr>";
	private static final String FAILED_RESOLUTION = "\t<tr><td class=\"def\" colspan=\"2\">%s</td></tr>";
	private static final String FIRST_DEPENDEE = "\t<tr><td class=\"de1 de %s\" colspan=\"2\"><a href=\"#%s\">%s</a></td></tr>";
	private static final String OTHER_DEPENDEE = "\t<tr><td class=\"de %s\" colspan=\"2\"><a href=\"#%s\">%s</a></td></tr>";

	private static final String CSS_CLASS_FOR_DEPENDEE_WITH_UNKNOWN_JDK_DEPENDENCIES = "ujd";
	private static final String CSS_CLASS_FOR_DEPENDEE_WITH_NO_JDK_DEPENDENCIES = "njd";
	private static final String CSS_CLASS_FOR_DEPENDEE_WITH_INDIRECT_JDK_DEPENDENCIES = "ijd";
	private static final String CSS_CLASS_FOR_DEPENDEE_WITH_DIRECT_JDK_DEPENDENCIES = "djd";

	private final SortedSet<CompletedArtifact> artifacts;

	private final List<String> frontMatter;

	private final Path file;
	private final Path tempFile;

	private Brick(SortedSet<CompletedArtifact> artifacts, List<String> frontMatter, Path file, Path tempFile) {
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

	public void addArtifact(CompletedArtifact artifact) {
		artifacts.add(artifact);
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
			writeFrontMatterToWriter(writer);
			writeArtifactsToWriter(writer);
		} catch (RuntimeIOException ex) {
			throw ex.getCause();
		}
	}

	private void writeFrontMatterToWriter(BufferedWriter writer) {
		frontMatter.forEach(frontMatterLine -> writeLine(writer, frontMatterLine));
	}

	private static void writeLine(BufferedWriter writer, String format, Object... args) {
		try {
			writer.append(format(format, args));
			writer.newLine();
		} catch (IOException ex) {
			throw new RuntimeIOException(ex);
		}
	}

	private void writeArtifactsToWriter(BufferedWriter writer) {
		artifacts.forEach(artifact -> writeArtifact(writer, artifact));
	}

	private static void writeArtifact(BufferedWriter writer, CompletedArtifact artifact) {
		writeLine(writer, "<table class=\"artifacts\">");
		writeDependent(writer, artifact);
		writeViolations(writer, artifact);
		writeDependees(writer, artifact);
		writeLine(writer, "</table>");
	}

	private static void writeDependent(BufferedWriter writer, CompletedArtifact artifact) {
		String coordinates = artifact.coordinates().toString();
		writeLine(writer, DEPENDANT, coordinates, coordinates);
	}

	private static void writeViolations(BufferedWriter writer, CompletedArtifact artifact) {
		Either<Exception, ImmutableSet<Violation>> violations = artifact.violations();
		if (violations.isLeft())
			writeFailedAnalysis(writer, violations.getLeft());
		else
			writeAnalysedViolations(writer, violations.get());
	}

	private static void writeFailedAnalysis(BufferedWriter writer, Exception error) {
		// TODO is the exception message ok should we write something else?
		writeLine(writer, FAILED_ANALYSIS, error.getMessage());
	}

	private static void writeAnalysedViolations(BufferedWriter writer, ImmutableSet<Violation> violations) {
		violations.stream()
				.findFirst()
				.ifPresent(violation -> writeViolation(writer, FIRST_VIOLATION, OTHER_VIOLATION_OF_MANY, violation));
		violations.stream()
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

	private static void writeDependees(BufferedWriter writer, CompletedArtifact artifact) {
		Either<Exception, ImmutableSet<CompletedArtifact>> dependees = artifact.dependees();
		if (dependees.isLeft())
			writeFailedResolution(writer, dependees.getLeft());
		else
			writeAnalysedDependees(writer, dependees.get());
	}

	private static void writeFailedResolution(BufferedWriter writer, Exception error) {
		// TODO is the exception message ok or should we write something else?
		writeLine(writer, FAILED_RESOLUTION, error.getMessage());
	}

	private static void writeAnalysedDependees(BufferedWriter writer, ImmutableSet<CompletedArtifact> dependees) {
		dependees.stream()
				.findFirst()
				.ifPresent(dependee -> writeDependee(writer, FIRST_DEPENDEE, dependee));
		dependees.stream()
				.skip(1)
				.forEach(dependee -> writeDependee(writer, OTHER_DEPENDEE, dependee));
	}

	private static void writeDependee(BufferedWriter writer, String format, CompletedArtifact dependee) {
		String coordinates = dependee.coordinates().toString();
		writeLine(
				writer,
				format,
				cssClassForDependeesDependenciesOnJdk(dependee),
				coordinates,
				coordinates.replace(":", " : "));
	}

	private static String cssClassForDependeesDependenciesOnJdk(CompletedArtifact dependee) {
		switch (dependee.transitiveMarker()) {
			case UNKNOWN:
				return CSS_CLASS_FOR_DEPENDEE_WITH_UNKNOWN_JDK_DEPENDENCIES;
			case NONE:
				return CSS_CLASS_FOR_DEPENDEE_WITH_NO_JDK_DEPENDENCIES;
			case INDIRECT:
				return CSS_CLASS_FOR_DEPENDEE_WITH_INDIRECT_JDK_DEPENDENCIES;
			case DIRECT:
				return CSS_CLASS_FOR_DEPENDEE_WITH_DIRECT_JDK_DEPENDENCIES;
			default:
				throw new IllegalArgumentException(
						format("Unknown dependency marker \"%s\".", dependee.transitiveMarker()));
		}
	}

	private void replaceFileWithTempFile() throws IOException {
		Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

	private static class RuntimeIOException extends RuntimeException {

		public RuntimeIOException(IOException cause) {
			super(cause);
		}

		@Override
		public synchronized IOException getCause() {
			return (IOException) super.getCause();
		}
	}

}
