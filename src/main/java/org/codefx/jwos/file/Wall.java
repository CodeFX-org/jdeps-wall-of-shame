package org.codefx.jwos.file;

import org.codefx.jwos.artifact.CompletedArtifact;
import org.codefx.jwos.artifact.MarkTransitiveInternalDependencies;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static org.codefx.jwos.artifact.MarkTransitiveInternalDependencies.DIRECT;
import static org.codefx.jwos.artifact.MarkTransitiveInternalDependencies.INDIRECT;
import static org.codefx.jwos.artifact.MarkTransitiveInternalDependencies.NONE;
import static org.codefx.jwos.artifact.MarkTransitiveInternalDependencies.UNKNOWN;

/**
 * The wall contains a {@link Brick} (a single file) for each type of
 * {@link MarkTransitiveInternalDependencies internal dependency}.
 * <p>
 * Artifacts can be {@link #addArtifact(CompletedArtifact) added} and the files can be {@link #write() written}.
 * <p>
 * This class is not thread-safe.
 */
class Wall {

	private final Map<MarkTransitiveInternalDependencies, Brick> bricks;

	private Wall(
			Brick unknownDependencies,
			Brick noDependencies,
			Brick indirectDependencies,
			Brick directDependencies) {
		bricks = new EnumMap<>(MarkTransitiveInternalDependencies.class);
		bricks.put(UNKNOWN, unknownDependencies);
		bricks.put(NONE, noDependencies);
		bricks.put(INDIRECT, indirectDependencies);
		bricks.put(DIRECT, directDependencies);
	}

	public static Wall of(WallFiles files, Collection<CompletedArtifact> artifacts) throws IOException {
		requireNonNull(files, "The argument 'files' must not be null.");
		requireNonNull(artifacts, "The argument 'artifacts' must not be null.");

		Wall wall = new Wall(
				Brick.of(files.unknownDependenciesFrontMatter(), files.unknownDependencies()),
				Brick.of(files.noDependenciesFrontMatter(), files.noDependencies()),
				Brick.of(files.indirectDependenciesFrontMatter(), files.indirectDependencies()),
				Brick.of(files.directDependenciesFrontMatter(), files.directDependencies()));
		artifacts.forEach(wall::addArtifact);
		return wall;
	}

	public static Wall of(WallFiles files) throws IOException {
		return of(files, emptySet());
	}

	public void addArtifact(CompletedArtifact artifact) {
		bricks.get(artifact.transitiveMarker()).addArtifact(artifact);
	}

	public void write() throws IOException {
		for (Brick brick : bricks.values()) {
			brick.write();
		}
	}

}
