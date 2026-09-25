package com.example.notes.bdd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.notes.note.api.NoteResponse;

import io.cucumber.spring.ScenarioScope;

/**
 * Mutable state shared between step definition methods within a single Cucumber scenario.
 * Scoped with {@link ScenarioScope} so each scenario gets a fresh instance even though the
 * underlying Spring application context is cached and reused across scenarios for speed.
 */
@Component
@ScenarioScope
public class ScenarioState {
    private String team = "alpha";
    private int lastStatus;
    private String lastErrorCode;
    private String lastErrorTitle;
    private List<NoteResponse> lastNoteList;
    private String lastReferencedTitle;

    private final Map<String, NoteResponse> originalByTitle = new HashMap<>();
    private final Map<String, NoteResponse> currentByTitle = new HashMap<>();

    public String team() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public int lastStatus() {
        return lastStatus;
    }

    public void setLastStatus(int lastStatus) {
        this.lastStatus = lastStatus;
    }

    public String lastErrorCode() {
        return lastErrorCode;
    }

    public void setLastErrorCode(String lastErrorCode) {
        this.lastErrorCode = lastErrorCode;
    }

    public String lastErrorTitle() {
        return lastErrorTitle;
    }

    public void setLastErrorTitle(String lastErrorTitle) {
        this.lastErrorTitle = lastErrorTitle;
    }

    public List<NoteResponse> lastNoteList() {
        return lastNoteList;
    }

    public void setLastNoteList(List<NoteResponse> lastNoteList) {
        this.lastNoteList = lastNoteList;
    }

    /** Remembers a note the first time it's seen (creation), preserving its original version. */
    public void rememberCreated(String title, NoteResponse note) {
        originalByTitle.put(title, note);
        currentByTitle.put(title, note);
        lastReferencedTitle = title;
    }

    /** Updates the "current" snapshot of a note after a successful mutation, keeping the original. */
    public void rememberCurrent(String title, NoteResponse note) {
        currentByTitle.put(title, note);
        lastReferencedTitle = title;
    }

    public NoteResponse originalByTitle(String title) {
        return originalByTitle.get(title);
    }

    public NoteResponse currentByTitle(String title) {
        return currentByTitle.get(title);
    }

    public NoteResponse lastReferencedOriginal() {
        return originalByTitle.get(lastReferencedTitle);
    }

    public NoteResponse lastReferencedCurrent() {
        return currentByTitle.get(lastReferencedTitle);
    }

    public String lastReferencedTitle() {
        return lastReferencedTitle;
    }
}
