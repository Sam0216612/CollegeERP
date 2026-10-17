package com.example.erp.service;

import com.example.erp.model.Attendance;
import com.example.erp.model.Student;
import com.example.erp.model.Subject;
import com.example.erp.repository.AttendanceRepository;
import com.example.erp.repository.StudentRepository;
import com.example.erp.service.AttendanceService.Entry;
import com.example.erp.service.AttendanceService.Submission;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AttendanceServiceTest {
    private AttendanceRepository attendance;
    private StudentRepository students;
    private EntityManager entities;
    private AttendanceService service;
    private Subject subject;
    private Student first, second;
    private final LocalDate day = LocalDate.of(2026, 1, 1);

    @BeforeEach
    void setup() {
        attendance = mock(AttendanceRepository.class);
        students = mock(StudentRepository.class);
        entities = mock(EntityManager.class);
        service = new AttendanceService(attendance, students, entities);
        subject = new Subject(); subject.setId("dbms"); subject.setSemester(3); subject.setCourse("AIML");
        first = new Student("1", "First", "AIML", 3, "first@mitwpu.edu.in", "Active");
        second = new Student("2", "Second", "AIML", 3, "second@mitwpu.edu.in", "Active");
        when(entities.find(Subject.class, "dbms", LockModeType.PESSIMISTIC_WRITE)).thenReturn(subject);
        when(students.findAll()).thenReturn(List.of(first, second));
        when(attendance.findBySubject_IdAndDate("dbms", day)).thenReturn(List.of());
    }

    private Submission payload(List<Entry> entries) {
        return new Submission("AIML", 3, "dbms", day, entries);
    }

    @Test
    void savesYesAndNoWithoutConvertingAbsenceIntoMissing() {
        var result = service.submit(payload(List.of(new Entry("1", true), new Entry("2", false))));
        assertEquals(2, result.saved());
        ArgumentCaptor<List<Attendance>> captor = ArgumentCaptor.forClass(List.class);
        verify(attendance).saveAllAndFlush(captor.capture());
        assertTrue(captor.getValue().get(0).isPresent());
        assertFalse(captor.getValue().get(1).isPresent());
        assertEquals(day, captor.getValue().get(1).getDate());
        assertEquals("dbms", captor.getValue().get(1).getSubject().getId());
    }

    @Test
    void updatingSameDayReusesExistingEntity() {
        Attendance existing = new Attendance(); existing.setStudent(first); existing.setSubject(subject);
        existing.setDate(day); existing.setPresent(false);
        when(attendance.findBySubject_IdAndDate("dbms", day)).thenReturn(List.of(existing));
        service.submit(payload(List.of(new Entry("1", true), new Entry("2", false))));
        ArgumentCaptor<List<Attendance>> captor = ArgumentCaptor.forClass(List.class);
        verify(attendance).saveAllAndFlush(captor.capture());
        assertSame(existing, captor.getValue().get(0));
        assertTrue(existing.isPresent());
        assertEquals(2, captor.getValue().size());
    }

    @Test
    void rejectsMissingAnswerBeforeWritingAnything() {
        assertThrows(ResponseStatusException.class, () -> service.submit(payload(
            List.of(new Entry("1", true), new Entry("2", null)))));
        verify(attendance, never()).saveAllAndFlush(anyList());
    }

    @Test
    void rejectsDuplicateStudentBeforeWritingAnything() {
        assertThrows(ResponseStatusException.class, () -> service.submit(payload(
            List.of(new Entry("1", true), new Entry("1", false)))));
        verify(attendance, never()).saveAllAndFlush(anyList());
    }

    @Test
    void rejectsStudentFromAnotherCohortBeforeWritingAnything() {
        assertThrows(ResponseStatusException.class, () -> service.submit(payload(
            List.of(new Entry("1", true), new Entry("other-course", false)))));
        verify(attendance, never()).saveAllAndFlush(anyList());
    }

    @Test
    void rejectsChangedRosterWithConflict() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.submit(payload(List.of(new Entry("1", true)))));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(attendance, never()).saveAllAndFlush(anyList());
    }

    @Test
    void rejectsWrongSubjectSemester() {
        subject.setSemester(2);
        assertThrows(ResponseStatusException.class, () -> service.submit(payload(
            List.of(new Entry("1", true), new Entry("2", false)))));
        verify(attendance, never()).saveAllAndFlush(anyList());
    }

    @Test
    void rejectsWrongSubjectCourse() {
        subject.setCourse("CSE");
        assertThrows(ResponseStatusException.class, () -> service.submit(payload(
            List.of(new Entry("1", true), new Entry("2", false)))));
        verify(attendance, never()).saveAllAndFlush(anyList());
    }

    @Test
    void rejectsFutureDate() {
        assertThrows(ResponseStatusException.class, () -> service.submit(new Submission("AIML", 3, "dbms",
            LocalDate.of(2999, 1, 1), List.of(new Entry("1", true), new Entry("2", false)))));
        verifyNoInteractions(attendance);
    }
}
