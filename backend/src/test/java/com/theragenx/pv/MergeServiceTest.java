package com.theragenx.pv;

import com.theragenx.pv.model.CaseRecord;
import com.theragenx.pv.model.ExtractedField;
import com.theragenx.pv.model.FollowUpPayload;
import com.theragenx.pv.model.MergedCase;
import com.theragenx.pv.model.MergedField;
import com.theragenx.pv.service.MergeService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MergeServiceTest {

    private final MergeService mergeService = new MergeService();

    // -------------------------------------------------------------------------
    // Field status scenarios
    // -------------------------------------------------------------------------

    @Test
    void fieldIsUnchangedWhenValueMatches() {
        CaseRecord stored = caseWithField("patient", "age", "62", 0.91);
        FollowUpPayload followUp = followUpWithField("patient", "age", "62", 0.95);

        MergedField result = merge(stored, followUp, "patient", "age");

        assertThat(result.getStatus()).isEqualTo("unchanged");
        assertThat(result.getPreviousValue()).isNull();
    }

    @Test
    void fieldIsOverriddenWhenValueDiffers() {
        CaseRecord stored = caseWithField("patient", "age", "62", 0.91);
        FollowUpPayload followUp = followUpWithField("patient", "age", "63", 0.95);

        MergedField result = merge(stored, followUp, "patient", "age");

        assertThat(result.getStatus()).isEqualTo("overridden");
        assertThat(result.getValue()).isEqualTo("63");
        assertThat(result.getPreviousValue()).isEqualTo("62");
    }

    @Test
    void fieldIsNewWhenAbsentFromStoredCase() {
        CaseRecord stored = caseWithField("patient", "age", "62", 0.91);
        FollowUpPayload followUp = followUpWithField("patient", "blood_type", "A+", 0.88);

        MergedCase merged = mergeService.merge(stored, followUp);

        MergedField ageField = merged.getSections().get("patient").get("age");
        assertThat(ageField.getStatus()).isEqualTo("missing_in_followup");

        MergedField bloodType = merged.getSections().get("patient").get("blood_type");
        assertThat(bloodType.getStatus()).isEqualTo("new");
        assertThat(bloodType.getPreviousValue()).isNull();
    }

    @Test
    void fieldIsMissingInFollowUpWhenAbsentFromPayload() {
        CaseRecord stored = caseWithField("patient", "age", "62", 0.91);
        FollowUpPayload followUp = followUpWithField("patient", "initials", "M.K.", 0.99);

        MergedField result = merge(stored, followUp, "patient", "age");

        assertThat(result.getStatus()).isEqualTo("missing_in_followup");
        assertThat(result.getValue()).isEqualTo("62");
        assertThat(result.getPreviousValue()).isNull();
    }

    @Test
    void fieldIsUnchangedWhenOnlyMetadataChanges() {
        CaseRecord stored = caseWithField("patient", "age", "62", 0.91);
        // same value, higher confidence, different source
        FollowUpPayload followUp = followUpWithFieldAndSource("patient", "age", "62", 0.99, "p.3 §1");

        MergedField result = merge(stored, followUp, "patient", "age");

        assertThat(result.getStatus()).isEqualTo("unchanged");
        // metadata is taken from the follow-up (fresher)
        assertThat(result.getConfidence()).isEqualTo(0.99);
        assertThat(result.getSource()).isEqualTo("p.3 §1");
    }

    // -------------------------------------------------------------------------
    // Version and metadata propagation
    // -------------------------------------------------------------------------

    @Test
    void versionIsIncrementedByOne() {
        CaseRecord stored = baseCase();
        stored.setVersion(3);

        MergedCase merged = mergeService.merge(stored, baseFollowUp());

        assertThat(merged.getVersion()).isEqualTo(4);
    }

    @Test
    void extractedAtAndSourceDocumentTakenFromFollowUp() {
        CaseRecord stored = baseCase();
        FollowUpPayload followUp = baseFollowUp();
        followUp.setExtractedAt("2026-05-01T10:00:00Z");
        followUp.setSourceDocument("followup_v2.pdf");

        MergedCase merged = mergeService.merge(stored, followUp);

        assertThat(merged.getExtractedAt()).isEqualTo("2026-05-01T10:00:00Z");
        assertThat(merged.getSourceDocument()).isEqualTo("followup_v2.pdf");
    }

    @Test
    void caseClassificationRetainedFromStoredCase() {
        CaseRecord stored = baseCase();
        stored.setCaseClassification("significant");

        MergedCase merged = mergeService.merge(stored, baseFollowUp());

        assertThat(merged.getCaseClassification()).isEqualTo("significant");
    }

    @Test
    void missingFieldsArrayPreservedFromFollowUp() {
        CaseRecord stored = baseCase();
        FollowUpPayload followUp = baseFollowUp();
        followUp.setMissingFields(List.of("adverse_event.onset_date", "patient.weight_kg"));

        MergedCase merged = mergeService.merge(stored, followUp);

        assertThat(merged.getMissingFields())
                .containsExactly("adverse_event.onset_date", "patient.weight_kg");
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    void nullSectionsInFollowUpMarksAllFieldsMissingInFollowUp() {
        CaseRecord stored = caseWithField("patient", "age", "62", 0.91);
        FollowUpPayload followUp = baseFollowUp();
        followUp.setSections(null);

        MergedField result = merge(stored, followUp, "patient", "age");

        assertThat(result.getStatus()).isEqualTo("missing_in_followup");
        assertThat(result.getValue()).isEqualTo("62");
    }

    @Test
    void newSectionInFollowUpAllFieldsMarkedNew() {
        CaseRecord stored = baseCase(); // has no "reporter" section
        FollowUpPayload followUp = baseFollowUp();
        followUp.setSections(Map.of(
                "reporter", Map.of(
                        "country", field("India", 0.99, "p.1 §1"))));

        MergedCase merged = mergeService.merge(stored, followUp);

        MergedField country = merged.getSections().get("reporter").get("country");
        assertThat(country.getStatus()).isEqualTo("new");
    }

    @Test
    void mixedStatusesInSingleSection() {
        // stored: age=62, sex=Male
        // follow-up: age=63 (overridden), blood_type=A+ (new), sex absent (missing_in_followup)
        CaseRecord stored = caseWith("patient", Map.of(
                "age", field("62", 0.91, "p.2 §1"),
                "sex", field("Male", 0.99, "p.2 §1")));

        FollowUpPayload followUp = baseFollowUp();
        followUp.setSections(Map.of(
                "patient", Map.of(
                        "age", field("63", 0.95, "p.2 §1"),
                        "blood_type", field("A+", 0.88, "p.3 §1"))));

        MergedCase merged = mergeService.merge(stored, followUp);
        Map<String, MergedField> patient = merged.getSections().get("patient");

        assertThat(patient.get("age").getStatus()).isEqualTo("overridden");
        assertThat(patient.get("age").getPreviousValue()).isEqualTo("62");
        assertThat(patient.get("sex").getStatus()).isEqualTo("missing_in_followup");
        assertThat(patient.get("blood_type").getStatus()).isEqualTo("new");
    }

    @Test
    void storedValueIsPreservedAfterMissingInFollowUp() {
        CaseRecord stored = caseWithField("patient", "sex", "Male", 0.99);
        FollowUpPayload followUp = baseFollowUp();
        followUp.setSections(Map.of("patient", Map.of(
                "age", field("62", 0.91, "p.2 §1"))));

        MergedField sex = merge(stored, followUp, "patient", "sex");

        assertThat(sex.getStatus()).isEqualTo("missing_in_followup");
        assertThat(sex.getValue()).isEqualTo("Male");
        assertThat(sex.getConfidence()).isEqualTo(0.99);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private MergedField merge(CaseRecord stored, FollowUpPayload followUp,
                              String section, String fieldName) {
        return mergeService.merge(stored, followUp).getSections().get(section).get(fieldName);
    }

    private CaseRecord caseWithField(String section, String fieldName, String value, double confidence) {
        return caseWith(section, Map.of(fieldName, field(value, confidence, "p.1 §1")));
    }

    private CaseRecord caseWith(String section, Map<String, ExtractedField> fields) {
        CaseRecord record = baseCase();
        record.setSections(Map.of(section, fields));
        return record;
    }

    private CaseRecord baseCase() {
        CaseRecord record = new CaseRecord();
        record.setCaseId("TEST-001");
        record.setVersion(1);
        record.setCaseClassification("non-significant");
        record.setExtractedAt("2026-01-01T00:00:00Z");
        record.setSourceDocument("doc_v1.pdf");
        record.setSections(Map.of());
        return record;
    }

    private FollowUpPayload followUpWithField(String section, String fieldName,
                                              String value, double confidence) {
        return followUpWithFieldAndSource(section, fieldName, value, confidence, "p.1 §1");
    }

    private FollowUpPayload followUpWithFieldAndSource(String section, String fieldName,
                                                       String value, double confidence, String source) {
        FollowUpPayload payload = baseFollowUp();
        payload.setSections(Map.of(section, Map.of(fieldName, field(value, confidence, source))));
        return payload;
    }

    private FollowUpPayload baseFollowUp() {
        FollowUpPayload payload = new FollowUpPayload();
        payload.setExtractedAt("2026-02-01T00:00:00Z");
        payload.setSourceDocument("doc_v2.pdf");
        return payload;
    }

    private ExtractedField field(String value, double confidence, String source) {
        ExtractedField f = new ExtractedField();
        f.setValue(value);
        f.setConfidence(confidence);
        f.setSource(source);
        return f;
    }
}
