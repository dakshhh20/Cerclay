package com.mittiandmore.controller;

import com.mittiandmore.dto.AdminDashboardResponse;
import com.mittiandmore.service.AdminDashboardService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService service;

    public AdminDashboardController(AdminDashboardService service) {
        this.service = service;
    }

    @GetMapping
    public AdminDashboardResponse dashboard(@RequestParam(defaultValue = "30") int days) {
        return service.getDashboard(days);
    }
}
