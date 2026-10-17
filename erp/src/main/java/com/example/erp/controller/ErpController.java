package com.example.erp.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PatchMapping;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.erp.service.StudentImportService;
import com.example.erp.model.Marks;
import com.example.erp.model.Student;
import com.example.erp.model.Subject;
import com.example.erp.repository.AttendanceRepository;
import org.springframework.transaction.annotation.Transactional;
import com.example.erp.repository.MarksRepository;
import com.example.erp.repository.StudentRepository;
import com.example.erp.repository.SubjectRepository;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ErpController {

    // Constructor to print your custom message on startup
    public ErpController() {
        System.out.println(" SERVER STARTED SIR ");
    }

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EntityManager entities;

    @Autowired
    private MarksRepository marksRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private StudentImportService studentImportService;

    // --- STUDENT ENDPOINTS ---
    @GetMapping("/students")
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @GetMapping("/students/{rollNo}")
    public Student getStudentByRollNo(@PathVariable String rollNo) {
        return studentRepository.findById(rollNo).orElse(null);
    }

    @PostMapping("/students")
    public Student createStudent(@RequestBody Student student) {
        return studentImportService.createStudent(student);
    }

    @Transactional
    @DeleteMapping("/students/{rollNo}")
    public void deleteStudent(@PathVariable String rollNo) {
        attendanceRepository.deleteByStudent_RollNo(rollNo);
        attendanceRepository.flush();
        studentRepository.deleteById(rollNo);
    }

    // --- SUBJECT ENDPOINTS ---
    @GetMapping("/subjects")
    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    @Transactional
    @PostMapping("/subjects")
    public Subject createSubject(@RequestBody Subject subject) {
        subject.setCourse(validCourse(subject.getCourse()));
        if (subject.getSemester() < 1 || subject.getSemester() > 8 || subject.getCredits() < 1 || subject.getCredits() > 6)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose semester 1–8 and credits 1–6.");
        if (subject.getName() == null || subject.getName().isBlank() || subject.getCode() == null || subject.getCode().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject name and code are required.");
        subject.setCode(subject.getCode().strip());
        subject.setName(subject.getName().strip());
        if (subject.getCode().length() > 200 || subject.getName().length() > 255)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject code/name is too long.");
        subject.setId(subject.getCourse() + "-" + subject.getCode());
        if (subjectRepository.existsById(subject.getId()) || subjectRepository.findAll().stream().anyMatch(existing ->
                subject.getCourse().equalsIgnoreCase(existing.getCourse()) && existing.getCode() != null
                && subject.getCode().equalsIgnoreCase(existing.getCode().strip())))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Subject code already exists in this course.");
        entities.persist(subject);
        entities.flush();
        return subject;
    }

    @Transactional
    @PatchMapping("/subjects/{id}/course")
    public Subject assignSubjectCourse(@PathVariable String id, @RequestBody Map<String, String> body) {
        String course = validCourse(body.get("course"));
        Subject subject = entities.find(Subject.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (subject == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found.");
        if (subject.getCourse() != null && !subject.getCourse().isBlank() && !course.equals(subject.getCourse()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This subject already has a course. Create a separate subject code for another course.");
        if (subjectRepository.findAll().stream().anyMatch(existing -> !existing.getId().equals(id)
                && course.equalsIgnoreCase(existing.getCourse()) && existing.getCode() != null
                && existing.getCode().equalsIgnoreCase(subject.getCode())))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This subject code already exists in the selected course. Assign existing subjects before adding new ones.");
        subject.setCourse(course);
        return subjectRepository.save(subject);
    }

    private String validCourse(String value) {
        String course = value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
        if (!course.equals("AIML") && !course.equals("CSE"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select AIML or CSE.");
        return course;
    }

    // --- MARKS ENDPOINTS ---
    @GetMapping("/marks")
    public Map<String, Map<String, Marks>> getAllMarks() {
        List<Marks> allMarks = marksRepository.findAll();
        Map<String, Map<String, Marks>> response = new HashMap<>();
        
        for (Marks m : allMarks) {
            response.putIfAbsent(m.getRollNo(), new HashMap<>());
            response.get(m.getRollNo()).put(m.getSubjectId(), m);
        }
        return response;
    }

    @Transactional
    @PostMapping("/marks")
    public Marks saveOrUpdateMarks(@RequestBody Marks marks) {
        Subject subject = entities.find(Subject.class, marks.getSubjectId(), LockModeType.PESSIMISTIC_WRITE);
        Student student = studentRepository.findById(marks.getRollNo()).orElse(null);
        if (subject == null || student == null || subject.getCourse() == null || student.getCourse() == null
            || !subject.getCourse().equalsIgnoreCase(student.getCourse().strip())
            || student.getSemester() == null || student.getSemester() != subject.getSemester())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student and subject must have the same course and semester.");
        if (marks.getInternal() < 0 || marks.getInternal() > 20 || marks.getLab() < 0 || marks.getLab() > 30
            || marks.getEndSem() < 0 || marks.getEndSem() > 50)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Marks must be within 20, 30 and 50.");
        Marks existing = marksRepository.findByRollNoAndSubjectId(marks.getRollNo(), marks.getSubjectId());
        if (existing != null) {
            existing.setInternal(marks.getInternal());
            existing.setLab(marks.getLab());
            existing.setEndSem(marks.getEndSem());
            return marksRepository.save(existing);
        }
        return marksRepository.save(marks);
    }
}