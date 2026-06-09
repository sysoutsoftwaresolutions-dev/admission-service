package com.school.admission;

import com.core.common.context.TenantContext;
import com.core.common.security.JwtTokenParser;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Admission Service verifying OpenAPI docs, security blocks, 
 * and dynamic database routing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "mongock.enabled=false"
})
public class AdmissionServiceApplicationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MongoDatabaseFactory mongoDatabaseFactory;

    @Autowired
    private JwtTokenParser jwtTokenParser;

    @MockBean
    private MongoClient mongoClient;

    @MockBean
    private com.core.common.event.OutboxPublisher outboxPublisher;

    @Test
    public void contextLoads() {
        assertNotNull(jwtTokenParser);
    }

    @Test
    public void testOpenApiEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/v3/api-docs", String.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("openapi"));
        assertTrue(response.getBody().contains("Admission API"));
    }

    @Test
    public void testSecuredWorkflowAspectDeny() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", "schoola");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/admissions/applications/any-id/approve",
                HttpMethod.POST,
                entity,
                String.class
        );

        // Should be rejected as Unauthorized/Error
        assertTrue(response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError());
    }

    @Test
    public void testDatabaseRouting() {
        MongoDatabase mockDbStandard = Mockito.mock(MongoDatabase.class);
        MongoDatabase mockDbPremium = Mockito.mock(MongoDatabase.class);

        Mockito.when(mongoClient.getDatabase("shared_educational_erp")).thenReturn(mockDbStandard);
        Mockito.when(mongoClient.getDatabase("tenant_schoolpremium")).thenReturn(mockDbPremium);

        // 1. Without context
        TenantContext.clear();
        MongoDatabase db1 = mongoDatabaseFactory.getMongoDatabase();
        assertNotNull(db1);
        Mockito.verify(mongoClient, Mockito.atLeastOnce()).getDatabase("shared_educational_erp");

        // 2. Premium Tenant Context
        TenantContext.setCurrentTenant("schoolPremium");
        MongoDatabase db2 = mongoDatabaseFactory.getMongoDatabase();
        assertNotNull(db2);
        Mockito.verify(mongoClient, Mockito.atLeastOnce()).getDatabase("tenant_schoolpremium");

        TenantContext.clear();
    }
}
