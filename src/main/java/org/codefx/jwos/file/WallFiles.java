package org.codefx.jwos.file;

import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;

/**
 * Holds paths to the files used to create the {@link WallOfShame}.
 */
public class WallFiles {

	private static final Path DEFAULT_DIRECT_DEPENDENCIES_FRONT_MATTER = Paths.get("_includes/direct-dependencies.md");
	private static final Path DEFAULT_INDIRECT_DEPENDENCIES_FRONT_MATTER = Paths.get("_includes/indirect-dependencies.md");
	private static final Path DEFAULT_NO_DEPENDENCIES_FRONT_MATTER = Paths.get("_includes/no-dependencies.md");
	private static final Path DEFAULT_UNKNOWN_DEPENDENCIES_FRONT_MATTER = Paths.get("_includes/unknown-dependencies.md");

	private static final Path DEFAULT_DIRECT_DEPENDENCIES = Paths.get("_posts/2015-12-15-direct-dependencies.md");
	private static final Path DEFAULT_INDIRECT_DEPENDENCIES = Paths.get("_posts/2015-12-16-indirect-dependencies.md");
	private static final Path DEFAULT_NO_DEPENDENCIES = Paths.get("_posts/2015-12-18-no-dependencies.md");
	private static final Path DEFAULT_UNKNOWN_DEPENDENCIES = Paths.get("_posts/2015-12-17-unknown-dependencies.md");

	private final Path directDependenciesFrontMatter;
	private final Path indirectDependenciesFrontMatter;
	private final Path noDependenciesFrontMatter;
	private final Path unknownDependenciesFrontMatter;

	private final Path directDependencies;
	private final Path indirectDependencies;
	private final Path noDependencies;
	private final Path unknownDependencies;

	public WallFiles(
			Path directDependenciesFrontMatter,
			Path indirectDependenciesFrontMatter,
			Path noDependenciesFrontMatter,
			Path unknownDependenciesFrontMatter,
			Path directDependencies,
			Path indirectDependencies,
			Path noDependencies,
			Path unknownDependencies) {
		this.directDependenciesFrontMatter = requireNonNull(
				directDependenciesFrontMatter,
				"The argument 'directDependenciesFrontMatter' must not be null.");
		this.indirectDependenciesFrontMatter = requireNonNull(
				indirectDependenciesFrontMatter,
				"The argument 'indirectDependenciesFrontMatter' must not be null.");
		this.noDependenciesFrontMatter = requireNonNull(
				noDependenciesFrontMatter,
				"The argument 'noDependenciesFrontMatter' must not be null.");
		this.unknownDependenciesFrontMatter = requireNonNull(
				unknownDependenciesFrontMatter,
				"The argument 'unknownDependenciesFrontMatter' must not be null.");

		this.directDependencies =
				requireNonNull(directDependencies, "The argument 'directDependencies' must not be null.");
		this.indirectDependencies =
				requireNonNull(indirectDependencies, "The argument 'indirectDependencies' must not be null.");
		this.noDependencies =
				requireNonNull(noDependencies, "The argument 'noDependencies' must not be null.");
		this.unknownDependencies = requireNonNull(
				unknownDependencies,
				"The argument 'unknownDependencies' must not be null.");
	}

	public static WallFiles defaultsInDirectory(Path directory) {
		return new WallFiles(
				directory.resolve(DEFAULT_DIRECT_DEPENDENCIES_FRONT_MATTER),
				directory.resolve(DEFAULT_INDIRECT_DEPENDENCIES_FRONT_MATTER),
				directory.resolve(DEFAULT_NO_DEPENDENCIES_FRONT_MATTER),
				directory.resolve(DEFAULT_UNKNOWN_DEPENDENCIES_FRONT_MATTER),
				directory.resolve(DEFAULT_DIRECT_DEPENDENCIES),
				directory.resolve(DEFAULT_INDIRECT_DEPENDENCIES),
				directory.resolve(DEFAULT_NO_DEPENDENCIES),
				directory.resolve(DEFAULT_UNKNOWN_DEPENDENCIES));
	}

	Path directDependenciesFrontMatter() {
		return directDependenciesFrontMatter;
	}

	Path indirectDependenciesFrontMatter() {
		return indirectDependenciesFrontMatter;
	}

	Path noDependenciesFrontMatter() {
		return noDependenciesFrontMatter;
	}

	Path unknownDependenciesFrontMatter() {
		return unknownDependenciesFrontMatter;
	}

	Path directDependencies() {
		return directDependencies;
	}

	Path indirectDependencies() {
		return indirectDependencies;
	}

	Path noDependencies() {
		return noDependencies;
	}

	Path unknownDependencies() {
		return unknownDependencies;
	}

}
