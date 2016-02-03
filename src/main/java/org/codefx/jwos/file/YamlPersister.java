package org.codefx.jwos.file;// NOT_PUBLISHED

import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.codefx.jwos.file.persistence.PersistentArtifactCoordinates;
import org.codefx.jwos.file.persistence.PersistentResolvedArtifact;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.util.function.Function;

class YamlPersister {

	// TD = Type Description
	
	private static final TypeDescription ARTIFACT_TD =
			new TypeDescription(PersistentArtifactCoordinates.class, "!artifact");
	private static final TypeDescription RESOLVED_ARTIFACT_TD =
			new TypeDescription(PersistentResolvedArtifact.class, "!resolved_artifact");

	private final Representer representer;

	YamlPersister() {
		representer = createRepresenter();
	}

	private static Representer createRepresenter() {
		Representer representer = new Representer();
		representer.addClassTag(ARTIFACT_TD.getType(), ARTIFACT_TD.getTag());
		representer.addClassTag(RESOLVED_ARTIFACT_TD.getType(), RESOLVED_ARTIFACT_TD.getTag());
		return representer;
	}

	private <T, P> String write(T element, Function<T, P> persistentWrapper) {
		return new Yaml(representer).dump(persistentWrapper.apply(element));
	}

	private <P, T> T read(String yamlString, Class<P> persistenceType, Function<P, T> persistentUnwrapper) {
		Constructor constructor = createConstructorWithTypeDescriptors(persistenceType);
		Yaml yaml = new Yaml(constructor, representer, new DumperOptions());
		P loaded = yaml.loadAs(yamlString, persistenceType);
		return persistentUnwrapper.apply(loaded);
	}

	private <P> Constructor createConstructorWithTypeDescriptors(Class<P> persistenceType) {
		Constructor constructor = new Constructor();
		constructor.addTypeDescription(ARTIFACT_TD);
		constructor.addTypeDescription(RESOLVED_ARTIFACT_TD);
		return constructor;
	}

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

}
