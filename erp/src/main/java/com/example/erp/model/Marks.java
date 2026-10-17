package com.example.erp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "marks")
public class Marks {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String rollNo;
    private String subjectId;
    private int internal;
    private int lab;
    private int endSem;

    public Marks() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public int getInternal() { return internal; }
    public void setInternal(int internal) { this.internal = internal; }

    public int getLab() { return lab; }
    public void setLab(int lab) { this.lab = lab; }

    public int getEndSem() { return endSem; }
    public void setEndSem(int endSem) { this.endSem = endSem; }
}