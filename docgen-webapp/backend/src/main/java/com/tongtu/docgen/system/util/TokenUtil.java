package com.tongtu.docgen.system.util;

import com.tongtu.docgen.system.exception.UnauthorizedException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Component
public class TokenUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${agent.auth.token.secret:change-me-in-prod}")
    private String secret;

    @Value("${agent.auth.token.expireMinutes:720}")
    private int expireMinutes;

    public String createToken(Long userId, String username) {
        long exp = Instant.now().plusSeconds(Math.max(10, expireMinutes) * 60L).getEpochSecond();
        Map<String, Object> payload = Map.of("uid", userId, "unm", username, "exp", exp);
        String payloadJson;
        try {
            payloadJson = MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Create token failed: " + e.getMessage(), e);
        }
        String payloadPart = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String sign = hmacSha256(payloadPart, secret);
        return payloadPart + "." + sign;
    }

    public TokenPayload verify(String token) {
        if (token == null || token.isBlank() || !token.contains(".")) {
            throw new UnauthorizedException("Invalid token.");
        }
        String[] parts = token.split("\\.", 2);
        String payloadPart = parts[0];
        String signPart = parts[1];

        String expected = hmacSha256(payloadPart, secret);
        if (!expected.equals(signPart)) {
            throw new UnauthorizedException("Invalid token signature.");
        }
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadPart);
            JsonNode root = MAPPER.readTree(payloadBytes);
            long userId = root.path("uid").asLong(0L);
            String username = root.path("unm").asText("");
            long exp = root.path("exp").asLong(0L);
            if (userId <= 0 || username.isBlank()) {
                throw new UnauthorizedException("Invalid token payload.");
            }
            if (Instant.now().getEpochSecond() > exp) {
                throw new UnauthorizedException("Token expired.");
            }
            return new TokenPayload(userId, username, exp);
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid token format.");
        }
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException("Sign token failed: " + e.getMessage(), e);
        }
    }

    public record TokenPayload(Long userId, String username, Long exp) {
    }
}


