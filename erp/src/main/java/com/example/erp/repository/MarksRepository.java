package com.example.erp.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.erp.model.Marks;

@Repository
public interface MarksRepository extends JpaRepository<Marks, Long> {
    Marks findByRollNoAndSubjectId(String rollNo, String subjectId);
}