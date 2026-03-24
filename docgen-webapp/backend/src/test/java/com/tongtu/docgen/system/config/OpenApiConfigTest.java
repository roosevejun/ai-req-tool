package com.tongtu.docgen.system.config;

import com.tongtu.docgen.system.security.PublicApi;
import com.tongtu.docgen.system.security.RequiredPermission;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigTest {

    private final OpenApiConfig openApiConfig = new OpenApiConfig();
    private final OperationCustomizer customizer = openApiConfig.permissionOperationCustomizer();

    @Test
    void publicApiShouldNotRequireBearer() throws Exception {
        HandlerMethod handlerMethod = handlerMethod("publicHealth");
        Operation operation = new Operation();

        customizer.customize(operation, handlerMethod);

        assertNotNull(operation.getSecurity());
        assertTrue(operation.getSecurity().isEmpty());
        ApiResponses responses = operation.getResponses();
        if (responses != null) {
            assertFalse(responses.containsKey("401"));
            assertFalse(responses.containsKey("403"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void securedApiShouldExposeAuthAndPermissions() throws Exception {
        HandlerMethod handlerMethod = handlerMethod("createProject");
        Operation operation = new Operation();

        customizer.customize(operation, handlerMethod);

        assertNotNull(operation.getSecurity());
        assertFalse(operation.getSecurity().isEmpty());
        assertEquals("bearerAuth", operation.getSecurity().get(0).keySet().stream().findFirst().orElse(null));

        assertNotNull(operation.getResponses());
        assertTrue(operation.getResponses().containsKey("401"));
        assertTrue(operation.getResponses().containsKey("403"));

        Map<String, Object> extensions = operation.getExtensions();
        assertNotNull(extensions);
        assertTrue(extensions.containsKey("x-permissions"));
        List<String> permissions = (List<String>) extensions.get("x-permissions");
        assertEquals(List.of("PROJECT:CREATE"), permissions);
        assertTrue(operation.getDescription().contains("Permission: PROJECT:CREATE"));
    }

    private HandlerMethod handlerMethod(String methodName) throws Exception {
        Method method = DummyController.class.getDeclaredMethod(methodName);
        return new HandlerMethod(new DummyController(), method);
    }

    private static class DummyController {
        @PublicApi
        public void publicHealth() {
        }

        @RequiredPermission("PROJECT:CREATE")
        public void createProject() {
        }
    }
}

