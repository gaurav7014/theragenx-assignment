package com.theragenx.pv.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ApiError {

    private int status;
    private String error;
    private String message;
}
