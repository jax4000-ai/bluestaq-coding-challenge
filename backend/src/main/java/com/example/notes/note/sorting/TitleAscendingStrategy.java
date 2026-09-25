package com.example.notes.note.sorting;

import java.util.Comparator;

import org.springframework.stereotype.Component;

import com.example.notes.note.Note;

@Component
public class TitleAscendingStrategy implements NoteSortStrategy {
    @Override
    public NoteSort supports() {
        return NoteSort.TITLE_ASC;
    }

    @Override
    public Comparator<Note> comparator() {
        return Comparator.comparing(Note::title, String.CASE_INSENSITIVE_ORDER);
    }
}
