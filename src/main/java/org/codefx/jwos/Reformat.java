package org.codefx.jwos;

import org.codefx.jwos.file.ResultFile;
import org.codefx.jwos.file.WallOfShame;

import java.io.IOException;

public class Reformat {

	public static void main(String[] args) throws IOException {
		ResultFile existingResults = ResultFile.read(Util.getPathToResourceFile(Util.RESULT_FILE_NAME));

		WallOfShame wall = WallOfShame.openExistingDirectoryWithDefaults(
				Util.GIT_REPOSITORY_URL, Util.PAGES_DIRECTORY, Util.GIT_USER_NAME, Util.GIT_PASSWORD, Util.GIT_EMAIL);
		wall.addArtifacts(existingResults.analyzedArtifactsUnmodifiable().stream());

		wall.write();
	}

}
