package com.school.admission.controller;

import com.core.common.context.TenantContext;
import com.core.common.exception.CoreException;
import com.core.common.security.SecuredWorkflow;
import com.core.workflow.context.StepExecutionContext;
import com.core.workflow.engine.StateMachineEngine;
import com.core.workflow.model.StateMachineDefinition;
import com.core.workflow.parser.StateMachineParser;
import com.school.admission.model.AdmissionApplication;
import com.school.admission.repository.AdmissionApplicationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller exposing REST endpoints for managing student admission applications.
 */
@RestController
@RequestMapping("/api/admissions")
@Tag(name = "Admission API", description = "Endpoints for managing student admission applications and processing approvals")
public class AdmissionApplicationController {

    private static final Logger log = LoggerFactory.getLogger(AdmissionApplicationController.class);

    private final AdmissionApplicationRepository applicationRepository;
    private final StateMachineEngine stateMachineEngine;
    private final StateMachineParser stateMachineParser;
    private final com.core.workflow.registry.WorkflowRegistry workflowRegistry;

    @Value("classpath:workflows/admission-workflow.json")
    private Resource workflowResource;

    private StateMachineDefinition admissionWorkflowDefinition;

    public AdmissionApplicationController(AdmissionApplicationRepository applicationRepository,
                                            StateMachineEngine stateMachineEngine,
                                            StateMachineParser stateMachineParser,
                                            com.core.workflow.registry.WorkflowRegistry workflowRegistry) {
        this.applicationRepository = applicationRepository;
        this.stateMachineEngine = stateMachineEngine;
        this.stateMachineParser = stateMachineParser;
        this.workflowRegistry = workflowRegistry;
    }

    @PostConstruct
    public void init() {
        try (InputStream is = workflowResource.getInputStream()) {
            this.admissionWorkflowDefinition = stateMachineParser.parse(is);
            this.workflowRegistry.register(this.admissionWorkflowDefinition);
            log.info("[AdmissionApplicationController] Parsed, registered, and loaded admission-workflow successfully.");
        } catch (Exception e) {
            log.error("[AdmissionApplicationController] Failed to load admission-workflow.json", e);
            throw new IllegalStateException("Failed to load admission-workflow.json", e);
        }
    }

    @PostMapping("/apply")
    @Operation(summary = "Submit a new student admission application", description = "Creates an admission application in SUBMITTED state.")
    @ApiResponse(responseCode = "200", description = "Application submitted successfully")
    public ResponseEntity<AdmissionApplication> apply(@RequestBody AdmissionApplication application) {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }
        application.setTenantId(tenantId);
        application.setStatus("SUBMITTED");
        application.setId(UUID.randomUUID().toString());

        AdmissionApplication saved = applicationRepository.save(application);
        log.info("[AdmissionAPI] Submitted application for {} {} under tenant {}", 
                saved.getFirstName(), saved.getLastName(), tenantId);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/applications/{id}/approve")
    @SecuredWorkflow({"ADMIN", "STAFF"})
    @org.springframework.transaction.annotation.Transactional
    @Operation(summary = "Approve an admission application", description = "Triggers the state machine workflow to generate a student profile and roll number.")
    @ApiResponse(responseCode = "200", description = "Application approved and student created successfully")
    public ResponseEntity<Map<String, Object>> approve(
            @PathVariable("id") String applicationId,
            @RequestHeader(value = "X-Operator-ID", defaultValue = "system") String operatorId) throws CoreException {

        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }

        log.info("[AdmissionAPI] Approving application. TenantId: {}, ApplicationId: {}, OperatorId: {}", 
                tenantId, applicationId, operatorId);

        // Fetch application to ensure it exists
        AdmissionApplication application = applicationRepository.findByTenantIdAndId(tenantId, applicationId)
                .orElseThrow(() -> new CoreException("Admission application not found: " + applicationId, "APPLICATION_NOT_FOUND"));

        if (!"SUBMITTED".equalsIgnoreCase(application.getStatus())) {
            throw new CoreException("Application is already processed. Current status: " + application.getStatus(), "APPLICATION_ALREADY_PROCESSED");
        }

        // Trigger workflow engine execution
        Map<String, Object> variables = new HashMap<>();
        variables.put("applicationId", applicationId);
        variables.put("correlationId", UUID.randomUUID().toString());
        variables.put("operatorId", operatorId);

        StepExecutionContext resultContext = stateMachineEngine.execute(admissionWorkflowDefinition, variables);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Application approved successfully");
        response.put("studentId", resultContext.getVariable("studentId"));
        response.put("rollNumber", resultContext.getVariable("rollNumber"));
        response.put("status", "APPROVED");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/applications")
    @SecuredWorkflow({"ADMIN", "STAFF"})
    @Operation(summary = "Get all admission applications", description = "Retrieve list of all applications for the current tenant.")
    public ResponseEntity<List<AdmissionApplication>> getApplications() {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }
        return ResponseEntity.ok(applicationRepository.findByTenantId(tenantId));
    }
}
