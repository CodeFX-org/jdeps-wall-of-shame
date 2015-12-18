package org.codefx.jwos;

import com.google.common.collect.ImmutableList;
import org.codefx.jwos.analysis.AnalysisFunctions;
import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.DeeplyAnalyzedArtifact;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.codefx.jwos.connect.BlockingReceiver;
import org.codefx.jwos.connect.BlockingSender;
import org.codefx.jwos.connect.Sink;
import org.codefx.jwos.connect.Source;
import org.codefx.jwos.connect.Transformer;
import org.codefx.jwos.connect.TransformerToMany;
import org.codefx.jwos.discovery.ProjectListFile;
import org.codefx.jwos.jdeps.JDeps;
import org.codefx.jwos.maven.MavenCentral;
import org.eclipse.aether.version.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.codefx.jwos.connect.Log.log;
import static org.codefx.jwos.connect.ThrowingConsumer.ignore;

public class Main {

	private static final String[] PROJECT_LIST_FILE_NAMES = {"top100JavaLibrariesByTakipi.txt"};
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	// TODO: observe the queues and create statistics

	private final BlockingQueue<ProjectCoordinates> mustDetectVersions;
	private final BlockingQueue<ArtifactCoordinates> mustAddToAnalyse;
	private final BlockingQueue<ArtifactCoordinates> mustResolve;
	private final BlockingQueue<ResolvedArtifact> mustAnalyze;
	private final BlockingQueue<AnalyzedArtifact> mustDeeplyAnalyze;
	private final BlockingQueue<DeeplyAnalyzedArtifact> mustFinish;

	private final List<Source<ProjectCoordinates>> findProjects;
	private final List<TransformerToMany<ProjectCoordinates, ArtifactCoordinates>> detectVersions;
	private final List<TransformerToMany<ArtifactCoordinates, ArtifactCoordinates>> addToAnalyze;
	private final List<Transformer<ArtifactCoordinates, ResolvedArtifact>> resolve;
	private final List<Transformer<ResolvedArtifact, AnalyzedArtifact>> analyze;
	private final List<TransformerToMany<AnalyzedArtifact, DeeplyAnalyzedArtifact>> deeplyAnalyze;
	private final List<Sink<DeeplyAnalyzedArtifact>> finish;

	private final ExecutorService pool;
	private final AnalysisFunctions analysis;
	private final MavenCentral maven;

	public Main() {
		mustDetectVersions = new ArrayBlockingQueue<>(5);
		mustAddToAnalyse = new ArrayBlockingQueue<>(5);
		mustResolve = new ArrayBlockingQueue<>(5);
		mustAnalyze = new ArrayBlockingQueue<>(5);
		mustDeeplyAnalyze = new ArrayBlockingQueue<>(5);
		mustFinish = new ArrayBlockingQueue<>(5);

		findProjects = new ArrayList<>();
		detectVersions = new ArrayList<>();
		addToAnalyze = new ArrayList<>();
		resolve = new ArrayList<>();
		analyze = new ArrayList<>();
		deeplyAnalyze = new ArrayList<>();
		finish = new ArrayList<>();

		pool = Executors.newFixedThreadPool(16);
		analysis = new AnalysisFunctions();
		maven = new MavenCentral();
	}

	public void run() {
		createAllConnections();
		activateAllConnections();
	}

	private void createAllConnections() {
		createReadProjectFiles(mustDetectVersions::put).forEach(findProjects::add);
		detectVersions.add(createDetectVersions(mustDetectVersions::take, mustAddToAnalyse::put, maven));
		addToAnalyze.add(createAddToAnalyze(mustAddToAnalyse::take, mustResolve::put, analysis));
		resolve.add(createMavenResolver(mustResolve::take, mustAnalyze::put, maven));
		analyze.add(createJDepsAnalyzer(mustAnalyze::take, mustDeeplyAnalyze::put));
		deeplyAnalyze.add(createDeepAnalyze(mustDeeplyAnalyze::take, mustFinish::put, analysis));
		finish.add(createLogPrinter(mustFinish::take));
	}

	public void activateAllConnections() {
		Stream
				.<List<? extends Runnable>>of(
						findProjects, detectVersions, addToAnalyze, resolve, analyze, deeplyAnalyze, finish)
				.flatMap(List::stream)
				.forEach(pool::execute);
	}

