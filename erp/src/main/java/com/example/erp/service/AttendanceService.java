package com.example.erp.service;

import com.example.erp.model.Attendance;
import com.example.erp.model.Student;
import com.example.erp.model.Subject;
import com.example.erp.repository.AttendanceRepository;
import com.example.erp.repository.StudentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AttendanceService {
    public record Entry(String rollNo, Boolean present) {}
    public record Submission(String course, Integer semester, String subjectId, LocalDate date, List<Entry> entries) {}
    public record Row(Long id, String rollNo, String subjectId, LocalDate date, boolean present, LocalDateTime updatedAt) {}
    public record Saved(int saved, String subjectId, LocalDate date) {}

    private final AttendanceRepository attendance;
    private final StudentRepository students;
    private final EntityManager entities;
    public AttendanceService(AttendanceRepository attendance, StudentRepository students, EntityManager entities) {
        this.attendance = attendance; this.students = students; this.entities = entities;
    }

    @Transactional(readOnly = true)
    public List<Row> list() {
        return attendance.findAllWithDetails().stream().map(a -> new Row(a.getId(), a.getStudent().getRollNo(),
            a.getSubject().getId(), a.getDate(), a.isPresent(), a.getUpdatedAt())).toList();
    }

    @Transactional
    public Saved submit(Submission submission) {
        if (submission == null || submission.course() == null || submission.course().isBlank()
            || submission.semester() == null || submission.semester() < 1 || submission.semester() > 8
            || submission.subjectId() == null || submission.subjectId().isBlank() || submission.date() == null
            || submission.entries() == null || submission.entries().isEmpty() || submission.entries().size() > 5000)
            throw bad("Select a course, semester, subject, date and a nonempty student roster (maximum 5000).");
        if (submission.date().isAfter(LocalDate.now(ZoneId.of("Asia/Kolkata"))))
            throw bad("Attendance cannot be submitted for a future date.");

        // Serialize writes for this subject, including simultaneous submissions on different servers.
        Subject subject = entities.find(Subject.class, submission.subjectId(), LockModeType.PESSIMISTIC_WRITE);
        if (subject == null || subject.getSemester() != submission.semester().intValue()
            || subject.getCourse() == null || !subject.getCourse().equalsIgnoreCase(submission.course().strip()))
            throw bad("The subject does not belong to the selected course and semester.");

        String course = submission.course().strip();
        Map<String, Student> roster = new HashMap<>();
        for (Student student : students.findAll()) {
            if (student.getCourse() != null && course.equalsIgnoreCase(student.getCourse().strip())
                && submission.semester().equals(student.getSemester())) roster.put(student.getRollNo(), student);
        }
        if (roster.isEmpty()) throw bad("There are no students in this course and semester.");
        Set<String> submitted = new HashSet<>();
        for (Entry entry : submission.entries()) {
            if (entry == null || entry.rollNo() == null || entry.present() == null)
                throw bad("Choose Yes or No for every student.");
            if (!submitted.add(entry.rollNo())) throw bad("The submission contains a duplicate student.");
            if (!roster.containsKey(entry.rollNo())) throw bad("A submitted student is outside this course and semester.");
        }
        if (!submitted.equals(roster.keySet())) throw new ResponseStatusException(HttpStatus.CONFLICT,
            "The class roster changed. Refresh and mark every current student before submitting.");

        Map<String, Attendance> existing = new HashMap<>();
        for (Attendance row : attendance.findBySubject_IdAndDate(subject.getId(), submission.date()))
            existing.put(row.getStudent().getRollNo(), row);
        List<Attendance> toSave = new ArrayList<>();
        for (Entry entry : submission.entries()) {
            Attendance row = existing.get(entry.rollNo());
            if (row == null) {
                row = new Attendance(); row.setStudent(roster.get(entry.rollNo()));
                row.setSubject(subject); row.setDate(submission.date());
            }
            row.setPresent(entry.present()); toSave.add(row);
        }
        attendance.saveAllAndFlush(toSave);
        return new Saved(toSave.size(), subject.getId(), submission.date());
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
