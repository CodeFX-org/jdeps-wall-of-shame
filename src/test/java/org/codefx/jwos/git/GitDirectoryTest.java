package org.codefx.jwos.git;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

import org.junit.Test;

public class GitDirectoryTest {

  private static final Path WORKING_TREE = Paths.get("/home/parlog/Code/others-nipa/jgit-test");
  private static final Path README = WORKING_TREE.resolve("README.md");
  private static final GitInformation INFO = GitInformation.simple(
      "git@github.com:CodeFX-org/jgit-test.git",
      WORKING_TREE,
      "--redactedUserName",
      "--redactedPassword",
      "nipa@codefx.org");

  @Test
  public void openExistingRepository() throws Exception {
    GitDirectory dir = GitDirectory.openExisting(INFO);
  }

  @Test
  public void commitFileChangeRepository() throws Exception {
    GitDirectory dir = GitDirectory.openExisting(INFO);
    appendToReadme(LocalDateTime.now().toString());
    dir.commitAll("Test Commit " + LocalDateTime.now());
  }

  private static void appendToReadme(String line) throws IOException {
    BufferedWriter writer = Files.newBufferedWriter(README, StandardOpenOption.APPEND);
    writer.append(line);
    writer.newLine();
    writer.flush();
  }

}
