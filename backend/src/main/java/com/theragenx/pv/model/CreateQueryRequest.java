package com.theragenx.pv.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQueryRequest {

    @NotBlank(message = "case_id is required")
    private String caseId;

    @NotBlank(message = "field_path is required")
    private String fieldPath;

    @NotBlank(message = "question is required")
    private String question;
}
