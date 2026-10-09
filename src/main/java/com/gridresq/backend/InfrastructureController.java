package com.gridresq.backend;

import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/infrastructure")
@CrossOrigin(origins = {
    "http://localhost:5173",
    "http://localhost:5174"
})
public class InfrastructureController {

    @GetMapping
    public List<Infrastructure> getInfrastructure() {

        List<Infrastructure> infrastructure = new ArrayList<>();

        infrastructure.add(
            new Infrastructure("Power Grid", 95, "Operational")
        );

        infrastructure.add(
            new Infrastructure("Water Network", 90, "Operational")
        );

        infrastructure.add(
            new Infrastructure("Telecom Tower", 88, "Operational")
        );

        infrastructure.add(
            new Infrastructure("Hospital H1", 100, "Operational")
        );

        infrastructure.add(
            new Infrastructure("Emergency Center", 92, "Operational")
        );

        return infrastructure;
    }
}