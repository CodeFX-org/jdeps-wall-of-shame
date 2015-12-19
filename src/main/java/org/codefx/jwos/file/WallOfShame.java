package org.codefx.jwos.file;

import org.codefx.jwos.artifact.DeeplyAnalyzedArtifact;
import org.codefx.jwos.git.GitDirectory;
import org.codefx.jwos.git.GitInformation;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

public class WallOfShame {

	private final Wall wall;
	private final GitDirectory git;

	private final List<DeeplyAnalyzedArtifact> addedSinceLastCommit;

	private WallOfShame(Wall wall, GitDirectory git) {
		this.wall = requireNonNull(wall, "The argument 'wall' must not be null.");
		this.git = requireNonNull(git, "The argument 'git' must not be null.");
		this.addedSinceLastCommit = new ArrayList<>();
	}

	public static WallOfShame openExistingDirectory(
			WallFiles wallFiles, GitInformation gitInformation) throws IOException {
		requireNonNull(gitInformation, "The argument 'gitInformation' must not be null.");
		requireNonNull(wallFiles, "The argument 'wallFiles' must not be null.");

		GitDirectory git = GitDirectory.openExisting(gitInformation);
		Wall wall = Wall.of(wallFiles);
		return new WallOfShame(wall, git);
	}

	public static WallOfShame openExistingDirectoryWithDefaults(
			String remoteUrl, Path directory, String userName, String password, String email) throws IOException {
		return openExistingDirectory(
				WallFiles.defaultsInDirectory(directory),
				GitInformation.simple(remoteUrl, directory, userName, password, email));
	}

	public void addArtifacts(DeeplyAnalyzedArtifact... artifacts) {
		addArtifacts(Arrays.stream(artifacts));
	}

	public void addArtifacts(Stream<DeeplyAnalyzedArtifact> artifacts) {
		artifacts.forEach(this::addArtifact);
	}

	private void addArtifact(DeeplyAnalyzedArtifact artifact) {
		wall.addArtifact(artifact);
		addedSinceLastCommit.add(artifact);
	}

	public void write() throws IOException {
		wall.write();
	}

	public void commit() throws GitAPIException {
		git.commitAll(createCommitMessage());
		addedSinceLastCommit.clear();
	}

	private String createCommitMessage() {
		return "Publish new results\n"
				+ addedSinceLastCommit.stream()
				.map(artifact -> artifact.artifact().toString() + ": " + artifact.marker())
				.collect(joining("\n * ", "\n * ", "\n"));
	}

	public void push() throws GitAPIException {
		git.push();
	}

}
