package com.example.notes.note;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.example.notes.security.DataClassification;

@Table("notes")
public record Note(
        @Id @Column("id") Long id,
        @Column("team_id") String teamId,
        @Column("author_id") String authorId,
        @Column("title") String title,
        @Column("content") String content,
        @Column("status") NoteStatus status,
        @Column("classification") DataClassification classification,
        @Column("created_at") Instant createdAt,
        @Column("updated_at") Instant updatedAt,
        @Version @Column("version") Long version) {

    public Note update(String newTitle, String newContent, Instant now) {
        return new Note(
                id, teamId, authorId, newTitle, newContent, status, classification, createdAt, now, version);
    }

    public Note withStatus(NoteStatus newStatus, Instant now) {
        return new Note(
                id, teamId, authorId, title, content, newStatus, classification, createdAt, now, version);
    }
}
