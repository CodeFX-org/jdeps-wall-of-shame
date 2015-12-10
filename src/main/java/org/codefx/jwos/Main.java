package org.codefx.jwos;

import com.google.common.collect.ImmutableSet;
import org.codefx.jwos.MavenCentral.MavenArtifact;
import org.codefx.jwos.jdeps.JDeps;
import org.codefx.jwos.jdeps.dependency.Violation;
import org.eclipse.aether.artifact.DefaultArtifact;

public class Main {

	public static void main(String[] args) throws Exception {
		MavenArtifact artifact = new MavenCentral()
				.downloadMavenArtifact(new DefaultArtifact("com.facebook.jcommon:memory:0.1.21"));
		ImmutableSet<Violation> violations = new JDeps().analyze(artifact.path());
		System.out.println(violations);
	}

}
