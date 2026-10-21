package com.sanedge.ecommerce.statsbackfill.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * One-shot backfill: reads aggregated revenue from the source PostgreSQL (transaction
 * DB) and writes it into the ClickHouse {@code stats_revenue} table.
 *
 * <p>The source SQL is configurable via {@code backfill.sql}; the default assumes a
 * {@code transactions(created_at, total_amount, merchant_id, product_id)} table and
 * must be adjusted to the real schema. Idempotent: rows are re-inserted (ClickHouse
 * is append-only), so run once per backfill window.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatsBackfillRunner implements CommandLineRunner {

    private final JdbcTemplate sourceJdbcTemplate;
    private final JdbcTemplate clickHouseJdbcTemplate;

    @Value("${backfill.sql:SELECT to_char(created_at, 'YYYY-MM') AS period, "
            + "COALESCE(merchant_id, 0) AS merchant_id, COALESCE(product_id, 0) AS product_id, "
            + "SUM(total_amount) AS revenue FROM transactions GROUP BY 1, 2, 3}")
    private String backfillSql;

    @Override
    public void run(String... args) {
        log.info("stats-backfill starting");
        ensureTable();
        int count = sourceJdbcTemplate.query(backfillSql, (rs, i) -> new Object[]{
                rs.getString("period"),
                rs.getLong("merchant_id"),
                rs.getLong("product_id"),
                rs.getDouble("revenue")
        }).stream().mapToInt(row -> {
            clickHouseJdbcTemplate.update(
                    "INSERT INTO stats_revenue (period, revenue, merchant_id, product_id, metric) "
                            + "VALUES (?, ?, ?, ?, 'txn')",
                    row[0], row[3], row[1], row[2]);
            return 1;
        }).sum();
        log.info("stats-backfill completed: {} rows written to ClickHouse", count);
    }

    private void ensureTable() {
        clickHouseJdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS stats_revenue ("
                        + "period String, "
                        + "revenue Float64, "
                        + "merchant_id Int64 DEFAULT 0, "
                        + "product_id Int64 DEFAULT 0, "
                        + "metric String DEFAULT 'txn'"
                        + ") ENGINE = MergeTree() ORDER BY (metric, period)");
    }
}
