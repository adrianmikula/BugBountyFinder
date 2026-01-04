package com.bugbounty.repository.service.impl;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JGitOperations Tests")
class JGitOperationsTest {

    private JGitOperations gitOperations;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        gitOperations = new JGitOperations();
    }

    @Test
    @DisplayName("Should read file content successfully")
    void shouldReadFileContent() throws IOException {
        // Given
        String testContent = "This is test file content\nLine 2\nLine 3";
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, testContent);

        // When
        String content = gitOperations.readFile(tempDir.toString(), "test.txt");

        // Then
        assertEquals(testContent, content);
    }

    @Test
    @DisplayName("Should throw IOException when file does not exist")
    void shouldThrowIOExceptionWhenFileNotFound() {
        // When & Then
        assertThrows(IOException.class, () -> {
            gitOperations.readFile(tempDir.toString(), "nonexistent.txt");
        });
    }

    @Test
    @DisplayName("Should list files in directory")
    void shouldListFilesInDirectory() throws IOException {
        // Given
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("file1.txt"), "content1");
        Files.writeString(subDir.resolve("file2.txt"), "content2");
        Files.writeString(subDir.resolve("file3.java"), "content3");

        // When
        String[] files = gitOperations.listFiles(tempDir.toString(), "subdir");

        // Then
        assertNotNull(files);
        assertEquals(3, files.length);
        // Files should be relative to subdir
        assertTrue(java.util.Arrays.asList(files).contains("file1.txt"));
        assertTrue(java.util.Arrays.asList(files).contains("file2.txt"));
        assertTrue(java.util.Arrays.asList(files).contains("file3.java"));
    }

    @Test
    @DisplayName("Should return empty array when directory does not exist")
    void shouldReturnEmptyArrayWhenDirectoryNotFound() throws IOException {
        // When
        String[] files = gitOperations.listFiles(tempDir.toString(), "nonexistent");

        // Then
        assertNotNull(files);
        assertEquals(0, files.length);
    }

    @Test
    @DisplayName("Should return empty array when path is not a directory")
    void shouldReturnEmptyArrayWhenPathIsNotDirectory() throws IOException {
        // Given
        Path testFile = tempDir.resolve("file.txt");
        Files.writeString(testFile, "content");

        // When
        String[] files = gitOperations.listFiles(tempDir.toString(), "file.txt");

        // Then
        assertNotNull(files);
        assertEquals(0, files.length);
    }

    @Test
    @DisplayName("Should list files recursively in nested directories")
    void shouldListFilesRecursively() throws IOException {
        // Given
        Path subDir = tempDir.resolve("subdir");
        Path nestedDir = subDir.resolve("nested");
        Files.createDirectories(nestedDir);
        Files.writeString(subDir.resolve("file1.txt"), "content1");
        Files.writeString(nestedDir.resolve("file2.txt"), "content2");

        // When
        String[] files = gitOperations.listFiles(tempDir.toString(), "subdir");

        // Then
        assertNotNull(files);
        assertEquals(2, files.length, "Should find 2 files: file1.txt and nested/file2.txt");
        // Files are returned as relative paths from the directoryPath (subdir)
        // So file1.txt is at subdir/file1.txt relative to tempDir, but relative to subdir it's just file1.txt
        // And nested/file2.txt is at subdir/nested/file2.txt relative to tempDir, but relative to subdir it's nested/file2.txt
        java.util.List<String> fileList = java.util.Arrays.asList(files);
        assertTrue(fileList.contains("file1.txt"), "Should contain file1.txt (relative to subdir)");
        assertTrue(fileList.contains("nested/file2.txt"), "Should contain nested/file2.txt (relative to subdir)");
    }

    @Test
    @DisplayName("Should handle empty directory")
    void shouldHandleEmptyDirectory() throws IOException {
        // Given
        Path emptyDir = tempDir.resolve("empty");
        Files.createDirectories(emptyDir);

        // When
        String[] files = gitOperations.listFiles(tempDir.toString(), "empty");

        // Then
        assertNotNull(files);
        assertEquals(0, files.length);
    }
}

