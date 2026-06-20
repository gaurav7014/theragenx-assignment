package com.theragenx.pv.model;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public class FollowUpPayload {

    @NotBlank(message = "extracted_at is required")
    private String extractedAt;

    @NotBlank(message = "source_document is required")
    private String sourceDocument;

    private List<String> missingFields;
    private Map<String, Map<String, ExtractedField>> sections;

    public FollowUpPayload() {}

    public String getExtractedAt() { return extractedAt; }
    public void setExtractedAt(String extractedAt) { this.extractedAt = extractedAt; }

    public String getSourceDocument() { return sourceDocument; }
    public void setSourceDocument(String sourceDocument) { this.sourceDocument = sourceDocument; }

    public List<String> getMissingFields() { return missingFields; }
    public void setMissingFields(List<String> missingFields) { this.missingFields = missingFields; }

    public Map<String, Map<String, ExtractedField>> getSections() { return sections; }
    public void setSections(Map<String, Map<String, ExtractedField>> sections) { this.sections = sections; }
}
