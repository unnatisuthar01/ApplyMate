package com.example.applymate.models;

public class JobApplication {

    private final long id;
    private final long userId;
    private final String companyName;
    private final String jobRole;
    private final String location;
    private final String jobType;
    private final Double salary;
    private final long applicationDate;
    private final String status;
    private final String description;
    private final String jobLink;
    private final String notes;

    public JobApplication(long id, long userId, String companyName, String jobRole,
                          String location, String jobType, Double salary, long applicationDate,
                          String status, String description, String jobLink, String notes) {
        this.id = id;
        this.userId = userId;
        this.companyName = companyName;
        this.jobRole = jobRole;
        this.location = location;
        this.jobType = jobType;
        this.salary = salary;
        this.applicationDate = applicationDate;
        this.status = status;
        this.description = description;
        this.jobLink = jobLink;
        this.notes = notes;
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getJobRole() {
        return jobRole;
    }

    public String getLocation() {
        return location;
    }

    public String getJobType() {
        return jobType;
    }

    public Double getSalary() {
        return salary;
    }

    public long getApplicationDate() {
        return applicationDate;
    }

    public String getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public String getJobLink() {
        return jobLink;
    }

    public String getNotes() {
        return notes;
    }
}
