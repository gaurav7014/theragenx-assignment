package com.theragenx.pv.model;

import java.time.Instant;

public class Query {

    private String id;
    private String caseId;
    private String fieldPath;
    private String question;
    private Instant createdAt;

    public Query() {}

    public Query(String id, String caseId, String fieldPath, String question, Instant createdAt) {
        this.id = id;
        this.caseId = caseId;
        this.fieldPath = fieldPath;
        this.question = question;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }

    public String getFieldPath() { return fieldPath; }
    public void setFieldPath(String fieldPath) { this.fieldPath = fieldPath; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
