package com.example.notes.note.sorting;

import java.util.Comparator;

import com.example.notes.note.Note;

public interface NoteSortStrategy {
    NoteSort supports();

    Comparator<Note> comparator();
}
