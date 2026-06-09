package com.school.admission.migration;

import io.mongock.api.annotations.BeforeExecution;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackBeforeExecution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.bson.Document;

/**
 * Mongock migration class that configures collections and compound indexes for the Admission Service database.
 */
@ChangeUnit(id = "admission-migration-init", order = "001", author = "antigravity")
public class DatabaseInitMigration {

    @BeforeExecution
    public void beforeExecution() {}

    @RollbackBeforeExecution
    public void rollbackBeforeExecution() {}

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        // Create indexes on student_profiles
        IndexOperations studentOps = mongoTemplate.indexOps("student_profiles");
        studentOps.ensureIndex(new Index().on("tenantId", org.springframework.data.domain.Sort.Direction.ASC));
        
        // Compound unique index for tenantId + rollNumber
        Document keys = new Document("tenantId", 1).append("rollNumber", 1);
        studentOps.ensureIndex(new CompoundIndexDefinition(keys).unique());
        
        studentOps.ensureIndex(new Index().on("status", org.springframework.data.domain.Sort.Direction.ASC));

        // Create indexes on admission_applications
        IndexOperations appOps = mongoTemplate.indexOps("admission_applications");
        appOps.ensureIndex(new Index().on("tenantId", org.springframework.data.domain.Sort.Direction.ASC));
        appOps.ensureIndex(new Index().on("status", org.springframework.data.domain.Sort.Direction.ASC));
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps("student_profiles").dropAllIndexes();
        mongoTemplate.indexOps("admission_applications").dropAllIndexes();
    }
}
