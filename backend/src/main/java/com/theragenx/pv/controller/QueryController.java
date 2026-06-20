package com.theragenx.pv.controller;

import com.theragenx.pv.model.CreateQueryRequest;
import com.theragenx.pv.model.Query;
import com.theragenx.pv.service.QueryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/queries")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<Query> createQuery(@Valid @RequestBody CreateQueryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(queryService.createQuery(request));
    }

    @GetMapping
    public ResponseEntity<List<Query>> getQueriesForCase(@RequestParam String caseId) {
        return ResponseEntity.ok(queryService.getQueriesForCase(caseId));
    }
}
