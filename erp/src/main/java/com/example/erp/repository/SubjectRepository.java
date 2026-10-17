package com.example.erp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.erp.model.Subject;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, String> {
}