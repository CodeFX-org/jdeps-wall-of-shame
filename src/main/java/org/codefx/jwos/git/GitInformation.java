package org.codefx.jwos.git;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public class GitInformation {

	private final String remoteUrl;
	private final Path directory;
	
	private final String userName;
	private final String password;
	private final String author;
	private final String authorMail;
	private final String committer;
	private final String committerMail;

	private GitInformation(
			String remoteUrl, Path directory, String userName, String password,
			String author, String authorMail, String committer, String committerMail) {
		this.remoteUrl = requireNonNull(remoteUrl, "The argument 'remoteUrl' must not be null.");
		this.directory = requireNonNull(directory, "The argument 'directory' must not be null.");
		
		this.userName = requireNonNull(userName, "The argument 'userName' must not be null.");
		this.password = requireNonNull(password, "The argument 'password' must not be null.");
		this.author = requireNonNull(author, "The argument 'author' must not be null.");
		this.authorMail = requireNonNull(authorMail, "The argument 'authorMail' must not be null.");
		this.committer = requireNonNull(committer, "The argument 'committer' must not be null.");
		this.committerMail = requireNonNull(committerMail, "The argument 'committerMail' must not be null.");
	}
	
	public static GitInformation simple(
			String remoteUrl, Path directory, String userName, String password, String email) {
		return new GitInformation(remoteUrl, directory, userName, password, userName, email, userName, email);
	}

	String remoteUrl() {
		return remoteUrl;
	}

	Path directory() {
		return directory;
	}

	String userName() {
		return userName;
	}

	String password() {
		return password;
	}

	String author() {
		return author;
	}

	String authorMail() {
		return authorMail;
	}

	String committer() {
		return committer;
	}

	String committerMail() {
		return committerMail;
	}
}
