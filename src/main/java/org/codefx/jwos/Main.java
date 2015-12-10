package org.codefx.jwos;

import org.codefx.jwos.MavenCentral.MavenArtifact;
import org.eclipse.aether.artifact.DefaultArtifact;

public class Main {

	public static void main(String[] args) throws Exception {
		MavenArtifact artifact = new MavenCentral()
				.downloadMavenArtifact(new DefaultArtifact("org.apache.maven:maven-profile:2.2.1"));
		System.out.println(artifact);
	}

}
