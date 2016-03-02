package org.codefx.jwos.file;// NOT_PUBLISHED

import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.codefx.jwos.file.persistence.PersistentAnalyzedArtifact;
import org.codefx.jwos.file.persistence.PersistentArtifactCoordinates;
import org.codefx.jwos.file.persistence.PersistentProjectCoordinates;
import org.codefx.jwos.file.persistence.PersistentResolvedArtifact;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.util.function.Function;

class YamlPersister {

	// TD = Type Description

	private static final TypeDescription PROJECT_TD =
			new TypeDescription(PersistentProjectCoordinates.class, "!project");
	private static final TypeDescription ARTIFACT_TD =
			new TypeDescription(PersistentArtifactCoordinates.class, "!artifact");
	private static final TypeDescription RESOLVED_ARTIFACT_TD =
			new TypeDescription(PersistentResolvedArtifact.class, "!resolved_artifact");
	private static final TypeDescription ANALYZED_ARTIFACT_TD =
			new TypeDescription(PersistentAnalyzedArtifact.class, "!analyzed_artifact");

	private final Representer representer;

	YamlPersister() {
		representer = createRepresenter();
	}

	private static Representer createRepresenter() {
		Representer representer = new Representer();
		representer.addClassTag(PROJECT_TD.getType(), PROJECT_TD.getTag());
		representer.addClassTag(ARTIFACT_TD.getType(), ARTIFACT_TD.getTag());
		representer.addClassTag(RESOLVED_ARTIFACT_TD.getType(), RESOLVED_ARTIFACT_TD.getTag());
		representer.addClassTag(ANALYZED_ARTIFACT_TD.getType(), ANALYZED_ARTIFACT_TD.getTag());
		return representer;
	}

	private <T, P> String write(T element, Function<T, P> persistentWrapper) {
		return new Yaml(representer).dump(persistentWrapper.apply(element));
	}

	private <P, T> T read(String yamlString, Class<P> persistenceType, Function<P, T> persistentUnwrapper) {
		Constructor constructor = createConstructorWithTypeDescriptors();
		Yaml yaml = new Yaml(constructor, representer, new DumperOptions());
		P loaded = yaml.loadAs(yamlString, persistenceType);
		return persistentUnwrapper.apply(loaded);
	}

	private Constructor createConstructorWithTypeDescriptors() {
		Constructor constructor = new Constructor();
		constructor.addTypeDescription(PROJECT_TD);
		constructor.addTypeDescription(ARTIFACT_TD);
		constructor.addTypeDescription(RESOLVED_ARTIFACT_TD);
		constructor.addTypeDescription(ANALYZED_ARTIFACT_TD);
		return constructor;
	}

	// PROJECTS

	String writeProject(ProjectCoordinates project) {
		return write(project, PersistentProjectCoordinates::from);
	}

	ProjectCoordinates readProject(String yamlString) {
		return read(yamlString, PersistentProjectCoordinates.class, PersistentProjectCoordinates::toProject);
	}

	// ARTIFACTS
	
	String writeArtifact(ArtifactCoordinates artifact) {
		return write(artifact, PersistentArtifactCoordinates::from);
	}

	ArtifactCoordinates readArtifact(String yamlString) {
		return read(yamlString, PersistentArtifactCoordinates.class, PersistentArtifactCoordinates::toArtifact);
	}

	String writeResolvedArtifact(ResolvedArtifact artifact) {
		return write(artifact, PersistentResolvedArtifact::from);
	}

	ResolvedArtifact readResolvedArtifact(String yamlString) {
		return read(yamlString, PersistentResolvedArtifact.class, PersistentResolvedArtifact::toArtifact);
	}

	String writeAnalyzedArtifact(AnalyzedArtifact artifact) {
		return write(artifact, PersistentAnalyzedArtifact::from);
	}

	AnalyzedArtifact readAnalyzedArtifact(String yamlString) {
		return read(yamlString, PersistentAnalyzedArtifact.class, PersistentAnalyzedArtifact::toArtifact);
	}

}
