package com.example.applymate.models;

public class Reminder {

    private final long id;
    private final long userId;
    private final Long jobId;
    private final String title;
    private final String description;
    private final String type;
    private final long date;
    private final String time;
    private final String status;

    public Reminder(long id, long userId, Long jobId, String title, String description,
                    String type, long date, String time, String status) {
        this.id = id;
        this.userId = userId;
        this.jobId = jobId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.date = date;
        this.time = time;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public long getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getStatus() {
        return status;
    }
}
