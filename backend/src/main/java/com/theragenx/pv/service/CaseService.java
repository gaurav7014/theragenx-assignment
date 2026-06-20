package com.theragenx.pv.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theragenx.pv.exception.CaseNotFoundException;
import com.theragenx.pv.model.CaseRecord;
import com.theragenx.pv.model.ExtractedField;
import com.theragenx.pv.model.FollowUpPayload;
import com.theragenx.pv.model.MergedCase;
import com.theragenx.pv.model.MergedField;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaseService {

    private final ConcurrentHashMap<String, MergedCase> store = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final MergeService mergeService;

    public CaseService(ObjectMapper objectMapper, MergeService mergeService) {
        this.objectMapper = objectMapper;
        this.mergeService = mergeService;
    }

    @PostConstruct
    void loadInitialCase() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("case_v1.json");
        if (is == null) {
            throw new IllegalStateException("case_v1.json not found on classpath");
        }
        CaseRecord record = objectMapper.readValue(is, CaseRecord.class);
        store.put(record.getCaseId(), toMergedCase(record));
    }

    public Collection<MergedCase> getAllCases() {
        return store.values();
    }

    public boolean caseExists(String caseId) {
        return store.containsKey(caseId);
    }

    public MergedCase getCase(String caseId) {
        MergedCase record = store.get(caseId);
        if (record == null) {
            throw new CaseNotFoundException(caseId);
        }
        return record;
    }

    public MergedCase replaceCase(String caseId, CaseRecord record) {
        if (!store.containsKey(caseId)) {
            throw new CaseNotFoundException(caseId);
        }
        record.setCaseId(caseId);
        MergedCase merged = toMergedCase(record);
        store.put(caseId, merged);
        return merged;
    }

    public MergedCase submitFollowUp(String caseId, FollowUpPayload payload) {
        MergedCase stored = getCase(caseId);
        MergedCase merged = mergeService.merge(stored, payload);
        store.put(caseId, merged);
        return merged;
    }

    private MergedCase toMergedCase(CaseRecord record) {
        Map<String, Map<String, MergedField>> mergedSections = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, ExtractedField>> sectionEntry : record.getSections().entrySet()) {
            Map<String, MergedField> fields = new LinkedHashMap<>();
            for (Map.Entry<String, ExtractedField> fieldEntry : sectionEntry.getValue().entrySet()) {
                ExtractedField f = fieldEntry.getValue();
                fields.put(fieldEntry.getKey(), new MergedField(f.getValue(), f.getConfidence(), f.getSource(), null, null));
            }
            mergedSections.put(sectionEntry.getKey(), fields);
        }
        return new MergedCase(record.getCaseId(), record.getVersion(), record.getCaseClassification(),
                record.getExtractedAt(), record.getSourceDocument(), null, mergedSections, null);
    }
}
