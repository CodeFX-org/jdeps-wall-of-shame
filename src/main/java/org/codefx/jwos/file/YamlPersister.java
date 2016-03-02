package org.codefx.jwos.file;// NOT_PUBLISHED

import com.google.common.collect.ImmutableSet;
import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.FailedArtifact;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.codefx.jwos.file.persistence.PersistentAnalyzedArtifact;
import org.codefx.jwos.file.persistence.PersistentArtifactCoordinates;
import org.codefx.jwos.file.persistence.PersistentFailedArtifact;
import org.codefx.jwos.file.persistence.PersistentProjectCoordinates;
import org.codefx.jwos.file.persistence.PersistentResolvedArtifact;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.util.function.Function;

import static com.google.common.collect.ImmutableSet.of;

class YamlPersister {

	// TD = Type Description

	private static final ImmutableSet<TypeDescription> TYPE_DESCRIPTIONS = of(
			new TypeDescription(PersistentProjectCoordinates.class, "!project"),
			new TypeDescription(PersistentArtifactCoordinates.class, "!artifact"),
			new TypeDescription(PersistentFailedArtifact.class, "!failed_artifact"),
			new TypeDescription(PersistentResolvedArtifact.class, "!resolved_artifact"),
			new TypeDescription(PersistentAnalyzedArtifact.class, "!analyzed_artifact")
	);

	private final Representer representer;

	YamlPersister() {
		representer = createRepresenter();
	}

	private static Representer createRepresenter() {
		Representer representer = new Representer();
		TYPE_DESCRIPTIONS
				.forEach(description -> representer.addClassTag(description.getType(), description.getTag()));
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
		TYPE_DESCRIPTIONS.forEach(constructor::addTypeDescription);
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

	String writeFailedArtifact(FailedArtifact artifact) {
		return write(artifact, PersistentFailedArtifact::from);
	}

	FailedArtifact readFailedArtifact(String yamlString) {
		return read(yamlString, PersistentFailedArtifact.class, PersistentFailedArtifact::toArtifact);
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
