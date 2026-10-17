# AIML College ERP & Academic Management System

A full-stack college ERP built with **Java 21, Spring Boot, MySQL, HTML, JavaScript, Tailwind CSS, and Chart.js**. The project brings academic records into one interface for managing students, course-specific subjects, marks, attendance, and performance summaries.

Developed as a **Database Management Systems (DBMS) Project-Based Learning project** for **TY ECE AIML A, MIT World Peace University**.

## Why this project?

Academic records spread across spreadsheets are difficult to maintain and consolidate. Duplicate entries, disconnected marks sheets, and manual calculations can lead to inconsistent reports. This ERP uses a shared relational database and a browser-based interface to simplify academic administration.

## Project scope

The following modules describe the project's intended scope, including the latest requested enhancements. Availability depends on the source version included in this repository.

| Module | Purpose |
| --- | --- |
| Dashboard | Summarize student counts, subjects, marks, and academic performance from connected records |
| Student management | Create, view, edit, and delete student profiles |
| Subject management | Organize subjects by course and select a course when adding a subject |
| Marks management | View and update student marks for the selected course and subjects |
| Marks sorting | Sort total marks in ascending or descending order |
| Missing marks checklist | Identify records that still require marks entry |
| At-risk student view | Help faculty identify students who may need academic support |
| Attendance management | Filter by course, semester, and subject, then record present or absent status |
| Performance charts | Visualize academic summaries with Chart.js |

## Technology stack

| Layer | Technologies |
| --- | --- |
| Frontend | HTML, JavaScript, Tailwind CSS |
| Visualizations | Chart.js |
| Backend | Java 21, Spring Boot |
| Database | MySQL |
| Communication | REST APIs and JSON |

## Architecture

The frontend operates as a single-page interface. It sends requests to the Spring Boot backend, which handles application logic and database access. MySQL stores the academic records used by the management screens and dashboard.

- **Frontend:** forms, navigation, record tables, filters, and charts.
- **Backend:** REST endpoints, input validation, and academic workflows.
- **Database:** persistent records and relationships between academic entities.

### Database design

The core academic entities are **students**, **subjects**, and **marks**. Course and semester information support filtering. Attendance adds subject-specific presence records.

Each marks record relates to a student and a subject. The exact table names, columns, constraints, and attendance structure should follow the backend entities or SQL schema shipped with the repository.

### Why MySQL?

College academic data has clear relationships. A relational database provides primary keys, foreign keys, SQL joins, and transactions to help maintain consistency and generate reports. MySQL fits the Java Spring Boot stack and the project's DBMS learning objectives.

## Getting started

### Prerequisites

- Java Development Kit (JDK) 21.
- A running MySQL server.
- The build tool or wrapper included with the backend.
- A modern browser and a local static HTTP server for the frontend.

### 1. Get the source

Clone this repository using its GitHub clone URL, or download and extract the source ZIP.

### 2. Configure MySQL

Create a database for the application. For example:

```sql
CREATE DATABASE college_erp;
```

Configure the backend's datasource in its existing Spring Boot configuration file. These environment variables are an example configuration, not a statement of the repository's current defaults:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

Set `DB_URL` to a JDBC connection URL such as `jdbc:mysql://localhost:3306/college_erp`, and set the username and password for your MySQL account. Follow the repository's SQL scripts, migrations, or configured schema-generation method to initialize the tables.

### 3. Run the backend

Open the backend project and run its Spring Boot application class. If it includes a Maven wrapper, run the following from the directory containing `pom.xml`:

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

If the project uses a different build tool, use its included run instructions instead. Confirm the server address and port from the application configuration or startup logs.

### 4. Run the frontend

Serve the frontend directory through a local HTTP server, such as VS Code Live Server. Configure its API base URL to match the backend address. If the frontend and backend use different origins, configure backend CORS to allow the frontend origin.

### 5. Check the integration

Create a sample student and subject, enter a marks record, and refresh the page to check persistence. Then edit the marks and compare the dashboard summary with the stored records. For attendance, select a course, semester, and subject, submit presence values, and reload the view.

## Typical workflows

### Academic records

1. Add students and assign their academic information.
2. Select a course and maintain its subjects.
3. Enter or update marks for the relevant students and subjects.
4. Review missing entries and academic performance summaries.

### Attendance

1. Select the course, semester, and subject.
2. Choose **Mark Attendance**.
3. Select present or absent for each student.
4. Submit the records and reload to check the saved values.

## Deployment overview

The frontend, Java backend, and MySQL database are separate deployment components. Configure the hosted frontend with the backend's public API URL and configure the backend with its database connection. Keep database credentials on the server, outside frontend code and version control.

## Future scope

- Role-based authentication for administrators, faculty, and students.
- PDF academic reports and export options.
- Audit history for marks and attendance changes.
- Student self-service portal.
- Automated database backups.

## Academic focus

This project demonstrates relational modeling, CRUD operations, database connectivity, and reporting across related academic records. It also connects DBMS concepts to a practical full-stack application.

## Repository description

> College ERP and academic management system built with Java Spring Boot, MySQL, and Tailwind CSS, covering student records, course-wise subjects, marks, attendance, and performance dashboards.

## License

Refer to the repository's `LICENSE` file if one is provided.
