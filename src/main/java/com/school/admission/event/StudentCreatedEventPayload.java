package com.school.admission.event;

/**
 * Payload data for a StudentCreated event.
 */
public class StudentCreatedEventPayload {
    private String studentId;
    private String firstName;
    private String lastName;
    private String email;
    private String grade;
    private String section;
    private String rollNumber;

    public StudentCreatedEventPayload() {}

    public StudentCreatedEventPayload(String studentId, String firstName, String lastName, String email, String grade, String section, String rollNumber) {
        this.studentId = studentId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.grade = grade;
        this.section = section;
        this.rollNumber = rollNumber;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
}
