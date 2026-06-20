package com.theragenx.pv.controller;

import com.theragenx.pv.model.CaseRecord;
import com.theragenx.pv.model.FollowUpPayload;
import com.theragenx.pv.model.MergedCase;
import com.theragenx.pv.service.CaseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/cases")
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @GetMapping
    public ResponseEntity<Collection<MergedCase>> getAllCases() {
        return ResponseEntity.ok(caseService.getAllCases());
    }

    @GetMapping("/{caseId}")
    public ResponseEntity<MergedCase> getCase(@PathVariable String caseId) {
        return ResponseEntity.ok(caseService.getCase(caseId));
    }

    @PutMapping("/{caseId}")
    public ResponseEntity<MergedCase> replaceCase(
            @PathVariable String caseId,
            @RequestBody CaseRecord record) {
        return ResponseEntity.ok(caseService.replaceCase(caseId, record));
    }

    @PostMapping("/{caseId}/follow-ups")
    public ResponseEntity<MergedCase> submitFollowUp(
            @PathVariable String caseId,
            @Valid @RequestBody FollowUpPayload payload) {
        return ResponseEntity.ok(caseService.submitFollowUp(caseId, payload));
    }
}
