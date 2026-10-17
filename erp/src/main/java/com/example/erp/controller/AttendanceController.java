package com.example.erp.controller;

import com.example.erp.service.AttendanceService;
import com.example.erp.service.AttendanceService.Row;
import com.example.erp.service.AttendanceService.Saved;
import com.example.erp.service.AttendanceService.Submission;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "*")
public class AttendanceController {
    private final AttendanceService service;
    public AttendanceController(AttendanceService service) { this.service = service; }
    @GetMapping
    public List<Row> list() { return service.list(); }
    @PostMapping
    public Saved submit(@RequestBody Submission submission) { return service.submit(submission); }
}
