package com.theragenx.pv.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergedField {

    private String value;
    private double confidence;
    private String source;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String previousValue;

    public MergedField(ExtractedField field, String status, String previousValue) {
        this.value = field.getValue();
        this.confidence = field.getConfidence();
        this.source = field.getSource();
        this.status = status;
        this.previousValue = previousValue;
    }
}
