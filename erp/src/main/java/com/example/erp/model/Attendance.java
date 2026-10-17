package com.example.erp.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "attendance", uniqueConstraints = @UniqueConstraint(
    name = "uk_attendance_student_subject_date", columnNames = {"student_roll_no", "subject_id", "attendance_date"}))
public class Attendance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_roll_no", nullable = false,
        foreignKey = @ForeignKey(name = "fk_attendance_student"))
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_attendance_subject"))
    private Subject subject;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate date;
    @Column(nullable = false)
    private boolean present;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Attendance() {}
    @PrePersist @PreUpdate
    public void touch() { updatedAt = LocalDateTime.now(ZoneOffset.UTC); }
    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public boolean isPresent() { return present; }
    public void setPresent(boolean present) { this.present = present; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
