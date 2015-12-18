package org.codefx.jwos.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class GitDirectory {

	private final GitInformation information;
	private final Git repository;

	private GitDirectory(GitInformation information) throws IOException {
		this.information = requireNonNull(information, "The argument 'information' must not be null.");
		
		Path gitWorkTree = information.directory();
		if (!Files.isDirectory(gitWorkTree))
			throw new IllegalArgumentException(format("Specified Git work tree '%s' is no directory.", gitWorkTree));

		Repository repository = new FileRepositoryBuilder()
				.readEnvironment()
				.setMustExist(false)
				.setWorkTree(gitWorkTree.toFile())
				.build();
		this.repository = new Git(repository);
	}

	public static GitDirectory openExisting(GitInformation information) throws IOException {
		return new GitDirectory(information);
	}

	public void commitAll(String message) throws GitAPIException {
		repository.commit()
				.setAll(true)
				.setAuthor(information.author(), information.authorMail())
				.setCommitter(information.committer(), information.committerMail())
				.setMessage(message)
				.call();
	}
	
	public void push() throws GitAPIException {
		repository.push()
				.setRemote(information.remoteUrl())
				.setCredentialsProvider(
						new UsernamePasswordCredentialsProvider(information.userName(), information.password()))
				.call();
	}

}
