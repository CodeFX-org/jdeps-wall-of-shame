package org.codefx.jwos;

import com.google.common.collect.ImmutableSet;
import org.codefx.jwos.maven.MavenCentral;
import org.codefx.jwos.maven.ResolvedArtifact;
import org.codefx.jwos.jdeps.JDeps;
import org.codefx.jwos.jdeps.dependency.Violation;
import org.eclipse.aether.artifact.DefaultArtifact;

public class Main {

	public static void main(String[] args) throws Exception {
		ResolvedArtifact artifact = new MavenCentral()
				.downloadMavenArtifact(new DefaultArtifact("com.facebook.jcommon:memory:0.1.21"));
		ImmutableSet<Violation> violations = new JDeps().analyze(artifact.path());
		System.out.println(violations);
	}

}
