package com.example.notes.audit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.notes.common.DataAccessDeniedException;
import com.example.notes.security.DataClassification;
import com.example.notes.security.RequestActorResolver;
import com.example.notes.security.ValidTeamId;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/teams/{teamId}/audit")
public class AuditController {
    private final AuditService auditService;
    private final RequestActorResolver actorResolver;

    public AuditController(AuditService auditService, RequestActorResolver actorResolver) {
        this.auditService = auditService;
        this.actorResolver = actorResolver;
    }

    @GetMapping
    public Flux<AuditRecord> list(
            @PathVariable @ValidTeamId String teamId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Clearance") DataClassification clearance) {
        if (actorResolver.resolve(userId, clearance).clearance() != DataClassification.CUI) {
            return Flux.error(new DataAccessDeniedException("CUI clearance is required to view the audit trail"));
        }
        return auditService.list(teamId);
    }
}
