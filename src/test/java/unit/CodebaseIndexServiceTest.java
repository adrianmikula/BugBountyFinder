package com.bugbounty.cve.service;

import com.bugbounty.cve.entity.CodebaseIndexEntity;
import com.bugbounty.cve.repository.CodebaseIndexRepository;
import com.bugbounty.repository.domain.Repository;
import com.bugbounty.repository.service.RepositoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CodebaseIndexService Tests")
class CodebaseIndexServiceTest {

    @Mock
    private CodebaseIndexRepository indexRepository;

    @Mock
    private RepositoryService repositoryService;

    @InjectMocks
    private CodebaseIndexService indexService;

    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Use reflection to set the objectMapper field
        try {
            java.lang.reflect.Field field = CodebaseIndexService.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(indexService, objectMapper);
            
            java.lang.reflect.Field basePathField = CodebaseIndexService.class.getDeclaredField("basePath");
            basePathField.setAccessible(true);
            basePathField.set(indexService, tempDir.toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to set fields", e);
        }
    }

    @Test
    @DisplayName("Should index Java repository")
    void shouldIndexJavaRepository() throws Exception {
        // Given
        Path javaFile = tempDir.resolve("Test.java");
        Files.writeString(javaFile, """
                package com.example;
                
                public class Test {
                    public void method() {
                    }
                }
                """);

        Repository repository = Repository.builder()
                .url("https://github.com/owner/repo")
                .build();
        repository.markAsCloned(tempDir.toString());

        when(indexRepository.findByRepositoryUrlAndLanguage(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(indexRepository.save(any(CodebaseIndexEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CodebaseIndexEntity result = indexService.indexRepository(repository, "Java");

        // Then
        assertNotNull(result);
        assertEquals("https://github.com/owner/repo", result.getRepositoryUrl());
        assertEquals("Java", result.getLanguage());
        assertNotNull(result.getIndexData());
        assertTrue(result.getIndexData().contains("Java"));
        verify(indexRepository, times(1)).save(any(CodebaseIndexEntity.class));
    }

    @Test
    @DisplayName("Should update existing index")
    void shouldUpdateExistingIndex() throws Exception {
        // Given
        Path javaFile = tempDir.resolve("Test.java");
        Files.writeString(javaFile, "public class Test {}");

        Repository repository = Repository.builder()
                .url("https://github.com/owner/repo")
                .build();
        repository.markAsCloned(tempDir.toString());

        CodebaseIndexEntity existing = CodebaseIndexEntity.builder()
                .id(java.util.UUID.randomUUID())
                .repositoryUrl("https://github.com/owner/repo")
                .language("Java")
                .indexVersion(1)
                .build();

        when(indexRepository.findByRepositoryUrlAndLanguage(anyString(), anyString()))
                .thenReturn(Optional.of(existing));
        when(indexRepository.save(any(CodebaseIndexEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CodebaseIndexEntity result = indexService.indexRepository(repository, "Java");

        // Then
        assertNotNull(result);
        assertEquals(2, result.getIndexVersion()); // Version incremented
        verify(indexRepository, times(1)).save(any(CodebaseIndexEntity.class));
    }

    @Test
    @DisplayName("Should return null if repository not cloned")
    void shouldReturnNullIfNotCloned() {
        // Given
        Repository repository = Repository.builder()
                .url("https://github.com/owner/repo")
                .build();
        // Not cloned

        // When
        CodebaseIndexEntity result = indexService.indexRepository(repository, "Java");

        // Then
        assertNull(result);
        verify(indexRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get index for repository and language")
    void shouldGetIndex() {
        // Given
        CodebaseIndexEntity entity = CodebaseIndexEntity.builder()
                .repositoryUrl("https://github.com/owner/repo")
                .language("Java")
                .indexData("{}")
                .build();

        when(indexRepository.findByRepositoryUrlAndLanguage("https://github.com/owner/repo", "Java"))
                .thenReturn(Optional.of(entity));

        // When
        Optional<CodebaseIndexEntity> result = indexService.getIndex("https://github.com/owner/repo", "Java");

        // Then
        assertTrue(result.isPresent());
        assertEquals("Java", result.get().getLanguage());
    }
}

