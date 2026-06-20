package com.theragenx.pv.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergedCase {

    private String caseId;
    private int version;
    private String caseClassification;
    private String extractedAt;
    private String sourceDocument;
    private List<String> missingFields;
    private Map<String, Map<String, MergedField>> sections;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> extraFields;

    @JsonAnyGetter
    public Map<String, Object> getExtraFields() {
        return extraFields;
    }
}
