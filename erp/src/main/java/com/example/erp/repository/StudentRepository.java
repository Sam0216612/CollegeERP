package com.example.erp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.erp.model.Student;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {
}