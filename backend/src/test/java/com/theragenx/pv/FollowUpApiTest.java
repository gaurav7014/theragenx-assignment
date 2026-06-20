package com.theragenx.pv;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theragenx.pv.model.FollowUpPayload;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FollowUpApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final String KNOWN_CASE = "PV-2026-0451";
    private static final String UNKNOWN_CASE = "PV-DOES-NOT-EXIST";
    private static final String FOLLOW_UP_URL = "/cases/{caseId}/follow-ups";

    // -------------------------------------------------------------------------
    // 404 — unknown case
    // -------------------------------------------------------------------------

    @Test
    void followUpReturns404ForUnknownCaseId() throws Exception {
        mockMvc.perform(post(FOLLOW_UP_URL, UNKNOWN_CASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayloadJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString(UNKNOWN_CASE)));
    }

    // -------------------------------------------------------------------------
    // 400 — malformed / missing payload
    // -------------------------------------------------------------------------

    @Test
    void followUpReturns400WhenBodyIsAbsent() throws Exception {
        mockMvc.perform(post(FOLLOW_UP_URL, KNOWN_CASE)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void followUpReturns400WhenRequiredFieldsMissing() throws Exception {
        mockMvc.perform(post(FOLLOW_UP_URL, KNOWN_CASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("extracted_at")))
                .andExpect(jsonPath("$.message", containsString("source_document")));
    }

    @Test
    void followUpReturns400WhenExtractedAtIsBlank() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "extracted_at", "",
                "source_document", "doc.pdf"));

        mockMvc.perform(post(FOLLOW_UP_URL, KNOWN_CASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("extracted_at")));
    }

    // -------------------------------------------------------------------------
    // Happy path — merged response shape
    // -------------------------------------------------------------------------

    @Test
    @DirtiesContext
    void followUpReturns200WithMergedCaseAndCorrectVersion() throws Exception {
        mockMvc.perform(post(FOLLOW_UP_URL, KNOWN_CASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayloadJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.case_id", is(KNOWN_CASE)))
                .andExpect(jsonPath("$.version", is(2)))
                .andExpect(jsonPath("$.source_document", is("followup_doc.pdf")))
                .andExpect(jsonPath("$.missing_fields", hasSize(1)))
                .andExpect(jsonPath("$.missing_fields[0]", is("patient.weight_kg")));
    }

    @Test
    @DirtiesContext
    void followUpOverriddenFieldHasPreviousValue() throws Exception {
        mockMvc.perform(post(FOLLOW_UP_URL, KNOWN_CASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayloadJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.patient.age.status", is("overridden")))
                .andExpect(jsonPath("$.sections.patient.age.value", is("63")))
                .andExpect(jsonPath("$.sections.patient.age.previous_value", is("62")));
    }

    @Test
    @DirtiesContext
    void followUpUnchangedFieldHasNoMedatataConflict() throws Exception {
        // initials value is same as v1, only confidence changes → must be "unchanged"
        mockMvc.perform(post(FOLLOW_UP_URL, KNOWN_CASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayloadJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.patient.initials.status", is("unchanged")))
                .andExpect(jsonPath("$.sections.patient.initials.previous_value").doesNotExist());
    }

    @Test
    @DirtiesContext
    void storedCaseVersionBumpsAfterFollowUp() throws Exception {
        mockMvc.perform(post(FOLLOW_UP_URL, KNOWN_CASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayloadJson()))
                .andExpect(status().isOk());

        // Subsequent GET should reflect the new version
        mockMvc.perform(get("/cases/{caseId}", KNOWN_CASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version", is(2)))
                .andExpect(jsonPath("$.source_document", is("followup_doc.pdf")));
    }

    // -------------------------------------------------------------------------
    // Status persistence — GET after POST must show statuses
    // -------------------------------------------------------------------------

    @Test
    @DirtiesContext
    void absentReporterSectionFieldsAreMissingInFollowUpOnBothPostAndGet() throws Exception {
        FollowUpPayload payload = new FollowUpPayload();
        payload.setExtractedAt("2026-05-01T10:00:00Z");
        payload.setSourceDocument("followup_no_reporter.pdf");
        payload.setMissingFields(java.util.List.of());
        payload.setSections(Map.of(
                "patient", Map.of(
                        "initials", field("M.K.", 0.99, "p.2 §1"))));
        String body = objectMapper.writeValueAsString(payload);

        mockMvc.perform(post(FOLLOW_UP_URL, KNOWN_CASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.reporter.qualification.status", is("missing_in_followup")))
                .andExpect(jsonPath("$.sections.reporter.qualification.value", is("Physician")))
                .andExpect(jsonPath("$.sections.reporter.country.status", is("missing_in_followup")))
                .andExpect(jsonPath("$.sections.reporter.country.value", is("India")));

        mockMvc.perform(get("/cases/{caseId}", KNOWN_CASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.reporter.qualification.status", is("missing_in_followup")))
                .andExpect(jsonPath("$.sections.reporter.qualification.value", is("Physician")))
                .andExpect(jsonPath("$.sections.reporter.country.status", is("missing_in_followup")))
                .andExpect(jsonPath("$.sections.reporter.country.value", is("India")));
    }

    // -------------------------------------------------------------------------
    // Query API — 400 on unknown caseId
    // -------------------------------------------------------------------------

    @Test
    void queryPostReturns400ForUnknownCaseId() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "case_id", UNKNOWN_CASE,
                "field_path", "patient.age",
                "question", "Is this correct?"));

        mockMvc.perform(post("/queries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString(UNKNOWN_CASE)));
    }

    @Test
    void queryGetReturns400ForUnknownCaseId() throws Exception {
        mockMvc.perform(get("/queries").param("caseId", UNKNOWN_CASE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void queryPostReturns400WhenFieldsMissing() throws Exception {
        mockMvc.perform(post("/queries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("case_id")));
    }

    @Test
    void queryPostCreatesQueryAndGetReturnsIt() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "case_id", KNOWN_CASE,
                "field_path", "adverse_event.outcome",
                "question", "Why did the outcome change?"));

        mockMvc.perform(post("/queries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.case_id", is(KNOWN_CASE)))
                .andExpect(jsonPath("$.field_path", is("adverse_event.outcome")))
                .andExpect(jsonPath("$.created_at").isNotEmpty());

        mockMvc.perform(get("/queries").param("caseId", KNOWN_CASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // -------------------------------------------------------------------------
    // Payload helper
    // -------------------------------------------------------------------------

    private String validPayloadJson() throws Exception {
        // initials: same value as v1 (M.K.) but higher confidence → unchanged
        // age: different value (63 vs 62) → overridden
        // sex, weight_kg absent from follow-up → missing_in_followup
        FollowUpPayload payload = new FollowUpPayload();
        payload.setExtractedAt("2026-05-01T10:00:00Z");
        payload.setSourceDocument("followup_doc.pdf");
        payload.setMissingFields(java.util.List.of("patient.weight_kg"));
        payload.setSections(Map.of(
                "patient", Map.of(
                        "initials", field("M.K.", 0.99, "p.2 §1"),
                        "age", field("63", 0.95, "p.2 §1"))));
        return objectMapper.writeValueAsString(payload);
    }

    private com.theragenx.pv.model.ExtractedField field(String value, double confidence, String source) {
        com.theragenx.pv.model.ExtractedField f = new com.theragenx.pv.model.ExtractedField();
        f.setValue(value);
        f.setConfidence(confidence);
        f.setSource(source);
        return f;
    }
}
