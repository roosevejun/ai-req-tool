package com.tongtu.docgen.system.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

@Component
public class DatabaseUpgradeRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DatabaseUpgradeRunner.class);
    private static final String UPGRADE_TABLE = "sys_schema_upgrade";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    @Value("${agent.db.autoUpgrade.enabled:true}")
    private boolean autoUpgradeEnabled;

    public DatabaseUpgradeRunner(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!autoUpgradeEnabled) {
            log.info("Database auto upgrade is disabled.");
            return;
        }

        ensureUpgradeTable();
        List<Resource> scripts = findUpgradeScripts();
        if (scripts.isEmpty()) {
            log.info("No database upgrade scripts found under classpath:db/upgrade-*.sql");
            return;
        }

        for (Resource script : scripts) {
            String scriptName = script.getFilename();
            if (scriptName == null || scriptName.isBlank()) {
                continue;
            }

            String checksum = sha256(script);
            String appliedChecksum = findAppliedChecksum(scriptName);
            if (checksum.equals(appliedChecksum)) {
                log.info("Database upgrade already applied: {}", scriptName);
                continue;
            }

            log.info("Applying database upgrade script: {}", scriptName);
            try (var connection = dataSource.getConnection()) {
                ScriptUtils.executeSqlScript(connection, new EncodedResource(script, StandardCharsets.UTF_8));
            } catch (ScriptException ex) {
                throw new IllegalStateException("Execute database upgrade script failed: " + scriptName, ex);
            }
            upsertAppliedScript(scriptName, checksum);
            log.info("Database upgrade applied successfully: {}", scriptName);
        }
    }

    private void ensureUpgradeTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_schema_upgrade (
                    script_name VARCHAR(255) PRIMARY KEY,
                    checksum VARCHAR(64) NOT NULL,
                    applied_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);
    }

    private List<Resource> findUpgradeScripts() throws Exception {
        Resource[] resources = resourceResolver.getResources("classpath*:db/upgrade-*.sql");
        List<Resource> result = new ArrayList<>();
        for (Resource resource : resources) {
            if (resource.exists()) {
                result.add(resource);
            }
        }
        result.sort(Comparator.comparing(Resource::getFilename, Comparator.nullsLast(String::compareToIgnoreCase)));
        return result;
    }

    private String findAppliedChecksum(String scriptName) {
        List<String> rows = jdbcTemplate.query(
                "SELECT checksum FROM " + UPGRADE_TABLE + " WHERE script_name = ?",
                (rs, rowNum) -> rs.getString(1),
                scriptName
        );
        return rows.isEmpty() ? "" : rows.get(0);
    }

    private void upsertAppliedScript(String scriptName, String checksum) {
        jdbcTemplate.update("""
                INSERT INTO sys_schema_upgrade(script_name, checksum, applied_at)
                VALUES (?, ?, NOW())
                ON CONFLICT (script_name)
                DO UPDATE SET checksum = EXCLUDED.checksum, applied_at = NOW()
                """, scriptName, checksum);
    }

    private String sha256(Resource resource) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = resource.getInputStream().readAllBytes();
        return HexFormat.of().formatHex(digest.digest(bytes));
    }
}
