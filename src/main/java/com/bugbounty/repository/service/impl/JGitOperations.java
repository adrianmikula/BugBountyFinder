package com.bugbounty.repository.service.impl;

import com.bugbounty.repository.service.GitOperations;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Component
@Slf4j
public class JGitOperations implements GitOperations {

    @Override
    public Git cloneRepository(String url, String localPath) throws GitAPIException {
        try {
            log.debug("Cloning repository from {} to {}", url, localPath);
            
            // Create parent directory if it doesn't exist
            Path path = Paths.get(localPath);
            Files.createDirectories(path.getParent());
            
            Git git = Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(new File(localPath))
                    .setCloneAllBranches(false)
                    .call();
            
            log.debug("Repository cloned successfully");
            return git;
        } catch (GitAPIException e) {
            log.error("Failed to clone repository: {}", url, e);
            throw e;
        } catch (IOException e) {
            log.error("Failed to clone repository: {}", url, e);
            throw new RuntimeException("Clone failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to clone repository: {}", url, e);
            throw new RuntimeException("Clone failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Git openRepository(String localPath) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder
                .setGitDir(new File(localPath, ".git"))
                .readEnvironment()
                .findGitDir()
                .build();
        
        return new Git(repository);
    }

    @Override
    public PullResult pull(Git git) throws GitAPIException {
        try {
            return git.pull().call();
        } catch (GitAPIException e) {
            log.error("Failed to pull repository", e);
            throw e;
        } catch (Exception e) {
            log.error("Failed to pull repository", e);
            throw new RuntimeException("Pull failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String readFile(String localPath, String filePath) throws IOException {
        Path fullPath = Paths.get(localPath, filePath);
        return Files.readString(fullPath);
    }

    @Override
    public String[] listFiles(String localPath, String directoryPath) throws IOException {
        Path fullPath = Paths.get(localPath, directoryPath);
        
        if (!Files.exists(fullPath) || !Files.isDirectory(fullPath)) {
            return new String[0];
        }
        
        try (Stream<Path> paths = Files.walk(fullPath)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(path -> fullPath.relativize(path).toString())
                    .toArray(String[]::new);
        }
    }
}

