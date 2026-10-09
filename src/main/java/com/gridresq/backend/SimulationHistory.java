package com.gridresq.backend;

import java.time.LocalDateTime;

public class SimulationHistory {

    private Long id;
    private String failedComponent;
    private int affectedCount;
    private int operationalCount;
    private int systemHealth;
    private LocalDateTime simulationTime;

    public SimulationHistory() {
    }

    public SimulationHistory(
            Long id,
            String failedComponent,
            int affectedCount,
            int operationalCount,
            int systemHealth,
            LocalDateTime simulationTime) {

        this.id = id;
        this.failedComponent = failedComponent;
        this.affectedCount = affectedCount;
        this.operationalCount = operationalCount;
        this.systemHealth = systemHealth;
        this.simulationTime = simulationTime;
    }

    public Long getId() {
        return id;
    }

    public String getFailedComponent() {
        return failedComponent;
    }

    public int getAffectedCount() {
        return affectedCount;
    }

    public int getOperationalCount() {
        return operationalCount;
    }

    public int getSystemHealth() {
        return systemHealth;
    }

    public LocalDateTime getSimulationTime() {
        return simulationTime;
    }
}