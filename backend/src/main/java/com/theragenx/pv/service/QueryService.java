package com.theragenx.pv.service;

import com.theragenx.pv.exception.InvalidRequestException;
import com.theragenx.pv.model.CreateQueryRequest;
import com.theragenx.pv.model.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class QueryService {

    private final Map<String, List<Query>> store = new ConcurrentHashMap<>();
    private final CaseService caseService;

    public QueryService(CaseService caseService) {
        this.caseService = caseService;
    }

    public Query createQuery(CreateQueryRequest request) {
        if (!caseService.caseExists(request.getCaseId())) {
            throw new InvalidRequestException("Case not found: " + request.getCaseId());
        }

        Query query = new Query(
                UUID.randomUUID().toString(),
                request.getCaseId(),
                request.getFieldPath(),
                request.getQuestion(),
                Instant.now());

        store.computeIfAbsent(request.getCaseId(), k -> new CopyOnWriteArrayList<>()).add(query);
        return query;
    }

    public List<Query> getQueriesForCase(String caseId) {
        if (!caseService.caseExists(caseId)) {
            throw new InvalidRequestException("Case not found: " + caseId);
        }
        return store.getOrDefault(caseId, List.of());
    }
}
