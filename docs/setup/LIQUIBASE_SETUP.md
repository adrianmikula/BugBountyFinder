# Liquibase Database Migration Setup

This project uses [Liquibase](https://www.liquibase.org/) to manage database schema changes in a controlled and versioned manner.

## Overview

Liquibase tracks all database changes in XML changelog files, allowing you to:
- Version control your database schema
- Apply changes consistently across environments
- Rollback changes if needed
- Track who made changes and when

## Directory Structure

```
src/main/resources/
└── db/
    └── changelog/
        ├── db.changelog-master.xml    # Master changelog file
        └── changes/
            └── 001-initial-schema.xml # Individual change sets
```

## How It Works

1. **On Application Startup**: Spring Boot automatically runs Liquibase migrations
2. **Change Tracking**: Liquibase maintains a `databasechangelog` table to track applied changes
3. **Idempotent**: Changes are only applied once, even if you restart the application

## Creating New Migrations

### Step 1: Create a New Changelog File

Create a new file in `src/main/resources/db/changelog/changes/` following the naming convention:
- `002-add-user-table.xml`
- `003-add-index-to-bounties.xml`
- etc.

### Step 2: Add Your Changes

Example: Adding a new column to the bounties table

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.24.xsd">

    <changeSet id="004-add-priority-to-bounties" author="your-name">
        <addColumn tableName="bounties">
            <column name="priority" type="INTEGER" defaultValue="0">
                <constraints nullable="false"/>
            </column>
        </addColumn>
    </changeSet>

</databaseChangeLog>
```

### Step 3: Include in Master Changelog

Add the new changelog file to `db.changelog-master.xml`:

```xml
<include file="db/changelog/changes/002-add-user-table.xml"/>
```

## Common ChangeSet Operations

### Add a Column
```xml
<addColumn tableName="bounties">
    <column name="new_column" type="VARCHAR(255)"/>
</addColumn>
```

### Create a Table
```xml
<createTable tableName="users">
    <column name="id" type="UUID">
        <constraints primaryKey="true" nullable="false"/>
    </column>
    <column name="username" type="VARCHAR(100)">
        <constraints nullable="false" unique="true"/>
    </column>
</createTable>
```

### Create an Index
```xml
<createIndex indexName="idx_username" tableName="users">
    <column name="username"/>
</createIndex>
```

### Modify a Column
```xml
<modifyDataType tableName="bounties" columnName="description" newDataType="TEXT"/>
```

### Drop a Column
```xml
<dropColumn tableName="bounties" columnName="old_column"/>
```

## Best Practices

1. **One ChangeSet Per Logical Change**: Each changeSet should represent a single, logical change
2. **Descriptive IDs**: Use clear, descriptive changeSet IDs (e.g., `004-add-priority-to-bounties`)
3. **Author Tracking**: Always include an author name for accountability
4. **Never Modify Applied Changes**: Once a changeSet has been applied, don't modify it. Create a new changeSet instead
5. **Test Locally First**: Always test migrations on a local database before deploying

## Running Migrations

### Automatic (Default)
Migrations run automatically when the Spring Boot application starts.

### Manual (Using Gradle)

You can also run Liquibase commands manually using the Liquibase CLI or Gradle plugin:

```bash
# Update database (applies pending changes)
./gradlew liquibaseUpdate

# Generate SQL without applying (dry-run)
./gradlew liquibaseUpdateSQL

# Rollback last change
./gradlew liquibaseRollback

# Check status
./gradlew liquibaseStatus
```

## Configuration

Liquibase is configured in `application.yml`:

```yaml
spring:
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
    enabled: true
    drop-first: false  # Never set to true in production!
```

## Troubleshooting

### Migration Fails on Startup

1. Check the application logs for the specific error
2. Verify your changelog XML syntax is correct
3. Ensure the database connection is working
4. Check if there are conflicting changes

### Need to Reset Database

**⚠️ WARNING: This will delete all data!**

```sql
-- Drop all tables
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```

Then restart the application to reapply all migrations.

### View Applied Changes

Query the Liquibase tracking table:

```sql
SELECT * FROM databasechangelog ORDER BY dateexecuted DESC;
```

## Resources

- [Liquibase Documentation](https://docs.liquibase.com/)
- [Liquibase Change Types](https://docs.liquibase.com/change-types/home.html)
- [Spring Boot Liquibase Integration](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.liquibase)

