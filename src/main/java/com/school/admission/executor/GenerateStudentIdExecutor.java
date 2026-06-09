package com.school.admission.executor;

import com.core.common.context.TenantContext;
import com.core.common.exception.CoreException;
import com.core.workflow.context.StepExecutionContext;
import com.core.workflow.executor.StepExecutor;
import com.core.workflow.model.StepResult;
import com.school.admission.model.AdmissionApplication;
import com.school.admission.model.StudentProfile;
import com.school.admission.repository.AdmissionApplicationRepository;
import com.school.admission.repository.StudentProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * StepExecutor that generates a unique Roll Number, creates a Student Profile, 
 * and approves the corresponding Admission Application.
 */
@Component("generateStudentIdExecutor")
public class GenerateStudentIdExecutor implements StepExecutor {

    private static final Logger log = LoggerFactory.getLogger(GenerateStudentIdExecutor.class);
    private final StudentProfileRepository studentProfileRepository;
    private final AdmissionApplicationRepository admissionApplicationRepository;

    public GenerateStudentIdExecutor(StudentProfileRepository studentProfileRepository,
                                     AdmissionApplicationRepository admissionApplicationRepository) {
        this.studentProfileRepository = studentProfileRepository;
        this.admissionApplicationRepository = admissionApplicationRepository;
    }

    @Override
    public StepResult execute(StepExecutionContext context) throws CoreException {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            tenantId = "default";
        }

        String applicationId = context.getVariable("applicationId", String.class);
        if (applicationId == null) {
            log.error("[GenerateStudentIdExecutor] Missing 'applicationId' in workflow variables");
            return StepResult.failure("Missing 'applicationId' variable in workflow context");
        }

        log.info("[GenerateStudentIdExecutor] Processing admission application. TenantId: {}, ApplicationId: {}", tenantId, applicationId);

        AdmissionApplication application = admissionApplicationRepository.findByTenantIdAndId(tenantId, applicationId)
                .orElseThrow(() -> new CoreException("Admission application not found: " + applicationId, "APPLICATION_NOT_FOUND"));

        if (!"SUBMITTED".equalsIgnoreCase(application.getStatus())) {
            log.warn("[GenerateStudentIdExecutor] Application is already processed. Status: {}", application.getStatus());
            return StepResult.failure("Application is already processed. Current status: " + application.getStatus());
        }

        // Generate student profile
        String studentId = UUID.randomUUID().toString();
        
        String rollNumber = null;
        boolean unique = false;
        int attempts = 0;
        while (!unique && attempts < 10) {
            attempts++;
            int randomDigits = (int) (Math.random() * 900000) + 100000;
            String candidate = "R-" + randomDigits;
            if (studentProfileRepository.findByTenantIdAndRollNumber(tenantId, candidate).isEmpty()) {
                rollNumber = candidate;
                unique = true;
            }
        }
        if (rollNumber == null) {
            rollNumber = "R-" + ((int) (Math.random() * 900000) + 100000) + "-" + (System.currentTimeMillis() % 1000);
        }

        StudentProfile profile = new StudentProfile(
                studentId,
                tenantId,
                application.getFirstName(),
                application.getLastName(),
                application.getEmail(),
                application.getGrade(),
                application.getSection(),
                rollNumber,
                "ACTIVE"
        );

        studentProfileRepository.save(profile);

        // Update application status
        application.setStatus("APPROVED");
        admissionApplicationRepository.save(application);

        log.info("[GenerateStudentIdExecutor] Student Profile created successfully. StudentId: {}, RollNumber: {}", studentId, rollNumber);

        // Put student details into the output context variables
        Map<String, Object> output = Map.of(
                "studentId", studentId,
                "rollNumber", rollNumber,
                "firstName", application.getFirstName(),
                "lastName", application.getLastName(),
                "email", application.getEmail(),
                "grade", application.getGrade(),
                "section", application.getSection()
        );

        return StepResult.success(output);
    }
}
