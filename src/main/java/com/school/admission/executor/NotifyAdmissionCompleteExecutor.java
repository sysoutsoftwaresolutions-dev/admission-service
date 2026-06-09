package com.school.admission.executor;

import com.core.common.exception.CoreException;
import com.core.workflow.context.StepExecutionContext;
import com.core.workflow.executor.StepExecutor;
import com.core.workflow.model.StepResult;
import com.school.admission.event.StudentCreatedEventPayload;
import com.school.admission.event.StudentLifecycleEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * StepExecutor that runs after profile creation to publish the StudentCreated event to Kafka.
 */
@Component("notifyAdmissionCompleteExecutor")
public class NotifyAdmissionCompleteExecutor implements StepExecutor {

    private static final Logger log = LoggerFactory.getLogger(NotifyAdmissionCompleteExecutor.class);
    private final StudentLifecycleEventPublisher eventPublisher;

    public NotifyAdmissionCompleteExecutor(StudentLifecycleEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public StepResult execute(StepExecutionContext context) throws CoreException {
        String studentId = context.getVariable("studentId", String.class);
        String firstName = context.getVariable("firstName", String.class);
        String lastName = context.getVariable("lastName", String.class);
        String email = context.getVariable("email", String.class);
        String grade = context.getVariable("grade", String.class);
        String section = context.getVariable("section", String.class);
        String rollNumber = context.getVariable("rollNumber", String.class);

        if (studentId == null || rollNumber == null) {
            log.error("[NotifyAdmissionCompleteExecutor] Missing studentId or rollNumber in context variables");
            return StepResult.failure("Missing required student information to trigger notification");
        }

        log.info("[NotifyAdmissionCompleteExecutor] Dispatched workflow completion. Publishing StudentCreated event for Roll: {}", rollNumber);

        StudentCreatedEventPayload payload = new StudentCreatedEventPayload(
                studentId, firstName, lastName, email, grade, section, rollNumber
        );

        // Generate trace tracking correlationId
        String correlationId = (String) context.getVariables().getOrDefault("correlationId", UUID.randomUUID().toString());
        String operatorId = (String) context.getVariables().getOrDefault("operatorId", "system");

        eventPublisher.publishStudentCreated(payload, correlationId, operatorId);

        return StepResult.success();
    }
}
