package com.example.notes;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.example.notes.audit.AuditRecordRepository;
import com.example.notes.note.NoteRepository;
import com.example.notes.note.NoteStatus;
import com.example.notes.note.api.CreateNoteRequest;
import com.example.notes.note.api.NoteResponse;
import com.example.notes.note.api.UpdateNoteRequest;
import com.example.notes.security.DataClassification;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class NoteApiIntegrationTest {
    private static final String TEAM_ALPHA = "/api/teams/alpha/notes";

    @Autowired
    private WebTestClient client;

    @Autowired
    private NoteRepository repository;

    @Autowired
    private AuditRecordRepository auditRepository;

    @BeforeEach
    void cleanDatabase() {
        auditRepository.deleteAll().block();
        repository.deleteAll().block();
        client = client.mutate()
                .defaultHeader("X-User-Id", "ada")
                .defaultHeader("X-User-Clearance", "CUI")
                .build();
    }

    @Test
    void createsListsUpdatesArchivesAndDeletesANote() {
        NoteResponse created = create("alpha", "ada", "Launch plan", "Confirm launch checklist");

        assertThat(created.id()).isNotNull();
        assertThat(created.status()).isEqualTo(NoteStatus.ACTIVE);
        assertThat(created.version()).isZero();

        client.get()
                .uri(TEAM_ALPHA + "?query=launch&sort=TITLE_ASC")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(NoteResponse.class)
                .value(notes -> assertThat(notes).extracting(NoteResponse::title).containsExactly("Launch plan"));

        NoteResponse updated = client.put()
                .uri(TEAM_ALPHA + "/" + created.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateNoteRequest("Launch plan", "Checklist confirmed", created.version()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(NoteResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(updated).isNotNull();
        assertThat(updated.content()).isEqualTo("Checklist confirmed");
        assertThat(updated.version()).isEqualTo(1);

        client.patch()
                .uri(TEAM_ALPHA + "/" + created.id() + "/archive")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ARCHIVED");

        client.get()
                .uri(TEAM_ALPHA + "?status=ACTIVE")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(NoteResponse.class)
                .hasSize(0);

        client.delete()
                .uri(TEAM_ALPHA + "/" + created.id())
                .exchange()
                .expectStatus().isNoContent();

        assertThat(auditRepository.findAllByTeamIdOrderByOccurredAtDesc("alpha")
                .collectList()
                .block())
                .extracting(record -> record.action())
                .containsExactly("NOTE_DELETED", "NOTE_ARCHIVED", "NOTE_UPDATED", "NOTE_CREATED");
    }

    @Test
    void isolatesNotesByTeam() {
        NoteResponse created = create("alpha", "ada", "Private plan", "Alpha-only content");

        client.get()
                .uri("/api/teams/beta/notes/" + created.id())
                .exchange()
                .expectStatus().isNotFound();

        client.get()
                .uri("/api/teams/beta/notes")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(NoteResponse.class)
                .hasSize(0);
    }

    @Test
    void rejectsDuplicateTitlesWithinTheSameTeam() {
        create("alpha", "ada", "Retrospective", "First version");

        client.post()
                .uri(TEAM_ALPHA)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateNoteRequest(
                        "retrospective", "Duplicate title", DataClassification.INTERNAL))
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Note rejected");

        create("beta", "grace", "Retrospective", "Allowed in another team");
    }

    @Test
    void rejectsAStaleUpdateWithConflict() {
        NoteResponse created = create("alpha", "ada", "Architecture", "Version one");

        client.put()
                .uri(TEAM_ALPHA + "/" + created.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateNoteRequest("Architecture", "Version two", created.version()))
                .exchange()
                .expectStatus().isOk();

        client.put()
                .uri(TEAM_ALPHA + "/" + created.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateNoteRequest("Architecture", "Stale edit", created.version()))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Edit conflict");
    }

    @Test
    void returnsStructuredErrorsForInvalidRequests() {
        client.post()
                .uri(TEAM_ALPHA)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateNoteRequest("", "", null))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Bad Request")
                .jsonPath("$.status").isEqualTo(400);
    }

    @Test
    void enforcesClassificationOnWritesAndReads() {
        NoteResponse cui = create(
                "alpha",
                "ada",
                "Controlled mission note",
                "CUI content",
                DataClassification.CUI);

        WebTestClient internalUser = client.mutate()
                .defaultHeaders(headers -> headers.set("X-User-Clearance", "INTERNAL"))
                .build();

        internalUser.get()
                .uri("/api/teams/alpha/notes/" + cui.id())
                .exchange()
                .expectStatus().isNotFound();

        internalUser.get()
                .uri("/api/teams/alpha/notes")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(NoteResponse.class)
                .hasSize(0);

        internalUser.post()
                .uri("/api/teams/alpha/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateNoteRequest("Denied", "CUI", DataClassification.CUI))
                .exchange()
                .expectStatus().isForbidden();
    }

    private NoteResponse create(String team, String author, String title, String content) {
        return create(team, author, title, content, DataClassification.INTERNAL);
    }

    private NoteResponse create(
            String team,
            String author,
            String title,
            String content,
            DataClassification classification) {
        return client.post()
                .uri("/api/teams/" + team + "/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.set("X-User-Id", author))
                .bodyValue(new CreateNoteRequest(title, content, classification))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(NoteResponse.class)
                .returnResult()
                .getResponseBody();
    }
}
