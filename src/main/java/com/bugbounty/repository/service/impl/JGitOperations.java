package com.bugbounty.repository.service.impl;

import com.bugbounty.repository.service.GitOperations;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.NullOutputStream;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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

    @Override
    public String getCommitDiff(String localPath, String commitId) throws IOException, GitAPIException {
        try (Git git = openRepository(localPath);
             RevWalk walk = new RevWalk(git.getRepository());
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             DiffFormatter diffFormatter = new DiffFormatter(outputStream)) {
            
            ObjectId commitObjectId = git.getRepository().resolve(commitId);
            if (commitObjectId == null) {
                throw new IllegalArgumentException("Commit not found: " + commitId);
            }
            
            RevCommit commit = walk.parseCommit(commitObjectId);
            
            // Get parent commit if it exists
            RevCommit parent = null;
            if (commit.getParentCount() > 0) {
                parent = walk.parseCommit(commit.getParent(0));
            }
            
            diffFormatter.setRepository(git.getRepository());
            diffFormatter.setDetectRenames(true);
            
            if (parent != null) {
                // Compare with parent
                try (RevWalk revWalk = new RevWalk(git.getRepository())) {
                    RevTree parentTree = revWalk.parseTree(parent.getTree());
                    RevTree commitTree = revWalk.parseTree(commit.getTree());
                    
                    List<org.eclipse.jgit.diff.DiffEntry> diffEntries = diffFormatter.scan(parentTree, commitTree);
                    for (org.eclipse.jgit.diff.DiffEntry diffEntry : diffEntries) {
                        diffFormatter.format(diffEntry);
                    }
                }
            } else {
                // Initial commit - show all files
                try (RevWalk revWalk = new RevWalk(git.getRepository())) {
                    RevTree commitTree = revWalk.parseTree(commit.getTree());
                    
                    List<org.eclipse.jgit.diff.DiffEntry> diffEntries = diffFormatter.scan(null, commitTree);
                    for (org.eclipse.jgit.diff.DiffEntry diffEntry : diffEntries) {
                        diffFormatter.format(diffEntry);
                    }
                }
            }
            
            diffFormatter.flush();
            return outputStream.toString();
        }
    }
}

