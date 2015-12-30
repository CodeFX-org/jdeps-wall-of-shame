package org.codefx.jwos.file;

import org.codefx.jwos.artifact.DeeplyAnalyzedArtifact;
import org.codefx.jwos.artifact.MarkInternalDependencies;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static org.codefx.jwos.artifact.MarkInternalDependencies.DIRECT;
import static org.codefx.jwos.artifact.MarkInternalDependencies.INDIRECT;
import static org.codefx.jwos.artifact.MarkInternalDependencies.NONE;

/**
 * The wall contains a {@link Brick} (a single file) for each type of
 * {@link MarkInternalDependencies internal dependency}.
 * <p>
 * Artifacts can be {@link #addArtifact(DeeplyAnalyzedArtifact) added} and the files can be {@link #write() written}.
 * <p>
 * This class is not thread-safe.
 */
class Wall {

	private final Map<MarkInternalDependencies, Brick> bricks;

	private Wall(Brick directDependencies, Brick indirectDependencies, Brick noDependencies) {
		bricks = new HashMap<>();
		bricks.put(DIRECT, directDependencies);
		bricks.put(INDIRECT, indirectDependencies);
		bricks.put(NONE, noDependencies);
	}

	public static Wall of(WallFiles files, Collection<DeeplyAnalyzedArtifact> artifacts) throws IOException {
		requireNonNull(files, "The argument 'files' must not be null.");
		requireNonNull(artifacts, "The argument 'artifacts' must not be null.");

		Wall wall = new Wall(
				Brick.of(files.directDependenciesFrontMatter(), files.directDependencies()),
				Brick.of(files.indirectDependenciesFrontMatter(), files.indirectDependencies()),
				Brick.of(files.noDependenciesFrontMatter(), files.noDependencies()));
		artifacts.forEach(wall::addArtifact);
		return wall;
	}

	public static Wall of(WallFiles files) throws IOException {
		return of(files, emptySet());
	}

	public void addArtifact(DeeplyAnalyzedArtifact artifact) {
		bricks.get(artifact.marker()).addArtifact(artifact);
	}

	public void write() throws IOException {
		for (Brick brick : bricks.values()) {
			brick.write();
		}
	}

}
