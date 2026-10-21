package com.sanedge.ecommerce.statsbackfill.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * Source PostgreSQL DataSource (the per-context transaction DB) that the backfill
 * reads aggregated revenue from. Point {@code backfill.source.*} at the transaction
 * service database.
 */
@Configuration
public class SourceConfig {

    @Bean
    public DataSource sourceDataSource(
            @Value("${backfill.source.url:jdbc:postgresql://localhost:5433/example_jpa_ecommerce}") String url,
            @Value("${backfill.source.user:postgres}") String user,
            @Value("${backfill.source.password:postgres}") String password) {
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setJdbcUrl(url);
        ds.setUsername(user);
        ds.setPassword(password);
        ds.setMaximumPoolSize(4);
        return ds;
    }

    @Bean
    public JdbcTemplate sourceJdbcTemplate(DataSource sourceDataSource) {
        return new JdbcTemplate(sourceDataSource);
    }
}
