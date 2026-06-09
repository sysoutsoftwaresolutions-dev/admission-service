package com.school.admission.repository;

import com.school.admission.model.AdmissionApplication;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
import java.util.List;

public interface AdmissionApplicationRepository extends MongoRepository<AdmissionApplication, String> {
    Optional<AdmissionApplication> findByTenantIdAndId(String tenantId, String id);
    List<AdmissionApplication> findByTenantId(String tenantId);
}
