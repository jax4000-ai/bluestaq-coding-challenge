package com.example.notes.audit;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.example.notes.security.DataClassification;

@Table("audit_records")
public record AuditRecord(
        @Id @Column("id") Long id,
        @Column("team_id") String teamId,
        @Column("actor_id") String actorId,
        @Column("action") String action,
        @Column("note_id") Long noteId,
        @Column("classification") DataClassification classification,
        @Column("occurred_at") Instant occurredAt) {
}
