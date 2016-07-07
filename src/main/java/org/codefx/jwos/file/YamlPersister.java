package org.codefx.jwos.file;

import com.google.common.collect.ImmutableSet;
import org.codefx.jwos.Util;
import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.CompletedArtifact;
import org.codefx.jwos.artifact.FailedArtifact;
import org.codefx.jwos.artifact.FailedProject;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.codefx.jwos.artifact.ResolvedProject;
import org.codefx.jwos.file.persistence.PersistentAnalysis;
import org.codefx.jwos.file.persistence.PersistentAnalyzedArtifact;
import org.codefx.jwos.file.persistence.PersistentArtifactCoordinates;
import org.codefx.jwos.file.persistence.PersistentCompletedArtifact;
import org.codefx.jwos.file.persistence.PersistentFailedArtifact;
import org.codefx.jwos.file.persistence.PersistentFailedProject;
import org.codefx.jwos.file.persistence.PersistentProjectCoordinates;
import org.codefx.jwos.file.persistence.PersistentResolvedArtifact;
import org.codefx.jwos.file.persistence.PersistentResolvedProject;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.CollectionNode;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.InputStream;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableSet.of;
import static java.util.stream.StreamSupport.stream;

class YamlPersister {

	private static final ImmutableSet<TypeDescription> TYPE_DESCRIPTIONS = of(
			// Descriptors are only _necessary_ on the YAML's top-level type. If no type descriptor is defined here,
			// the fully qualified class name is used, which is fragile. So an explicit tag is defined for that class.
			// But _if_ tags are given, they are always used so by specifying tags for all types, the resulting file
			// gets very cluttered.
			new TypeDescription(PersistentAnalysis.class, "!persistent_analysis")
	);

	private final Representer representer;

	public YamlPersister() {
		representer = createRepresenter();
	}

	private static Representer createRepresenter() {
		Representer representer = new SkipEmptyRepresenter();
		TYPE_DESCRIPTIONS
				.forEach(description -> representer.addClassTag(description.getType(), description.getTag()));
		return representer;
	}

	// WRITE

	public <P> String write(P persistentElement) {
		return new Yaml(representer).dump(persistentElement);
	}

	private <T, P> String write(T element, Function<T, P> persistentWrapper) {
		return write(persistentWrapper.apply(element));
	}

	public <P> String writeAll(Stream<P> persistentElements) {
		return new Yaml(representer).dumpAll(persistentElements.iterator());
	}

	private <T, P> String writeAll(Stream<T> elements, Function<T, P> persistentWrapper) {
		return writeAll(elements.map(persistentWrapper));
	}

	// READ

	public <P> P read(String yamlString, Class<P> persistenceType) {
		return read(Util.asInputStream(yamlString), persistenceType);
	}

	public <P> P read(InputStream yamlStream, Class<P> persistenceType) {
		Yaml yaml = createYamlFromConstructorWithTypeDescriptors();
		return yaml.loadAs(yamlStream, persistenceType);
	}

	private Yaml createYamlFromConstructorWithTypeDescriptors() {
		Constructor constructor = new Constructor();
		TYPE_DESCRIPTIONS.forEach(constructor::addTypeDescription);
		return new Yaml(constructor, representer, new DumperOptions());
	}

	private <P, T> T read(String yamlString, Class<P> persistenceType, Function<P, T> persistentUnwrapper) {
		P loaded = read(yamlString, persistenceType);
		return persistentUnwrapper.apply(loaded);
	}

	public <P> Stream<P> readAll(String yamlString, Class<P> persistenceType) {
		Yaml yaml = createYamlFromConstructorWithTypeDescriptors();
		return stream(yaml.loadAll(yamlString).spliterator(), false)
				.map(persistenceType::cast);
	}

	private <P, T> Stream<T> readAll(String yamlString, Class<P> persistenceType, Function<P, T> persistentUnwrapper) {
		return readAll(yamlString, persistenceType)
				.map(persistentUnwrapper);
	}

	// PROJECTS

	String writeProject(ProjectCoordinates project) {
		return write(project, PersistentProjectCoordinates::from);
	}

	ProjectCoordinates readProject(String yamlString) {
		return read(yamlString, PersistentProjectCoordinates.class, PersistentProjectCoordinates::toProject);
	}

	String writeFailedProject(FailedProject project) {
		return write(project, PersistentFailedProject::from);
	}

	FailedProject readFailedProject(String yamlString) {
		return read(yamlString, PersistentFailedProject.class, PersistentFailedProject::toProject);
	}

	String writeResolvedProject(ResolvedProject project) {
		return write(project, PersistentResolvedProject::from);
	}

	ResolvedProject readResolvedProject(String yamlString) {
		return read(yamlString, PersistentResolvedProject.class, PersistentResolvedProject::toProject);
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

	String writeCompletedArtifacts(Collection<CompletedArtifact> artifact) {
		return writeCompletedArtifacts(artifact.stream());
	}

	String writeCompletedArtifacts(Stream<CompletedArtifact> artifact) {
		return writeAll(artifact, PersistentCompletedArtifact::from);
	}

	Stream<CompletedArtifact> readCompletedArtifacts(String yamlString) {
		Stream<PersistentCompletedArtifact> persistentArtifacts = readAll(yamlString, PersistentCompletedArtifact.class);
		return PersistentCompletedArtifact.toArtifacts(persistentArtifacts);
	}

	/**
	 * A representer that skips empty collections and null elements, so they do not clutter the output.
	 */
	private static class SkipEmptyRepresenter extends Representer {

		@Override
		protected NodeTuple representJavaBeanProperty(Object javaBean, Property property,
				Object propertyValue, Tag customTag) {
			NodeTuple tuple = super.representJavaBeanProperty(javaBean, property, propertyValue,
					customTag);
			Node valueNode = tuple.getValueNode();
			if (Tag.NULL.equals(valueNode.getTag())) {
				// skip 'null' values
				return null;
			}
			if (valueNode instanceof CollectionNode) {
				if (Tag.SEQ.equals(valueNode.getTag())) {
					SequenceNode seq = (SequenceNode) valueNode;
					if (seq.getValue().isEmpty()) {
						// skip empty lists
						return null;
					}
				}
				if (Tag.MAP.equals(valueNode.getTag())) {
					MappingNode seq = (MappingNode) valueNode;
					if (seq.getValue().isEmpty()) {
						// skip empty maps
						return null;
					}
				}
			}
			return tuple;
		}
	}

}
