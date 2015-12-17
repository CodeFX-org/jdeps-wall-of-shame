package org.codefx.jwos;

import com.google.common.collect.ImmutableList;
import org.codefx.jwos.analysis.AnalysisFunctions;
import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.DeeplyAnalyzedArtifact;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.codefx.jwos.connect.BlockingReceiver;
import org.codefx.jwos.connect.BlockingSender;
import org.codefx.jwos.connect.Sink;
import org.codefx.jwos.connect.Source;
import org.codefx.jwos.connect.Transformer;
import org.codefx.jwos.connect.TransformerToMany;
import org.codefx.jwos.jdeps.JDeps;
import org.codefx.jwos.maven.MavenCentral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.codefx.jwos.connect.Log.log;
import static org.codefx.jwos.connect.ThrowingConsumer.ignore;

public class Main {

	// TODO: observe the queues and create statistics

	private final BlockingQueue<ArtifactCoordinates> mustAddToAnalyse;
	private final BlockingQueue<ArtifactCoordinates> mustResolve;
	private final BlockingQueue<ResolvedArtifact> mustAnalyze;
	private final BlockingQueue<AnalyzedArtifact> mustDeeplyAnalyze;
	private final BlockingQueue<DeeplyAnalyzedArtifact> mustFinish;

	// TODO: create these connections in 'run'

	private final Source<ArtifactCoordinates> fixedListOfArtifacts;
	private final TransformerToMany<ArtifactCoordinates, ArtifactCoordinates> addToAnalyze;
	private final Transformer<ArtifactCoordinates, ResolvedArtifact> resolve;
	private final Transformer<ResolvedArtifact, AnalyzedArtifact> analyze;
	private final TransformerToMany<AnalyzedArtifact, DeeplyAnalyzedArtifact> deeplyAnalyze;
	private final Sink<DeeplyAnalyzedArtifact> print;

	public Main() {
		mustAddToAnalyse = new ArrayBlockingQueue<>(5);
		mustResolve = new ArrayBlockingQueue<>(5);
		mustAnalyze = new ArrayBlockingQueue<>(5);
		mustDeeplyAnalyze = new ArrayBlockingQueue<>(5);
		mustFinish = new ArrayBlockingQueue<>(5);

		AnalysisFunctions analysis = new AnalysisFunctions();
		fixedListOfArtifacts = createFixedListOfArtifacts(mustAddToAnalyse::put);
		addToAnalyze = createAddToAnalyze(mustAddToAnalyse::take, mustResolve::put, analysis);
		resolve = createMavenResolver(mustResolve::take, mustAnalyze::put);
		analyze = createJDepsAnalyzer(mustAnalyze::take, mustDeeplyAnalyze::put);
		deeplyAnalyze = createDeepAnalyze(mustDeeplyAnalyze::take, mustFinish::put, analysis);
		print = createLogPrinter(mustFinish::take);
	}

	public void run() {
		ExecutorService pool = Executors.newFixedThreadPool(8);
		pool.execute(fixedListOfArtifacts::supply);
		pool.execute(addToAnalyze::transform);
		pool.execute(resolve::transform);
		pool.execute(analyze::transform);
		pool.execute(deeplyAnalyze::transform);
		pool.execute(print::consume);
	}

	private static Source<ArtifactCoordinates> createFixedListOfArtifacts(BlockingReceiver<ArtifactCoordinates> out) {
		Logger logger = LoggerFactory.getLogger("Fixed Artifact List");
		Iterator<ArtifactCoordinates> artifacts = ImmutableList
				.of(
						ArtifactCoordinates.from("com.facebook.jcommon", "memory", "0.1.21"))
				.iterator();
		return new Source<>(
				log(
						() -> artifacts.hasNext()
								? Optional.of(artifacts.next())
								: Optional.<ArtifactCoordinates>empty(),
						"Fetched %s.",
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
			BlockingSender<ArtifactCoordinates> in, BlockingReceiver<ResolvedArtifact> out) {
		Logger logger = LoggerFactory.getLogger("Maven Resolve");
		MavenCentral maven = new MavenCentral();
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
