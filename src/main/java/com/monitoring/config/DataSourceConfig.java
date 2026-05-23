package com.monitoring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

/**
 * Конфигурация JDBC: именованный шаблон поверх пула HikariCP из Spring Boot.
 */
@Configuration
public class DataSourceConfig {

    /**
     * NamedParameterJdbcTemplate упрощает работу с именованными параметрами SQL и используется адаптерами.
     */
    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
