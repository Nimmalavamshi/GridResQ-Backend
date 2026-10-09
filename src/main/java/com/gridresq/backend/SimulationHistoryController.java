package com.gridresq.backend;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = {
    "http://localhost:5173",
    "http://localhost:5174"
})
public class SimulationHistoryController {

    private final SimulationHistoryService historyService;

    public SimulationHistoryController(
            SimulationHistoryService historyService) {

        this.historyService = historyService;
    }

    @GetMapping
    public List<SimulationHistory> getHistory() {

        return historyService.getAllSimulations();
    }
}