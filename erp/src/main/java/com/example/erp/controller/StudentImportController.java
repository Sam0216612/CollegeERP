package com.example.erp.controller;

import com.example.erp.service.StudentImportService;
import com.example.erp.service.StudentImportService.ImportResult;
import com.example.erp.service.StudentImportService.ImportRow;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
public class StudentImportController {
    private final StudentImportService service;
    public StudentImportController(StudentImportService service) { this.service = service; }

    @PostMapping("/import")
    public ImportResult importStudents(@RequestBody List<ImportRow> rows) {
        return service.importStudents(rows);
    }
}
