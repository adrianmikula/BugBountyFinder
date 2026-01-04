package com.bugbounty.repository.service;

import com.bugbounty.repository.domain.Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepositoryService {

    private final GitOperations gitOperations;

    public Repository cloneRepository(Repository repository, String basePath) throws Exception {
        log.info("Cloning repository: {} to {}", repository.getUrl(), basePath);
        
        if (repository.isCloned()) {
            log.debug("Repository already cloned: {}", repository.getLocalPath());
            return repository;
        }

        Path repoPath = Paths.get(basePath, repository.getOwner(), repository.getName());
        String localPath = repoPath.toString();

        try {
            gitOperations.cloneRepository(repository.getUrl(), localPath);
            repository.markAsCloned(localPath);
            log.info("Successfully cloned repository to: {}", localPath);
            return repository;
        } catch (Exception e) {
            log.error("Failed to clone repository: {}", repository.getUrl(), e);
            throw e;
        }
    }

    public boolean isCloned(Repository repository) {
        return repository.isCloned();
    }

    public void updateRepository(Repository repository) throws Exception {
        if (!repository.isCloned()) {
            throw new IllegalStateException("Repository not cloned: " + repository.getUrl());
        }

        log.debug("Updating repository: {}", repository.getLocalPath());
        var git = gitOperations.openRepository(repository.getLocalPath());
        gitOperations.pull(git);
        log.debug("Repository updated successfully");
    }

    public String getFileContent(Repository repository, String filePath) throws Exception {
        if (!repository.isCloned()) {
            throw new IllegalStateException("Repository not cloned: " + repository.getUrl());
        }

        return gitOperations.readFile(repository.getLocalPath(), filePath);
    }

    public String[] getFiles(Repository repository, String directoryPath) throws Exception {
        if (!repository.isCloned()) {
            throw new IllegalStateException("Repository not cloned: " + repository.getUrl());
        }

        return gitOperations.listFiles(repository.getLocalPath(), directoryPath);
    }

    public String getCommitDiff(Repository repository, String commitId) throws Exception {
        if (!repository.isCloned()) {
            throw new IllegalStateException("Repository not cloned: " + repository.getUrl());
        }

        return gitOperations.getCommitDiff(repository.getLocalPath(), commitId);
    }
}

