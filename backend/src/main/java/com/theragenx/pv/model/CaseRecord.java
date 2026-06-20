package com.theragenx.pv.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseRecord {

    private String caseId;
    private int version;
    private String caseClassification;
    private String extractedAt;
    private String sourceDocument;
    private Map<String, Map<String, ExtractedField>> sections;
}
