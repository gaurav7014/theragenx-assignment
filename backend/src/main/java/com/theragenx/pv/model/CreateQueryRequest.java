package com.theragenx.pv.model;

import jakarta.validation.constraints.NotBlank;

public class CreateQueryRequest {

    @NotBlank(message = "case_id is required")
    private String caseId;

    @NotBlank(message = "field_path is required")
    private String fieldPath;

    @NotBlank(message = "question is required")
    private String question;

    public CreateQueryRequest() {}

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }

    public String getFieldPath() { return fieldPath; }
    public void setFieldPath(String fieldPath) { this.fieldPath = fieldPath; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
}
