package org.codefx.jwos.discovery;

import org.codefx.jwos.artifact.ProjectCoordinates;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A plain text file containing project coordinates.
 * <p>
 * A project list file must be of the following structure:
 *
 * [comment lines]
 * [empty line]
 * [project lines in the form "groupId:artifactId"]
 *
 * The empty line is crucial as it is used to determine where the projects start.
 * It even has to be present if the file is uncommented.
 */
public class ProjectListFile {

	private final Path file;
	private Iterator<String> openedFile;

	public ProjectListFile(Path file) {
		this.file = requireNonNull(file, "The argument 'file' must not be null.");
		if (!Files.isRegularFile(file))
			throw new IllegalArgumentException();
	}

	public ProjectListFile open() throws IOException {
		openedFile = Files.lines(file).iterator();
		fastForwardPastComments(openedFile);
		return this;
	}

	private static void fastForwardPastComments(Iterator<String> openedFile) {
		boolean foundEmptyLine = false;
		while (!foundEmptyLine && openedFile.hasNext())
			foundEmptyLine = openedFile.next().trim().isEmpty();
	}

	public Optional<ProjectCoordinates> readNextProject() {
		if (openedFile.hasNext())
			return Optional.of(readLine(openedFile.next()));
		else
			return Optional.empty();
	}

	private ProjectCoordinates readLine(String line) {
		String[] coordinates = line.split(":");
		return ProjectCoordinates.from(coordinates[0], coordinates[1]);
	}

}
