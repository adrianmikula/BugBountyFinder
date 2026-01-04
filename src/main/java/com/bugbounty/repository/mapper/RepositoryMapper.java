package com.bugbounty.repository.mapper;

import com.bugbounty.repository.domain.Repository;
import com.bugbounty.repository.entity.RepositoryEntity;
import org.springframework.stereotype.Component;

@Component
public class RepositoryMapper {

    public RepositoryEntity toEntity(Repository domain) {
        if (domain == null) {
            return null;
        }

        return RepositoryEntity.builder()
                .id(domain.getId())
                .url(domain.getUrl())
                .owner(domain.getOwner())
                .name(domain.getName())
                .defaultBranch(domain.getDefaultBranch())
                .language(domain.getLanguage())
                .localPath(domain.getLocalPath())
                .build();
    }

    public Repository toDomain(RepositoryEntity entity) {
        if (entity == null) {
            return null;
        }

        return Repository.builder()
                .id(entity.getId())
                .url(entity.getUrl())
                .owner(entity.getOwner())
                .name(entity.getName())
                .defaultBranch(entity.getDefaultBranch())
                .language(entity.getLanguage())
                .localPath(entity.getLocalPath())
                .build();
    }
}

