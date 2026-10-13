package com.gridresq.backend;

import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/simulation")
@CrossOrigin(origins = {
    "http://localhost:5173",
    "http://localhost:5174"
})
public class SimulationController {

    private final SimulationHistoryService historyService;

    public SimulationController(
            SimulationHistoryService historyService) {
        this.historyService = historyService;
    }

    private static final List<String> COMPONENTS = Arrays.asList(
        "Power Grid",
        "Water Network",
        "Telecom Tower",
        "Hospital H1",
        "Emergency Center"
    );

    private static final Map<String, Double> INITIAL_CAPACITY =
        new LinkedHashMap<>();

    /*
     * Illustrative infrastructure dependencies.
     * These rules are for simulation purposes, not real-world predictions.
     */
    private static final Map<String, List<String>> DEPENDENCIES =
        new LinkedHashMap<>();

    static {
        INITIAL_CAPACITY.put("Power Grid", 95.0);
        INITIAL_CAPACITY.put("Water Network", 90.0);
        INITIAL_CAPACITY.put("Telecom Tower", 88.0);
        INITIAL_CAPACITY.put("Hospital H1", 100.0);
        INITIAL_CAPACITY.put("Emergency Center", 92.0);

        DEPENDENCIES.put(
            "Power Grid",
            Collections.emptyList()
        );

        DEPENDENCIES.put(
            "Water Network",
            Arrays.asList("Power Grid")
        );

        DEPENDENCIES.put(
            "Telecom Tower",
            Arrays.asList("Power Grid")
        );

        DEPENDENCIES.put(
            "Hospital H1",
            Arrays.asList("Power Grid", "Water Network")
        );

        DEPENDENCIES.put(
            "Emergency Center",
            Arrays.asList("Power Grid", "Telecom Tower")
        );
    }

    @PostMapping
    public Map<String, Object> runSimulation(
            @RequestBody Map<String, String> request) {

        String failedComponent = request.get("component");

        if (failedComponent == null ||
                !COMPONENTS.contains(failedComponent)) {
            throw new IllegalArgumentException(
                "Invalid infrastructure component"
            );
        }

        Map<String, Double> capacities = new LinkedHashMap<>();
        Map<String, String> statuses = new LinkedHashMap<>();

        List<String> affectedComponents = new ArrayList<>();
        List<String> recoveryRecommendations = new ArrayList<>();

        // Initialize the infrastructure system.
        for (String component : COMPONENTS) {
            capacities.put(
                component,
                INITIAL_CAPACITY.get(component)
            );

            statuses.put(component, "Operational");
        }

        // Apply illustrative cascading-failure rules.
        switch (failedComponent) {

            case "Power Grid":
                capacities.put("Power Grid", 0.0);
                capacities.put("Water Network", 55.0);
                capacities.put("Telecom Tower", 53.0);
                capacities.put("Hospital H1", 65.0);
                capacities.put("Emergency Center", 57.0);
                break;

            case "Water Network":
                capacities.put("Water Network", 0.0);
                capacities.put("Hospital H1", 60.0);
                break;

            case "Telecom Tower":
                capacities.put("Telecom Tower", 0.0);
                capacities.put("Emergency Center", 65.0);
                break;

            case "Hospital H1":
                capacities.put("Hospital H1", 0.0);
                break;

            case "Emergency Center":
                capacities.put("Emergency Center", 0.0);
                break;

            default:
                throw new IllegalArgumentException(
                    "Unsupported infrastructure component"
                );
        }

        // Classify component states.
        for (String component : COMPONENTS) {

            double capacity = capacities.get(component);

            if (component.equals(failedComponent)) {
                statuses.put(component, "Initial Failure");
                affectedComponents.add(component);

            } else if (capacity < INITIAL_CAPACITY.get(component)) {
                statuses.put(component, "Affected");
                affectedComponents.add(component);
            }
        }

        // Calculate average remaining system capacity.
        double totalCapacity = 0.0;

        for (double capacity : capacities.values()) {
            totalCapacity += capacity;
        }

        int systemHealth = (int) Math.round(
            totalCapacity / COMPONENTS.size()
        );

        // Count unaffected components.
        int operationalCount = 0;

        for (String component : COMPONENTS) {
            if ("Operational".equals(statuses.get(component))) {
                operationalCount++;
            }
        }

        // Calculate system risk level.
        String riskLevel;

        if (systemHealth < 40) {
            riskLevel = "Critical";
        } else if (systemHealth < 60) {
            riskLevel = "High";
        } else if (systemHealth < 80) {
            riskLevel = "Moderate";
        } else {
            riskLevel = "Low";
        }

        // Generate recovery recommendations.
        switch (failedComponent) {

            case "Power Grid":
                recoveryRecommendations.add(
                    "Prioritize restoring the power supply."
                );
                recoveryRecommendations.add(
                    "Assess backup power availability at the hospital."
                );
                recoveryRecommendations.add(
                    "Restore power-dependent water and telecom services."
                );
                break;

            case "Water Network":
                recoveryRecommendations.add(
                    "Prioritize restoring water distribution."
                );
                recoveryRecommendations.add(
                    "Check hospital water reserves and backup supply."
                );
                break;

            case "Telecom Tower":
                recoveryRecommendations.add(
                    "Restore telecommunications connectivity."
                );
                recoveryRecommendations.add(
                    "Establish alternative communication for emergency services."
                );
                break;

            case "Hospital H1":
                recoveryRecommendations.add(
                    "Activate hospital emergency procedures."
                );
                recoveryRecommendations.add(
                    "Assess patient transfers and backup medical facilities."
                );
                break;

            case "Emergency Center":
                recoveryRecommendations.add(
                    "Activate an alternate emergency coordination center."
                );
                recoveryRecommendations.add(
                    "Establish backup communication channels."
                );
                break;

            default:
                recoveryRecommendations.add(
                    "Assess the failed infrastructure component."
                );
        }

        // Save the simulation summary to MySQL.
        historyService.saveSimulation(
            failedComponent,
            affectedComponents.size(),
            operationalCount,
            systemHealth
        );

        // Return results to the frontend.
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("initialFailure", failedComponent);
        result.put("affectedComponents", affectedComponents);
        result.put("affectedCount", affectedComponents.size());
        result.put("stillOperational", operationalCount);
        result.put("systemHealth", systemHealth);
        result.put("capacities", capacities);
        result.put("statuses", statuses);
        result.put("riskLevel", riskLevel);
        result.put("dependencies", DEPENDENCIES);
        result.put(
            "recoveryRecommendations",
            recoveryRecommendations
        );
        result.put(
            "message",
            "Simulation completed and saved to database"
        );

        return result;
    }

    @PostMapping("/reset")
    public Map<String, String> resetSimulation() {

        Map<String, String> result = new LinkedHashMap<>();

        result.put("status", "success");
        result.put(
            "message",
            "Simulation reset successfully"
        );

        return result;
    }
}