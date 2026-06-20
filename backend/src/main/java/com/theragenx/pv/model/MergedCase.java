package com.theragenx.pv.model;

import java.util.List;
import java.util.Map;

public class MergedCase {

    private String caseId;
    private int version;
    private String caseClassification;
    private String extractedAt;
    private String sourceDocument;
    private List<String> missingFields;
    private Map<String, Map<String, MergedField>> sections;

    public MergedCase() {}

    public MergedCase(String caseId, int version, String caseClassification,
                      String extractedAt, String sourceDocument,
                      List<String> missingFields,
                      Map<String, Map<String, MergedField>> sections) {
        this.caseId = caseId;
        this.version = version;
        this.caseClassification = caseClassification;
        this.extractedAt = extractedAt;
        this.sourceDocument = sourceDocument;
        this.missingFields = missingFields;
        this.sections = sections;
    }

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

    public List<String> getMissingFields() { return missingFields; }
    public void setMissingFields(List<String> missingFields) { this.missingFields = missingFields; }

    public Map<String, Map<String, MergedField>> getSections() { return sections; }
    public void setSections(Map<String, Map<String, MergedField>> sections) { this.sections = sections; }
}
