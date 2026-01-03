package com.bugbounty.repository.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;

public interface GitOperations {
    Git cloneRepository(String url, String localPath) throws GitAPIException;
    Git openRepository(String localPath) throws IOException;
    PullResult pull(Git git) throws GitAPIException;
    String readFile(String localPath, String filePath) throws IOException;
    String[] listFiles(String localPath, String directoryPath) throws IOException;
}

