package com.theragenx.pv.service;

import com.theragenx.pv.model.ExtractedField;
import com.theragenx.pv.model.FollowUpPayload;
import com.theragenx.pv.model.MergedCase;
import com.theragenx.pv.model.MergedField;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MergeService {

    public MergedCase merge(MergedCase stored, FollowUpPayload followUp) {
        Map<String, Map<String, MergedField>> storedSections = stored.getSections();
        Map<String, Map<String, ExtractedField>> followUpSections =
                followUp.getSections() != null ? followUp.getSections() : Map.of();

        Map<String, Map<String, MergedField>> mergedSections = new LinkedHashMap<>();
        for (String section : storedSections.keySet()) {
            mergedSections.put(section, mergeSection(
                    storedSections.getOrDefault(section, Map.of()),
                    followUpSections.getOrDefault(section, Map.of())));
        }
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
                mergedSections,
                followUp.getExtraFields().isEmpty() ? null : followUp.getExtraFields());
    }

    private Map<String, MergedField> mergeSection(
            Map<String, MergedField> storedFields,
            Map<String, ExtractedField> followUpFields) {

        Map<String, MergedField> result = new LinkedHashMap<>();

        for (Map.Entry<String, MergedField> entry : storedFields.entrySet()) {
            String name = entry.getKey();
            MergedField stored = entry.getValue();
            ExtractedField followUp = followUpFields.get(name);

            if (followUp == null) {
                result.put(name, new MergedField(stored.getValue(), stored.getConfidence(), stored.getSource(), "missing_in_followup", null));
            } else if (stored.getValue().equals(followUp.getValue())) {
                result.put(name, new MergedField(followUp, "unchanged", null));
            } else {
                result.put(name, new MergedField(followUp, "overridden", stored.getValue()));
            }
        }

        for (Map.Entry<String, ExtractedField> entry : followUpFields.entrySet()) {
            if (!storedFields.containsKey(entry.getKey())) {
                result.put(entry.getKey(), new MergedField(entry.getValue(), "new", null));
            }
        }

        return result;
    }
}
