package com.monitoring.adapters.outbound.db.jdbc;

import com.monitoring.core.application.ports.out.repositories.AlertRuleRepository;
import com.monitoring.core.domain.AlertRule;
import com.monitoring.core.domain.Severity;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC-реализация хранилища правил. Маппинг строки severity в enum {@link Severity}.
 */
@Repository
public class AlertRuleJdbcRepository implements AlertRuleRepository {

    private static final String SELECT_BY_ID = """
            SELECT id, metric_name, operator, threshold, severity, is_active
            FROM alert_rules
            WHERE id = :id
            """;

    private static final String SELECT_ACTIVE = """
            SELECT id, metric_name, operator, threshold, severity, is_active
            FROM alert_rules
            WHERE is_active = TRUE
            ORDER BY id
            """;

    private static final String INSERT = """
            INSERT INTO alert_rules (metric_name, operator, threshold, severity, is_active)
            VALUES (:metric_name, :operator, :threshold, :severity, :is_active)
            """;

    private static final String UPDATE = """
            UPDATE alert_rules
            SET metric_name = :metric_name,
                operator = :operator,
                threshold = :threshold,
                severity = :severity,
                is_active = :is_active
            WHERE id = :id
            """;

    private static final String UPDATE_STATUS = """
            UPDATE alert_rules
            SET is_active = :is_active
            WHERE id = :id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public AlertRuleJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<AlertRule> findById(Long id) {
        var params = new MapSqlParameterSource("id", id);
        try {
            var rule = jdbc.queryForObject(SELECT_BY_ID, params, new AlertRuleRowMapper());
            return Optional.ofNullable(rule);
        } catch (EmptyResultDataAccessException ex) {
            // Spring бросает это исключение, если строк нет — трактуем как отсутствие сущности
            return Optional.empty();
        }
    }

    @Override
    public List<AlertRule> findActiveRules() {
        return jdbc.query(SELECT_ACTIVE, new AlertRuleRowMapper());
    }

    @Override
    @Transactional
    public AlertRule save(AlertRule rule) {
        if (rule.id() == null) {
            var kh = new GeneratedKeyHolder();
            jdbc.update(INSERT, toParams(rule), kh, new String[]{"id"});
            var key = Objects.requireNonNull(kh.getKey(), "Не удалось получить сгенерированный ключ");
            return new AlertRule(key.longValue(), rule.metricName(), rule.operator(), rule.threshold(), rule.severity(), rule.isActive());
        }
        jdbc.update(UPDATE, toParams(rule).addValue("id", rule.id()));
        return rule;
    }

    @Override
    @Transactional
    public int updateStatus(Long id, Boolean isActive) {
        var params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("is_active", isActive);
        return jdbc.update(UPDATE_STATUS, params);
    }

    private static MapSqlParameterSource toParams(AlertRule rule) {
        return new MapSqlParameterSource()
                .addValue("metric_name", rule.metricName())
                .addValue("operator", rule.operator())
                .addValue("threshold", rule.threshold())
                .addValue("severity", rule.severity().name())
                .addValue("is_active", rule.isActive());
    }

    /**
     * Маппер строки БД в доменный {@link AlertRule}; severity преобразуется из строкового литерала CHECK-ограничения.
     */
    private static final class AlertRuleRowMapper implements RowMapper<AlertRule> {
        @Override
        public AlertRule mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new AlertRule(
                    rs.getLong("id"),
                    rs.getString("metric_name"),
                    rs.getString("operator"),
                    rs.getDouble("threshold"),
                    Severity.valueOf(rs.getString("severity")),
                    rs.getBoolean("is_active")
            );
        }
    }
}
