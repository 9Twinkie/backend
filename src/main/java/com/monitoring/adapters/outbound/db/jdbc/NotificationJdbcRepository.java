package com.monitoring.adapters.outbound.db.jdbc;

import com.monitoring.core.application.ports.out.repositories.NotificationRepository;
import com.monitoring.core.domain.Notification;
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
 * JDBC-реализация хранилища уведомлений. read_at NULL трактуется как «не прочитано».
 */
@Repository
public class NotificationJdbcRepository implements NotificationRepository {

    private static final String SELECT_BY_ID = """
            SELECT id, incident_id, engineer_id, channel, delivered, read_at
            FROM notifications
            WHERE id = :id
            """;

    private static final String SELECT_BY_INCIDENT = """
            SELECT id, incident_id, engineer_id, channel, delivered, read_at
            FROM notifications
            WHERE incident_id = :incident_id
            ORDER BY id
            """;

    private static final String SELECT_ALL = """
            SELECT id, incident_id, engineer_id, channel, delivered, read_at
            FROM notifications
            ORDER BY id DESC
            """;

    private static final String SELECT_BY_ENGINEER = """
            SELECT id, incident_id, engineer_id, channel, delivered, read_at
            FROM notifications
            WHERE engineer_id = :engineer_id
            ORDER BY id DESC
            """;

    private static final String UPDATE_READ = """
            UPDATE notifications
            SET read_at = CURRENT_TIMESTAMP
            WHERE id = :id
              AND read_at IS NULL
            """;

    private static final String INSERT = """
            INSERT INTO notifications (incident_id, engineer_id, channel, delivered, read_at)
            VALUES (:incident_id, :engineer_id, :channel, :delivered, :read_at)
            """;

    private static final String COUNT_UNREAD = """
            SELECT COUNT(*)
            FROM notifications
            WHERE engineer_id = :engineer_id
              AND read_at IS NULL
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public NotificationJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Notification> findById(Long id) {
        var params = new MapSqlParameterSource("id", id);
        try {
            return Optional.ofNullable(jdbc.queryForObject(SELECT_BY_ID, params, new NotificationRowMapper()));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<Notification> findByIncidentId(Long incidentId) {
        var params = new MapSqlParameterSource("incident_id", incidentId);
        return jdbc.query(SELECT_BY_INCIDENT, params, new NotificationRowMapper());
    }

    @Override
    public List<Notification> findAll() {
        return jdbc.query(SELECT_ALL, new NotificationRowMapper());
    }

    @Override
    public List<Notification> findByEngineerId(Long engineerId) {
        var params = new MapSqlParameterSource("engineer_id", engineerId);
        return jdbc.query(SELECT_BY_ENGINEER, params, new NotificationRowMapper());
    }

    @Override
    @Transactional
    public int markAsRead(Long id) {
        return jdbc.update(UPDATE_READ, new MapSqlParameterSource("id", id));
    }

    @Override
    @Transactional
    public Notification create(Notification notification) {
        var kh = new GeneratedKeyHolder();
        var params = new MapSqlParameterSource()
                .addValue("incident_id", notification.incidentId())
                .addValue("engineer_id", notification.engineerId())
                .addValue("channel", notification.channel())
                .addValue("delivered", notification.delivered())
                .addValue("read_at", notification.readAt());
        jdbc.update(INSERT, params, kh, new String[]{"id"});
        var key = Objects.requireNonNull(kh.getKey(), "Не удалось получить сгенерированный ключ");
        return new Notification(
                key.longValue(),
                notification.incidentId(),
                notification.engineerId(),
                notification.channel(),
                notification.delivered(),
                notification.readAt()
        );
    }

    @Override
    public long countUnreadByEngineer(Long engineerId) {
        var params = new MapSqlParameterSource("engineer_id", engineerId);
        var count = Objects.requireNonNull(
                jdbc.queryForObject(COUNT_UNREAD, params, Long.class),
                "COUNT(*) не должен быть NULL"
        );
        return count;
    }

    /**
     * Маппер строки БД в доменное {@link Notification}.
     */
    private static final class NotificationRowMapper implements RowMapper<Notification> {
        @Override
        public Notification mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp readTs = rs.getTimestamp("read_at");
            LocalDateTime readAt = readTs != null ? readTs.toLocalDateTime() : null;
            return new Notification(
                    rs.getLong("id"),
                    rs.getLong("incident_id"),
                    rs.getLong("engineer_id"),
                    rs.getString("channel"),
                    rs.getBoolean("delivered"),
                    readAt
            );
        }
    }
}
