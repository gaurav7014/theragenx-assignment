package com.theragenx.pv.model;

import com.fasterxml.jackson.annotation.JsonInclude;

public class MergedField {

    private String value;
    private double confidence;
    private String source;
    private String status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String previousValue;

    public MergedField() {}

    public MergedField(ExtractedField field, String status, String previousValue) {
        this.value = field.getValue();
        this.confidence = field.getConfidence();
        this.source = field.getSource();
        this.status = status;
        this.previousValue = previousValue;
    }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPreviousValue() { return previousValue; }
    public void setPreviousValue(String previousValue) { this.previousValue = previousValue; }

    public ExtractedField toExtractedField() {
        ExtractedField f = new ExtractedField();
        f.setValue(this.value);
        f.setConfidence(this.confidence);
        f.setSource(this.source);
        return f;
    }
}