	private static Stream<Source<ProjectCoordinates>> createReadProjectFiles(
			BlockingReceiver<ProjectCoordinates> out) {
		return Arrays
				.stream(PROJECT_LIST_FILE_NAMES)
				.map(fileName -> createReadProjectFile(fileName, out))
				.filter(Optional::isPresent)
				.map(Optional::get);
	}

	private static Optional<Source<ProjectCoordinates>> createReadProjectFile(
			String fileName, BlockingReceiver<ProjectCoordinates> out) {
		try {
			Logger logger = LoggerFactory.getLogger("Read Project List '" + fileName + "'");
			Path file = Paths.get(Main.class.getClassLoader().getResource(fileName).getPath());
			ProjectListFile listFile = new ProjectListFile(file).open();
			Source<ProjectCoordinates> source = new Source<>(
					log(
							listFile::readNextProject,
							"Fetched project %s from " + fileName,
							logger),
					out,
					logger);
			return Optional.of(source);
		} catch (IOException ex) {
			LOGGER.error("Opening '" + fileName + "' failed.", ex);
			return Optional.empty();
		}
	}

	private static TransformerToMany<ProjectCoordinates, ArtifactCoordinates> createDetectVersions(
			BlockingSender<ProjectCoordinates> in,
			BlockingReceiver<ArtifactCoordinates> out,
			MavenCentral maven) {
		Logger logger = LoggerFactory.getLogger("Add To Analyze");
		return new TransformerToMany<>(
				in,
				log(
						"Detecting versions of %s.",
						project -> {
							ImmutableList<Version> versions = maven.detectAllVersionsOf(project);
							return project.toArtifactsWithVersions(versions);
						},
						"Detected versions %s.",
						logger),
				out,
				logger);
	}

	private static TransformerToMany<ArtifactCoordinates, ArtifactCoordinates> createAddToAnalyze(
			BlockingSender<ArtifactCoordinates> in,
			BlockingReceiver<ArtifactCoordinates> out,
			AnalysisFunctions analysis) {
		Logger logger = LoggerFactory.getLogger("Add To Analyze");
		return new TransformerToMany<>(
				in,
				log(
						"Adding %s to analysis.",
						analysis.addToAnalyzeAsFunction(),
						"Retrieved %s for analysis.",
						logger),
				out,
				logger);
	}

	private static Transformer<ArtifactCoordinates, ResolvedArtifact> createMavenResolver(
			BlockingSender<ArtifactCoordinates> in, BlockingReceiver<ResolvedArtifact> out, MavenCentral maven) {
		Logger logger = LoggerFactory.getLogger("Maven Resolve");
		return new Transformer<>(
				in,
				log(
						"Downloading %s...",
						maven::downloadMavenArtifact,
						"Downloaded %s.",
						logger),
				out,
				logger);
	}

	private static Transformer<ResolvedArtifact, AnalyzedArtifact> createJDepsAnalyzer(
			BlockingSender<ResolvedArtifact> in, BlockingReceiver<AnalyzedArtifact> out) {
		Logger logger = LoggerFactory.getLogger("JDeps Analysis");
		JDeps jdeps = new JDeps();
		return new Transformer<>(
				in,
				log(
						"Analyzing %s...",
						art -> new AnalyzedArtifact(art.artifact(), jdeps.analyze(art.path()), art.dependencies()),
						"Analyzed %s.",
						logger),
				out,
				logger);
	}

	private static TransformerToMany<AnalyzedArtifact, DeeplyAnalyzedArtifact> createDeepAnalyze(
			BlockingSender<AnalyzedArtifact> in,
			BlockingReceiver<DeeplyAnalyzedArtifact> out,
			AnalysisFunctions analysis) {
		Logger logger = LoggerFactory.getLogger("Deep Analysis");
		return new TransformerToMany<>(
				in,
				log(
						"Deeply analyzing %s...",
						analysis.deepAnalyzeAsFunction(),
						"Deeply analyzed %s.",
						logger),
				out,
				logger);
	}

	private static Sink<DeeplyAnalyzedArtifact> createLogPrinter(BlockingSender<DeeplyAnalyzedArtifact> in) {
		Logger logger = LoggerFactory.getLogger("Output");
		return new Sink<>(in, log("Done with %s", ignore(), logger), logger);
	}

	public static void main(String[] args) throws Exception {
		Main main = new Main();
		main.run();
	}

}
