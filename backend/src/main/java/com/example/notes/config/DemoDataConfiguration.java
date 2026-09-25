package com.example.notes.config;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.example.notes.note.Note;
import com.example.notes.note.NoteRepository;
import com.example.notes.note.NoteStatus;
import com.example.notes.security.DataClassification;

import reactor.core.publisher.Flux;

@Component
@Profile("demo")
public class DemoDataConfiguration implements ApplicationRunner {
    private final NoteRepository repository;
    private final Clock clock;

    public DemoDataConfiguration(NoteRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        repository.count()
                .filter(count -> count == 0)
                .flatMapMany(ignored -> Flux.fromIterable(seedNotes()))
                .concatMap(repository::save)
                .then()
                .block();
    }

    private List<Note> seedNotes() {
        Instant now = clock.instant();
        return List.of(
                note(
                        "Public release checklist",
                        "Coordinate the approved public release timeline.",
                        DataClassification.PUBLIC,
                        now.minusSeconds(300)),
                note(
                        "Internal integration plan",
                        "Track service owners, interfaces, and delivery risks.",
                        DataClassification.INTERNAL,
                        now.minusSeconds(180)),
                note(
                        "Controlled sensor assessment",
                        "Synthetic CUI demonstration record visible only to CUI-cleared operators.",
                        DataClassification.CUI,
                        now.minusSeconds(60)));
    }

    private Note note(
            String title,
            String content,
            DataClassification classification,
            Instant timestamp) {
        return new Note(
                null,
                "orbital-ops",
                "operator.ada",
                title,
                content,
                NoteStatus.ACTIVE,
                classification,
                timestamp,
                timestamp,
                null);
    }
}
