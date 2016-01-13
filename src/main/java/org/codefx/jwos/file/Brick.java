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

	private static final String SEPARATOR_IN_VIOLATION = " -> ";
	private static final String VIOLATION_LINE = "%s " + SEPARATOR_IN_VIOLATION + " %s";
	
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
		}
	}

	private void writeFrontMatterToWriter(BufferedWriter writer) throws IOException {
		for (String frontMatterLine : frontMatter)
			writeLine(writer, frontMatterLine);
	}

	private void writeArtifactsToWriter(BufferedWriter writer) throws IOException {
		writeLine(writer, "<ul>");
		for (DeeplyAnalyzedArtifact artifact : artifacts)
			writeArtifact(writer, artifact);
		writeLine(writer, "</ul>");
	}

	private static void writeArtifact(BufferedWriter writer, DeeplyAnalyzedArtifact artifact) throws IOException {
		writeLine(writer, "<li>");
		writeArtifactLine(writer, artifact);

		writeLine(writer, "<ul>");
		for (Violation violation : artifact.violations())
			for (InternalType dependeeType : violation.getInternalDependencies())
				writeViolationLine(writer, violation.getDependent(), dependeeType);
		for (DeeplyAnalyzedArtifact dependee : artifact.dependees())
			writeArtifactLine(writer, dependee);
		writeLine(writer, "</ul>");

		writeLine(writer, "</li>");
	}

	private static void writeArtifactLine(BufferedWriter writer, DeeplyAnalyzedArtifact artifact) throws IOException {
		writeLine(writer, artifact.coordinates().toString());
	}

	private static void writeViolationLine(BufferedWriter writer, Type dependent, InternalType dependee)
			throws IOException {
		writeLine(writer, VIOLATION_LINE, dependent.getFullyQualifiedName(), dependee.getFullyQualifiedName());
	}

	private static void writeLine(BufferedWriter writer, String format, Object... args) throws IOException {
		writer.append(format(format, args));
		writer.newLine();
	}

	private void replaceFileWithTempFile() throws IOException {
		Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

}
