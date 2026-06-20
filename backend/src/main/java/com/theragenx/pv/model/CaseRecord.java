package com.theragenx.pv.model;

import java.util.Map;

public class CaseRecord {

    private String caseId;
    private int version;
    private String caseClassification;
    private String extractedAt;
    private String sourceDocument;
    private Map<String, Map<String, ExtractedField>> sections;

    public CaseRecord() {}

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public String getCaseClassification() { return caseClassification; }
    public void setCaseClassification(String caseClassification) { this.caseClassification = caseClassification; }

    public String getExtractedAt() { return extractedAt; }
    public void setExtractedAt(String extractedAt) { this.extractedAt = extractedAt; }

    public String getSourceDocument() { return sourceDocument; }
    public void setSourceDocument(String sourceDocument) { this.sourceDocument = sourceDocument; }

    public Map<String, Map<String, ExtractedField>> getSections() { return sections; }
    public void setSections(Map<String, Map<String, ExtractedField>> sections) { this.sections = sections; }
}
