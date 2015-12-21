package org.codefx.jwos;

import com.google.common.collect.ImmutableSet;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;

public class Util {

	public static final String[] PROJECT_LIST_FILE_NAMES = { "top10JavaLibrariesByTakipi.txt" };
	public static final String RESULT_FILE_NAME = "results.txt";

	public static final Path PAGES_DIRECTORY = Paths.get("/home/nipa/code/JDepsWallOfShame-Pages");

	public static final String REPOSITORY_URL = "git@github.com:CodeFX-org/jdeps-wall-of-shame.git";
	public static final String USER_NAME = "nicolaiparlog";
	public static final String EMAIL = "nipa@codefx.org";
	public static final String PASSWORD = "GithubWithNicolaiParlog";

	public static Path getPathToResourceFile(String fileName) {
		return Optional
				.ofNullable(Util.class.getClassLoader().getResource(fileName))
				.map(URL::getPath)
				.map(Paths::get)
				.orElseThrow(() -> new IllegalArgumentException(format("No resource file '%s' was found.", fileName)));
	}

	public static <T> Collector<T, ?, ImmutableSet<T>> toImmutableSet() {
		// TODO create more performant implementation using the builder
		return Collectors.collectingAndThen(toSet(), ImmutableSet::copyOf);
	}
}
