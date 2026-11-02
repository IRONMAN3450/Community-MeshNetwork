package org.example.mesh2.dashboard;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return dashboardService.stats();
    }

    @GetMapping("/map")
    public Map<String, Object> map() {
        return dashboardService.mapData();
    }
}
