package com.tongtu.docgen.system.config;

import com.tongtu.docgen.system.security.RequiredPermission;
import com.tongtu.docgen.system.security.PublicApi;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class OpenApiConfig {
    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Requirement Tool API")
                        .description("API for project, requirement, and system management.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH,
                                new SecurityScheme()
                                        .name(BEARER_AUTH)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("Token")));
    }

    @Bean
    public OperationCustomizer permissionOperationCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            if (isPublicOperation(handlerMethod)) {
                operation.setSecurity(new ArrayList<>());
            } else {
                operation.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
                ApiResponses responses = operation.getResponses();
                if (responses == null) {
                    responses = new ApiResponses();
                    operation.setResponses(responses);
                }
                responses.putIfAbsent("401", new ApiResponse().description("Unauthorized"));
                responses.putIfAbsent("403", new ApiResponse().description("Forbidden"));
            }

            List<String> permissions = collectRequiredPermissions(handlerMethod);
            if (permissions.isEmpty()) {
                return operation;
            }
            operation.addExtension("x-permissions", permissions);
            String tip = "Permission: " + String.join(", ", permissions);
            if (!StringUtils.hasText(operation.getDescription())) {
                operation.setDescription(tip);
            } else if (!operation.getDescription().contains(tip)) {
                operation.setDescription(operation.getDescription() + "\n" + tip);
            }
            return operation;
        };
    }

    private boolean isPublicOperation(HandlerMethod handlerMethod) {
        PublicApi classAnno = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), PublicApi.class);
        if (classAnno != null) {
            return true;
        }
        PublicApi methodAnno = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), PublicApi.class);
        return methodAnno != null;
    }

    private List<String> collectRequiredPermissions(HandlerMethod handlerMethod) {
        List<String> out = new ArrayList<>();
        RequiredPermission classAnno = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequiredPermission.class);
        RequiredPermission methodAnno = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequiredPermission.class);
        if (classAnno != null && classAnno.value() != null) {
            for (String p : classAnno.value()) {
                if (StringUtils.hasText(p)) {
                    out.add(p);
                }
            }
        }
        if (methodAnno != null && methodAnno.value() != null) {
            for (String p : methodAnno.value()) {
                if (StringUtils.hasText(p)) {
                    out.add(p);
                }
            }
        }
        return out;
    }
}
