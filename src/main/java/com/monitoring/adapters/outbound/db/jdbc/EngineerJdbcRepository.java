package com.monitoring.adapters.outbound.db.jdbc;

import com.monitoring.core.application.ports.out.repositories.EngineerRepository;
import com.monitoring.core.domain.Engineer;
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
 * JDBC-реализация хранилища инженеров. Поля phone/notification_prefs маппятся напрямую из VARCHAR-колонок.
 */
@Repository
public class EngineerJdbcRepository implements EngineerRepository {

    private static final String SELECT_BY_ID = """
            SELECT id, username, password_hash, role, phone, notification_prefs
            FROM engineers
            WHERE id = :id
            """;

    private static final String SELECT_BY_USERNAME = """
            SELECT id, username, password_hash, role, phone, notification_prefs
            FROM engineers
            WHERE username = :username
            """;

    private static final String SELECT_ALL = """
            SELECT id, username, password_hash, role, phone, notification_prefs
            FROM engineers
            ORDER BY id
            """;

    private static final String INSERT = """
            INSERT INTO engineers (username, password_hash, role, phone, notification_prefs)
            VALUES (:username, :password_hash, :role, :phone, :notification_prefs)
            """;

    private static final String UPDATE = """
            UPDATE engineers
            SET username = :username,
                password_hash = :password_hash,
                role = :role,
                phone = :phone,
                notification_prefs = :notification_prefs
            WHERE id = :id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public EngineerJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Engineer> findById(Long id) {
        var params = new MapSqlParameterSource("id", id);
        try {
            return Optional.ofNullable(jdbc.queryForObject(SELECT_BY_ID, params, new EngineerRowMapper()));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Engineer> findByUsername(String username) {
        var params = new MapSqlParameterSource("username", username);
        try {
            return Optional.ofNullable(jdbc.queryForObject(SELECT_BY_USERNAME, params, new EngineerRowMapper()));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public Engineer save(Engineer engineer) {
        if (engineer.id() == null) {
            var kh = new GeneratedKeyHolder();
            jdbc.update(INSERT, toParams(engineer), kh, new String[]{"id"});
            var key = Objects.requireNonNull(kh.getKey(), "Не удалось получить сгенерированный ключ");
            return new Engineer(
                    key.longValue(),
                    engineer.username(),
                    engineer.passwordHash(),
                    engineer.role(),
                    engineer.phone(),
                    engineer.notificationPrefs()
            );
        }
        jdbc.update(UPDATE, toParams(engineer).addValue("id", engineer.id()));
        return engineer;
    }

    @Override
    public List<Engineer> findAll() {
        return jdbc.query(SELECT_ALL, new EngineerRowMapper());
    }

    private static MapSqlParameterSource toParams(Engineer engineer) {
        return new MapSqlParameterSource()
                .addValue("username", engineer.username())
                .addValue("password_hash", engineer.passwordHash())
                .addValue("role", engineer.role())
                .addValue("phone", engineer.phone())
                .addValue("notification_prefs", engineer.notificationPrefs());
    }

    /**
     * Маппер строки БД в доменного {@link Engineer}.
     */
    private static final class EngineerRowMapper implements RowMapper<Engineer> {
        @Override
        public Engineer mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Engineer(
                    rs.getLong("id"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("role"),
                    rs.getString("phone"),
                    rs.getString("notification_prefs")
            );
        }
    }
}
