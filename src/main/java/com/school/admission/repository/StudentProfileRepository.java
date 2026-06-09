package com.school.admission.repository;

import com.school.admission.model.StudentProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
import java.util.List;

public interface StudentProfileRepository extends MongoRepository<StudentProfile, String> {
    Optional<StudentProfile> findByTenantIdAndId(String tenantId, String id);
    Optional<StudentProfile> findByTenantIdAndRollNumber(String tenantId, String rollNumber);
    List<StudentProfile> findByTenantId(String tenantId);
}
