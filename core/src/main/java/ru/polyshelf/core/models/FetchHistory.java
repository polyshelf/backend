package ru.polyshelf.core.models;

import java.time.OffsetDateTime;

/** A checkpoint refers to the next raw row, including rows excluded by validation. */
public record FetchHistory(String source, String revision, long nextRow, OffsetDateTime updatedAt) {
}
