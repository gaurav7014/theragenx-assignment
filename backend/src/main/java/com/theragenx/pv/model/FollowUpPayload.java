package com.theragenx.pv.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowUpPayload {

    @NotBlank(message = "extracted_at is required")
    private String extractedAt;

    @NotBlank(message = "source_document is required")
    private String sourceDocument;

    private List<String> missingFields;
    private Map<String, Map<String, ExtractedField>> sections;

    @Builder.Default
    private Map<String, Object> extraFields = new LinkedHashMap<>();

    @JsonAnySetter
    public void setExtraField(String key, Object value) {
        extraFields.put(key, value);
    }
}
