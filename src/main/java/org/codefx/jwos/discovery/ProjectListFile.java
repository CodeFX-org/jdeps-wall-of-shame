package org.codefx.jwos.discovery;

import org.codefx.jwos.artifact.ProjectCoordinates;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class ProjectListFile {

	private final Path file;
	private Iterator<String> openedFile;

	public ProjectListFile(Path file) {
		if (!Files.isRegularFile(file))
			throw new IllegalArgumentException();
		this.file = requireNonNull(file, "The argument 'file' must not be null.");
	}

	public void open() throws IOException {
		openedFile = Files.lines(file).iterator();
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
