package com.theragenx.pv.service;

import com.theragenx.pv.model.CaseRecord;
import com.theragenx.pv.model.ExtractedField;
import com.theragenx.pv.model.FollowUpPayload;
import com.theragenx.pv.model.MergedCase;
import com.theragenx.pv.model.MergedField;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MergeService {

    public MergedCase merge(CaseRecord stored, FollowUpPayload followUp) {
        Map<String, Map<String, ExtractedField>> storedSections = stored.getSections();
        Map<String, Map<String, ExtractedField>> followUpSections =
                followUp.getSections() != null ? followUp.getSections() : Map.of();

        // Union of section names, stored order first
        Map<String, Map<String, MergedField>> mergedSections = new LinkedHashMap<>();
        for (String section : storedSections.keySet()) {
            mergedSections.put(section, mergeSection(
                    storedSections.getOrDefault(section, Map.of()),
                    followUpSections.getOrDefault(section, Map.of())));
        }
        // Sections that are new in the follow-up
        for (String section : followUpSections.keySet()) {
            if (!mergedSections.containsKey(section)) {
                mergedSections.put(section, mergeSection(Map.of(), followUpSections.get(section)));
            }
        }

        return new MergedCase(
                stored.getCaseId(),
                stored.getVersion() + 1,
                stored.getCaseClassification(),
                followUp.getExtractedAt(),
                followUp.getSourceDocument(),
                followUp.getMissingFields(),
                mergedSections);
    }

    private Map<String, MergedField> mergeSection(
            Map<String, ExtractedField> storedFields,
            Map<String, ExtractedField> followUpFields) {

        Map<String, MergedField> result = new LinkedHashMap<>();

        // Walk stored fields first to preserve order
        for (Map.Entry<String, ExtractedField> entry : storedFields.entrySet()) {
            String name = entry.getKey();
            ExtractedField stored = entry.getValue();
            ExtractedField followUp = followUpFields.get(name);

            if (followUp == null) {
                result.put(name, new MergedField(stored, "missing_in_followup", null));
            } else if (stored.getValue().equals(followUp.getValue())) {
                result.put(name, new MergedField(followUp, "unchanged", null));
            } else {
                result.put(name, new MergedField(followUp, "overridden", stored.getValue()));
            }
        }

        // Fields that are new in the follow-up
        for (Map.Entry<String, ExtractedField> entry : followUpFields.entrySet()) {
            if (!storedFields.containsKey(entry.getKey())) {
                result.put(entry.getKey(), new MergedField(entry.getValue(), "new", null));
            }
        }

        return result;
    }
}
