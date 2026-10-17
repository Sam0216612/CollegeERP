package com.example.erp.service;

import com.example.erp.model.Student;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StudentImportService {
    public record ImportRow(int rowNumber, Student student) {}
    public record RowResult(int rowNumber, String rollNo, String status, String message) {}
    public record ImportResult(int added, int skipped, int invalid, int failed, List<RowResult> rows) {}

    private final JdbcTemplate jdbc;
    public StudentImportService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Student createStudent(Student student) {
        ImportResult result = importStudents(List.of(new ImportRow(1, student)));
        RowResult row = result.rows().get(0);
        if (!row.status().equals("added")) {
            HttpStatus status = row.status().equals("duplicate") ? HttpStatus.CONFLICT
                : row.status().equals("invalid") ? HttpStatus.BAD_REQUEST : HttpStatus.SERVICE_UNAVAILABLE;
            throw new ResponseStatusException(status, row.message());
        }
        return student;
    }

    public ImportResult importStudents(List<ImportRow> rows) {
        if (rows == null || rows.isEmpty() || rows.size() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Send between 1 and 2000 student rows.");
        }
        return jdbc.execute((ConnectionCallback<ImportResult>) connection -> {
            // A database lock coordinates both CSV imports and manual creation, even across server instances.
            try (PreparedStatement lock = connection.prepareStatement(
                    "SELECT GET_LOCK(CONCAT(DATABASE(), ':erp_students_create'), 10)");
                 ResultSet acquired = lock.executeQuery()) {
                if (!acquired.next() || acquired.getInt(1) != 1) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Another student import is running. Retry shortly.");
                }
            }
            try { return insertNewRows(connection, rows); }
            finally {
                try (PreparedStatement unlock = connection.prepareStatement(
                        "SELECT RELEASE_LOCK(CONCAT(DATABASE(), ':erp_students_create'))")) {
                    unlock.execute();
                }
            }
        });
    }

    private ImportResult insertNewRows(Connection connection, List<ImportRow> rows) throws SQLException {
        Set<String> rolls = new HashSet<>(), emails = new HashSet<>();
        try (PreparedStatement select = connection.prepareStatement("SELECT roll_no, email FROM students");
             ResultSet existing = select.executeQuery()) {
            while (existing.next()) {
                rolls.add(key(existing.getString(1)));
                String email = key(existing.getString(2));
                if (!email.isEmpty()) emails.add(email);
            }
        }
        List<RowResult> results = new ArrayList<>();
        int added = 0, skipped = 0, invalid = 0, failed = 0;
        // Deliberately INSERT: never JPA merge, REPLACE, or ON DUPLICATE KEY UPDATE.
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO students (roll_no, name, course, semester, email, status) VALUES (?, ?, ?, ?, ?, ?)")) {
            for (ImportRow input : rows) {
                Student student = input == null ? null : input.student();
                int line = input == null ? 0 : input.rowNumber();
                String error = validate(student);
                String roll = student == null ? "" : clean(student.getRollNo());
                if (error != null) {
                    invalid++;
                    results.add(new RowResult(line, roll, "invalid", error));
                    continue;
                }
                if (rolls.contains(key(roll)) || emails.contains(key(student.getEmail()))) {
                    skipped++;
                    results.add(new RowResult(line, roll, "duplicate", "Roll number or email already exists; record unchanged."));
                    continue;
                }
                try {
                    insert.setString(1, student.getRollNo());
                    insert.setString(2, student.getName());
                    insert.setString(3, student.getCourse());
                    insert.setInt(4, student.getSemester());
                    insert.setString(5, student.getEmail());
                    insert.setString(6, student.getStatus());
                    insert.executeUpdate();
                    rolls.add(key(student.getRollNo()));
                    emails.add(key(student.getEmail()));
                    added++;
                    results.add(new RowResult(line, roll, "added", "New student added."));
                } catch (SQLException exception) {
                    if (exception.getErrorCode() == 1062) {
                        skipped++;
                        results.add(new RowResult(line, roll, "duplicate", "Database rejected a duplicate key; record unchanged."));
                    } else {
                        failed++;
                        results.add(new RowResult(line, roll, "failed", "Database could not save this row. Check the backend terminal."));
                        System.err.println("Student import row " + line + ": SQL state " + exception.getSQLState()
                            + ", error code " + exception.getErrorCode());
                    }
                }
            }
        }
        return new ImportResult(added, skipped, invalid, failed, results);
    }

    private static String clean(String value) { return value == null ? "" : value.strip(); }
    private static String key(String value) { return clean(value).toLowerCase(Locale.ROOT); }

    private static String validate(Student student) {
        if (student == null) return "Missing student details.";
        student.setRollNo(clean(student.getRollNo()));
        student.setName(clean(student.getName()));
        student.setCourse(clean(student.getCourse()).toUpperCase(Locale.ROOT));
        student.setEmail(key(student.getEmail()));
        String status = clean(student.getStatus());
        student.setStatus(status.isEmpty() ? "Active" : status);
        if (!student.getRollNo().matches("[A-Za-z0-9][A-Za-z0-9_-]{0,63}"))
            return "Roll number must contain 1–64 letters, digits, underscores or hyphens.";
        if (student.getName().isEmpty() || student.getName().length() > 255) return "Name is required (maximum 255 characters).";
        if (!Set.of("AIML", "CSE").contains(student.getCourse())) return "Course must be AIML or CSE.";
        if (student.getSemester() == null || student.getSemester() < 1 || student.getSemester() > 8)
            return "Semester must be a whole number from 1 to 8.";
        if (student.getEmail().length() > 255 || !student.getEmail().matches("[^\\s@]+@mitwpu\\.edu\\.in"))
            return "Enter a valid @mitwpu.edu.in email.";
        if (student.getStatus().length() > 50) return "Status must be at most 50 characters.";
        return null;
    }
}
