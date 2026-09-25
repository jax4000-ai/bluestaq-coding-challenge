package com.example.notes.note.sorting;

import java.util.Comparator;

import org.springframework.stereotype.Component;

import com.example.notes.note.Note;

@Component
public class CreatedDescendingStrategy implements NoteSortStrategy {
    @Override
    public NoteSort supports() {
        return NoteSort.CREATED_DESC;
    }

    @Override
    public Comparator<Note> comparator() {
        return Comparator.comparing(Note::createdAt).reversed();
    }
}
