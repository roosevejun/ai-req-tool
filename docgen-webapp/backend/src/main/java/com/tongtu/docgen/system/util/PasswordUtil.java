package com.tongtu.docgen.system.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordUtil {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String hash) {
        if (hash == null || hash.isBlank()) {
            return false;
        }
        if (hash.startsWith("{noop}")) {
            return hash.substring("{noop}".length()).equals(rawPassword);
        }
        return encoder.matches(rawPassword, hash);
    }
}

