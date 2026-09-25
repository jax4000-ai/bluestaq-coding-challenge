package com.example.notes.note.validation;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.notes.note.service.NoteDraft;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class NoteRuleChain {
    private final List<NoteRule> rules;

    public NoteRuleChain(List<NoteRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public Mono<Void> validate(NoteDraft draft) {
        return Flux.fromIterable(rules)
                .concatMap(rule -> rule.validate(draft))
                .then();
    }
}
