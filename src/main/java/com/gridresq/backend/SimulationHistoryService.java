package com.gridresq.backend;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SimulationHistoryService {

    private final JdbcTemplate jdbcTemplate;

    public SimulationHistoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveSimulation(
            String failedComponent,
            int affectedCount,
            int operationalCount,
            int systemHealth) {

        String sql = """
            INSERT INTO simulation_history
            (
                failed_component,
                affected_count,
                operational_count,
                system_health
            )
            VALUES (?, ?, ?, ?)
            """;

        jdbcTemplate.update(
            sql,
            failedComponent,
            affectedCount,
            operationalCount,
            systemHealth
        );
    }

    public List<SimulationHistory> getAllSimulations() {

        String sql = """
            SELECT
                id,
                failed_component,
                affected_count,
                operational_count,
                system_health,
                simulation_time
            FROM simulation_history
            ORDER BY id DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {

            java.sql.Timestamp timestamp =
                rs.getTimestamp("simulation_time");

            return new SimulationHistory(
                rs.getLong("id"),
                rs.getString("failed_component"),
                rs.getInt("affected_count"),
                rs.getInt("operational_count"),
                rs.getInt("system_health"),
                timestamp != null
                    ? timestamp.toLocalDateTime()
                    : null
            );
        });
    }
}