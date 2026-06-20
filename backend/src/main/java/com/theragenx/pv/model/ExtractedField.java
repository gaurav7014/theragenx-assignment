package com.theragenx.pv.model;

public class ExtractedField {

    private String value;
    private double confidence;
    private String source;

    public ExtractedField() {}

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
