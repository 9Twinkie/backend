package com.monitoring.adapters.outbound.db.jdbc;

import com.monitoring.core.application.ports.out.repositories.IncidentRepository;
import com.monitoring.core.domain.Incident;
import com.monitoring.core.domain.Severity;
import com.monitoring.core.domain.Status;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class IncidentJdbcRepository implements IncidentRepository {

    private static final String INCIDENT_COLUMNS = """
            id, rule_id, prometheus_fingerprint, prometheus_alert_name, prometheus_expr, prometheus_severity,
            timestamp, status, assigned_engineer_id, resolved_at
            """;

    private static final String SELECT_BY_ID = """
            SELECT
            """ + INCIDENT_COLUMNS + """
            FROM incidents
            WHERE id = :id
            """;

    private static final String SELECT_BY_ENGINEER_STATUS = """
            SELECT
            """ + INCIDENT_COLUMNS + """
            FROM incidents
            WHERE assigned_engineer_id = :engineer_id
              AND status = :status
            ORDER BY timestamp DESC
            """;

    private static final String SELECT_ALL = """
            SELECT
            """ + INCIDENT_COLUMNS + """
            FROM incidents
            ORDER BY timestamp DESC
            """;

    private static final String SELECT_OPEN_BY_RULE = """
            SELECT
            """ + INCIDENT_COLUMNS + """
            FROM incidents
            WHERE rule_id = :rule_id
              AND status IN ('NEW', 'CONFIRMED')
            ORDER BY timestamp DESC
            LIMIT 1
            """;

    private static final String SELECT_OPEN_BY_FINGERPRINT = """
            SELECT
            """ + INCIDENT_COLUMNS + """
            FROM incidents
            WHERE prometheus_fingerprint = :fingerprint
              AND status IN ('NEW', 'CONFIRMED')
            ORDER BY timestamp DESC
            LIMIT 1
            """;

    private static final String SELECT_ALL_OPEN = """
            SELECT
            """ + INCIDENT_COLUMNS + """
            FROM incidents
            WHERE status IN ('NEW', 'CONFIRMED')
            ORDER BY timestamp DESC
            """;

    private static final String SELECT_ALL_OPEN_PROMETHEUS = """
            SELECT
            """ + INCIDENT_COLUMNS + """
            FROM incidents
            WHERE status IN ('NEW', 'CONFIRMED')
              AND prometheus_fingerprint IS NOT NULL
            ORDER BY timestamp DESC
            """;

    private static final String SELECT_VISIBLE_TO_ENGINEER = """
            SELECT
            """ + INCIDENT_COLUMNS + """
            FROM incidents
            WHERE status = 'NEW'
               OR assigned_engineer_id = :engineer_id
            ORDER BY timestamp DESC
            """;

    private static final String INSERT = """
            INSERT INTO incidents (
                rule_id, prometheus_fingerprint, prometheus_alert_name, prometheus_expr, prometheus_severity,
                timestamp, status, assigned_engineer_id, resolved_at
            )
            VALUES (
                :rule_id, :prometheus_fingerprint, :prometheus_alert_name, :prometheus_expr, :prometheus_severity,
                :timestamp, :status, :assigned_engineer_id, :resolved_at
            )
            """;

    private static final String UPDATE_STATUS = """
            UPDATE incidents
            SET status = :status,
                assigned_engineer_id = :engineer_id,
                resolved_at = CASE
                    WHEN :status = 'CLOSED' THEN COALESCE(resolved_at, CURRENT_TIMESTAMP)
                    ELSE resolved_at
                END
            WHERE id = :id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public IncidentJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Incident> findById(Long id) {
        var params = new MapSqlParameterSource("id", id);
        try {
            return Optional.ofNullable(jdbc.queryForObject(SELECT_BY_ID, params, new IncidentRowMapper()));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<Incident> findByEngineerIdAndStatus(Long engineerId, String status) {
        var params = new MapSqlParameterSource()
                .addValue("engineer_id", engineerId)
                .addValue("status", status);
        return jdbc.query(SELECT_BY_ENGINEER_STATUS, params, new IncidentRowMapper());
    }

    @Override
    public List<Incident> findAll() {
        return jdbc.query(SELECT_ALL, new IncidentRowMapper());
    }

    @Override
    public Optional<Incident> findOpenByRuleId(Long ruleId) {
        var params = new MapSqlParameterSource("rule_id", ruleId);
        var list = jdbc.query(SELECT_OPEN_BY_RULE, params, new IncidentRowMapper());
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public Optional<Incident> findOpenByPrometheusFingerprint(String fingerprint) {
        var params = new MapSqlParameterSource("fingerprint", fingerprint);
        var list = jdbc.query(SELECT_OPEN_BY_FINGERPRINT, params, new IncidentRowMapper());
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public List<Incident> findAllOpen() {
        return jdbc.query(SELECT_ALL_OPEN, new IncidentRowMapper());
    }

    @Override
    public List<Incident> findAllOpenPrometheusSourced() {
        return jdbc.query(SELECT_ALL_OPEN_PROMETHEUS, new IncidentRowMapper());
    }

    @Override
    public List<Incident> findVisibleToEngineer(Long engineerId) {
        var params = new MapSqlParameterSource("engineer_id", engineerId);
        return jdbc.query(SELECT_VISIBLE_TO_ENGINEER, params, new IncidentRowMapper());
    }

    @Override
    @Transactional
    public Incident create(Incident incident) {
        var kh = new GeneratedKeyHolder();
        var params = new MapSqlParameterSource()
                .addValue("rule_id", incident.ruleId())
                .addValue("prometheus_fingerprint", incident.prometheusFingerprint())
                .addValue("prometheus_alert_name", incident.prometheusAlertName())
                .addValue("prometheus_expr", incident.prometheusExpr())
                .addValue("prometheus_severity", severityToDb(incident.prometheusSeverity()))
                .addValue("timestamp", incident.timestamp())
                .addValue("status", incident.status().name())
                .addValue("assigned_engineer_id", incident.assignedEngineerId())
                .addValue("resolved_at", incident.resolvedAt());
        jdbc.update(INSERT, params, kh, new String[]{"id"});
        var key = Objects.requireNonNull(kh.getKey(), "Не удалось получить сгенерированный ключ");
        return copyWithId(incident, key.longValue());
    }

    @Override
    @Transactional
    public int updateStatus(Long id, String status, Long engineerId) {
        var params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("status", status)
                .addValue("engineer_id", engineerId);
        return jdbc.update(UPDATE_STATUS, params);
    }

    private static Incident copyWithId(Incident incident, long id) {
        return new Incident(
                id,
                incident.ruleId(),
                incident.prometheusFingerprint(),
                incident.prometheusAlertName(),
                incident.prometheusExpr(),
                incident.prometheusSeverity(),
                incident.timestamp(),
                incident.status(),
                incident.assignedEngineerId(),
                incident.resolvedAt()
        );
    }

    private static String severityToDb(Severity severity) {
        return severity != null ? severity.name() : null;
    }

    private static final class IncidentRowMapper implements RowMapper<Incident> {
        @Override
        public Incident mapRow(ResultSet rs, int rowNum) throws SQLException {
            Long ruleId = rs.getLong("rule_id");
            if (rs.wasNull()) {
                ruleId = null;
            }
            Long assigned = rs.getLong("assigned_engineer_id");
            if (rs.wasNull()) {
                assigned = null;
            }
            Timestamp resolvedTs = rs.getTimestamp("resolved_at");
            LocalDateTime resolved = resolvedTs != null ? resolvedTs.toLocalDateTime() : null;
            String sevStr = rs.getString("prometheus_severity");
            Severity prometheusSeverity = sevStr != null ? Severity.valueOf(sevStr) : null;
            return new Incident(
                    rs.getLong("id"),
                    ruleId,
                    rs.getString("prometheus_fingerprint"),
                    rs.getString("prometheus_alert_name"),
                    rs.getString("prometheus_expr"),
                    prometheusSeverity,
                    rs.getTimestamp("timestamp").toLocalDateTime(),
                    Status.valueOf(rs.getString("status")),
                    assigned,
                    resolved
            );
        }
    }
}
