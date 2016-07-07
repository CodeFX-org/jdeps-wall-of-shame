package org.codefx.jwos;

import com.google.common.collect.ImmutableSet;
import org.codefx.jwos.file.RuntimeIOException;
import org.codefx.jwos.file.YamlAnalysisPersistence;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class Util {

	public static final String[] PROJECT_LIST_FILE_NAMES = {
			"SomeLibraries.txt",
//			"cadenzaDependencies.txt",
//			"top100JavaLibrariesByTakipi.txt"
	};
	public static final String RESULT_FILE_NAME = "results.yaml";

	public static final Path LOCAL_MAVEN_REPOSITORY = Paths.get("/home/parlog/.m2/repository");
	public static final Path PAGES_DIRECTORY = Paths.get("/home/parlog/Code/others-nipa/JDeps-Wall-of-Shame-Pages");

	public static final String GIT_REPOSITORY_URL = "git@github.com:CodeFX-org/jdeps-wall-of-shame.git";
	public static final String GIT_USER_NAME = "nicolaiparlog";
	public static final String GIT_EMAIL = "nipa@codefx.org";
	public static final String GIT_PASSWORD = "********";

	public static Path createNewTempFileForResourceFile(String fileName) {
		try {
			Path directory = getPathToExistingResourceFile(fileName).getParent();
			return Files.createTempFile(directory, fileName, "");
		} catch (IOException ex) {
			throw new RuntimeIOException(ex);
		}
	}

	public static Path getPathToExistingResourceFile(String fileName) {
		return getPathToResourceFile(fileName)
				.orElseThrow(() -> new IllegalArgumentException(format("No resource file '%s' was found.", fileName)));
	}

	public static Optional<Path> getPathToResourceFile(String fileName) {
		return Optional
				.ofNullable(Util.class.getClassLoader().getResource(fileName))
				.map(URL::getPath)
				.map(Paths::get);
	}

	public static <T> Collector<T, ?, ImmutableSet<T>> toImmutableSet() {
		// TODO create more performant implementation using the builder
		return Collectors.collectingAndThen(toSet(), ImmutableSet::copyOf);
	}

	public static <P, T> List<P> transformToList(Collection<T> collection, Function<T, P> transform) {
		return transform(collection, transform, toList());
	}

	public static <P, T> ImmutableSet<P> transformToImmutableSet(Collection<T> collection, Function<T, P> transform) {
		return transform(collection, transform, toImmutableSet());
	}

	public static <P, T, C extends Collection<P>> C transform(
			Collection<T> collection,
			Function<T, P> transform,
			Collector<P, ?, C> collector) {
		return collection == null
				? Stream.<P>empty().collect(collector)
				: collection.stream().map(transform).collect(collector);
	}

	public static ByteArrayInputStream asInputStream(String string) {
		return new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
	}

	public static YamlAnalysisPersistence createYamlPersistence(Path resultFile) throws IOException {
		return YamlAnalysisPersistence.fromStream(Files.newInputStream(resultFile));
	}


}
