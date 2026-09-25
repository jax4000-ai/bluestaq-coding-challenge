package com.example.notes.audit;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;

public interface AuditRecordRepository extends ReactiveCrudRepository<AuditRecord, Long> {
    Flux<AuditRecord> findAllByTeamIdOrderByOccurredAtDesc(String teamId);
}
