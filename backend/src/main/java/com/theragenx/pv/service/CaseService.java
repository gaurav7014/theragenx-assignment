package com.theragenx.pv.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theragenx.pv.exception.CaseNotFoundException;
import com.theragenx.pv.model.CaseRecord;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaseService {

    private final ConcurrentHashMap<String, CaseRecord> store = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public CaseService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadInitialCase() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("case_v1.json");
        if (is == null) {
            throw new IllegalStateException("case_v1.json not found on classpath");
        }
        CaseRecord record = objectMapper.readValue(is, CaseRecord.class);
        store.put(record.getCaseId(), record);
    }

    public CaseRecord getCase(String caseId) {
        CaseRecord record = store.get(caseId);
        if (record == null) {
            throw new CaseNotFoundException(caseId);
        }
        return record;
    }
}
