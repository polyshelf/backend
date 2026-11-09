package ru.polyshelf.core.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.polyshelf.core.models.FetchHistory;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FetchHistoryRepository {
    private static final String SELECT = "SELECT source, revision, next_row, updated_at FROM fetch_history WHERE source = ?";
    private static final RowMapper<FetchHistory> ROW_MAPPER = (rs, row) -> new FetchHistory(
            rs.getString("source"), rs.getString("revision"), rs.getLong("next_row"),
            rs.getObject("updated_at", OffsetDateTime.class));
    private final JdbcTemplate jdbc;

    public Optional<FetchHistory> find(String source) {
        return jdbc.query(SELECT, ROW_MAPPER, source).stream().findFirst();
    }

    public FetchHistory lock(String source) {
        return jdbc.queryForObject(SELECT + " FOR UPDATE", ROW_MAPPER, source);
    }

    public void insertIfAbsent(String source, String revision) {
        jdbc.update("INSERT INTO fetch_history (source, revision) VALUES (?, ?) ON CONFLICT (source) DO NOTHING",
                source, revision);
    }

    public FetchHistory update(String source, String revision, long nextRow) {
        return jdbc.queryForObject("""
                UPDATE fetch_history SET revision = ?, next_row = ?, updated_at = clock_timestamp()
                WHERE source = ?
                RETURNING source, revision, next_row, updated_at
                """, ROW_MAPPER, revision, nextRow, source);
    }
}
