package com.example.notes.note.sorting;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class NoteSortStrategyRegistry {
    private final Map<NoteSort, NoteSortStrategy> strategies;

    public NoteSortStrategyRegistry(List<NoteSortStrategy> strategies) {
        EnumMap<NoteSort, NoteSortStrategy> byType = new EnumMap<>(NoteSort.class);
        strategies.forEach(strategy -> byType.put(strategy.supports(), strategy));
        this.strategies = Map.copyOf(byType);
    }

    public NoteSortStrategy get(NoteSort sort) {
        return strategies.get(sort);
    }
}
