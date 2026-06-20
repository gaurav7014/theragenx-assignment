package com.theragenx.pv.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Query {

    private String id;
    private String caseId;
    private String fieldPath;
    private String question;
    private Instant createdAt;
}
