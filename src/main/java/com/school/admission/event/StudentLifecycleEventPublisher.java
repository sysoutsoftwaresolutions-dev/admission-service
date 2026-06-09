package com.school.admission.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.core.common.context.TenantContext;
import com.core.common.event.OutboxPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publisher component for sending student lifecycle events via the database transactional outbox.
 */
@Component
public class StudentLifecycleEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(StudentLifecycleEventPublisher.class);
    private final OutboxPublisher outboxPublisher;
    private final ObjectMapper objectMapper;

    public StudentLifecycleEventPublisher(OutboxPublisher outboxPublisher, ObjectMapper objectMapper) {
        this.outboxPublisher = outboxPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes a StudentCreated event to the database outbox.
     *
     * @param payload       the student payload details
     * @param correlationId the correlation identifier for trace tracking
     * @param operatorId    the identity of the actor performing the operation
     */
    public void publishStudentCreated(StudentCreatedEventPayload payload, String correlationId, String operatorId) {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            tenantId = "default";
        }

        EventEnvelope<StudentCreatedEventPayload> envelope = new EventEnvelope<>(
                correlationId != null ? correlationId : UUID.randomUUID().toString(),
                "StudentCreated",
                "v1",
                tenantId,
                operatorId != null ? operatorId : "system",
                payload
        );

        try {
            String jsonPayload = objectMapper.writeValueAsString(envelope);
            String topic = "admission-student-lifecycle";

            log.info("[StudentLifecycleEventPublisher] Enqueuing StudentCreated event to Outbox. EventId: {}, TenantId: {}", 
                    envelope.getEventId(), tenantId);

            outboxPublisher.enqueue(envelope.getEventId(), topic, jsonPayload, tenantId);
        } catch (Exception e) {
            log.error("[StudentLifecycleEventPublisher] Error serializing EventEnvelope for student ID: {}", payload.getStudentId(), e);
            throw new RuntimeException("Failed to enqueue StudentCreated event to outbox", e);
        }
    }
}
