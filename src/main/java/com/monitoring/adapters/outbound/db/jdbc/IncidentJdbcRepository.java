package com.monitoring.adapters.outbound.db.jdbc;

import com.monitoring.core.application.ports.out.repositories.IncidentRepository;
import com.monitoring.core.domain.Incident;
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

/**
 * JDBC-реализация хранилища инцидентов. Статус хранится строкой и маппится в {@link Status}.
 */
@Repository
public class IncidentJdbcRepository implements IncidentRepository {

    private static final String SELECT_BY_ID = """
            SELECT id, rule_id, timestamp, status, assigned_engineer_id, resolved_at
            FROM incidents
            WHERE id = :id
            """;

    private static final String SELECT_BY_ENGINEER_STATUS = """
            SELECT id, rule_id, timestamp, status, assigned_engineer_id, resolved_at
            FROM incidents
            WHERE assigned_engineer_id = :engineer_id
              AND status = :status
            ORDER BY timestamp DESC
            """;

    private static final String SELECT_ALL = """
            SELECT id, rule_id, timestamp, status, assigned_engineer_id, resolved_at
            FROM incidents
            ORDER BY timestamp DESC
            """;

    private static final String SELECT_OPEN_BY_RULE = """
            SELECT id, rule_id, timestamp, status, assigned_engineer_id, resolved_at
            FROM incidents
            WHERE rule_id = :rule_id
              AND status IN ('NEW', 'CONFIRMED')
            ORDER BY timestamp DESC
            LIMIT 1
            """;

    private static final String SELECT_ALL_OPEN = """
            SELECT id, rule_id, timestamp, status, assigned_engineer_id, resolved_at
            FROM incidents
            WHERE status IN ('NEW', 'CONFIRMED')
            ORDER BY timestamp DESC
            """;

    private static final String SELECT_VISIBLE_TO_ENGINEER = """
            SELECT id, rule_id, timestamp, status, assigned_engineer_id, resolved_at
            FROM incidents
            WHERE status = 'NEW'
               OR assigned_engineer_id = :engineer_id
            ORDER BY timestamp DESC
            """;

    private static final String INSERT = """
            INSERT INTO incidents (rule_id, timestamp, status, assigned_engineer_id, resolved_at)
            VALUES (:rule_id, :timestamp, :status, :assigned_engineer_id, :resolved_at)
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
    public List<Incident> findAllOpen() {
        return jdbc.query(SELECT_ALL_OPEN, new IncidentRowMapper());
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
                .addValue("timestamp", incident.timestamp())
                .addValue("status", incident.status().name())
                .addValue("assigned_engineer_id", incident.assignedEngineerId())
                .addValue("resolved_at", incident.resolvedAt());
        jdbc.update(INSERT, params, kh, new String[]{"id"});
        var key = Objects.requireNonNull(kh.getKey(), "Не удалось получить сгенерированный ключ");
        return new Incident(
                key.longValue(),
                incident.ruleId(),
                incident.timestamp(),
                incident.status(),
                incident.assignedEngineerId(),
                incident.resolvedAt()
        );
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

    /**
     * Маппер строки БД в доменный {@link Incident}; nullable поля инженера/разрешения обрабатываются через getObject.
     */
    private static final class IncidentRowMapper implements RowMapper<Incident> {
        @Override
        public Incident mapRow(ResultSet rs, int rowNum) throws SQLException {
            Long assigned = rs.getLong("assigned_engineer_id");
            if (rs.wasNull()) {
                assigned = null;
            }
            Timestamp resolvedTs = rs.getTimestamp("resolved_at");
            LocalDateTime resolved = resolvedTs != null ? resolvedTs.toLocalDateTime() : null;
            return new Incident(
                    rs.getLong("id"),
                    rs.getLong("rule_id"),
                    rs.getTimestamp("timestamp").toLocalDateTime(),
                    Status.valueOf(rs.getString("status")),
                    assigned,
                    resolved
            );
        }
    }
}
