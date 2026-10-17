package com.example.erp.repository;

import com.example.erp.model.Attendance;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    @Query("select a from Attendance a join fetch a.student join fetch a.subject order by a.date desc, a.id")
    List<Attendance> findAllWithDetails();
    List<Attendance> findBySubject_IdAndDate(String subjectId, LocalDate date);
    void deleteByStudent_RollNo(String rollNo);
}
