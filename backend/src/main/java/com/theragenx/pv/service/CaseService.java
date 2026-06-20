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

    private final ConcurrentHashMap<String, CaseRecord> store = new ConcurrentHashMap<>();
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
        store.put(record.getCaseId(), record);
    }

    public Collection<CaseRecord> getAllCases() {
        return store.values();
    }

    public boolean caseExists(String caseId) {
        return store.containsKey(caseId);
    }

    public CaseRecord getCase(String caseId) {
        CaseRecord record = store.get(caseId);
        if (record == null) {
            throw new CaseNotFoundException(caseId);
        }
        return record;
    }

    public MergedCase submitFollowUp(String caseId, FollowUpPayload payload) {
        CaseRecord stored = getCase(caseId);
        MergedCase merged = mergeService.merge(stored, payload);
        store.put(caseId, toCleanRecord(merged));
        return merged;
    }

    // Converts a MergedCase back into a plain CaseRecord for storage.
    // MergedField already holds the correct current value/confidence/source for every status.
    private CaseRecord toCleanRecord(MergedCase merged) {
        Map<String, Map<String, ExtractedField>> cleanSections = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, MergedField>> sectionEntry : merged.getSections().entrySet()) {
            Map<String, ExtractedField> cleanFields = new LinkedHashMap<>();
            for (Map.Entry<String, MergedField> fieldEntry : sectionEntry.getValue().entrySet()) {
                cleanFields.put(fieldEntry.getKey(), fieldEntry.getValue().toExtractedField());
            }
            cleanSections.put(sectionEntry.getKey(), cleanFields);
        }

        CaseRecord updated = new CaseRecord();
        updated.setCaseId(merged.getCaseId());
        updated.setVersion(merged.getVersion());
        updated.setCaseClassification(merged.getCaseClassification());
        updated.setExtractedAt(merged.getExtractedAt());
        updated.setSourceDocument(merged.getSourceDocument());
        updated.setSections(cleanSections);
        return updated;
    }
}
