// Bu dosyanın ait olduğu paket.
package com.tensec.api.run;

// Spring'in SQL çalıştırma aracı.
import org.springframework.jdbc.core.simple.JdbcClient;
// "Bu sınıf veritabanıyla konuşur" etiketi.
import org.springframework.stereotype.Repository;

// Koşularla ilgili tüm SQL burada.
@Repository
public class RunRepository {

    // SQL çalıştıran araç.
    private final JdbcClient jdbc;

    // Spring JdbcClient'ı buraya kendisi verir.
    public RunRepository(JdbcClient jdbc) {
        // Aracı sakla.
        this.jdbc = jdbc;
    }

    // Koşuyu kaydeder. Aynı runId ikinci kez gelirse hiçbir şey yapmaz; bir koşu değiştirilemez.
    public void save(SavedRun run) {
        // :isim parametredir; değerleri SQL'e elle eklemek SQL injection açığı yaratırdı.
        jdbc.sql("""
                INSERT INTO runs (run_id, player_id, story_id, ending_id, won, cards_played)
                VALUES (:runId, :playerId, :storyId, :endingId, :won, :cardsPlayed)
                ON CONFLICT (run_id) DO NOTHING
                """)
                .param("runId", run.runId())
                .param("playerId", run.playerId())
                .param("storyId", run.storyId())
                .param("endingId", run.endingId())
                .param("won", run.won())
                .param("cardsPlayed", run.cardsPlayed())
                .update();
    }

    // Bir hikâyenin istatistiklerini ve bir sona ulaşma sayısını hesaplar.
    public StoryStats stats(String storyId, String endingId) {
        // FILTER: sadece koşulu tutan satırları say.
        return jdbc.sql("""
                SELECT COUNT(*) AS runs,
                       COUNT(DISTINCT player_id) AS players,
                       COUNT(*) FILTER (WHERE won) AS survived,
                       COUNT(*) FILTER (WHERE ending_id = :endingId) AS same_ending
                FROM runs
                WHERE story_id = :storyId
                """)
                .param("storyId", storyId)
                .param("endingId", endingId)
                // Satırı StoryStats record'una çevir.
                .query((row, index) -> new StoryStats(
                        row.getInt("runs"),
                        row.getInt("players"),
                        row.getInt("survived"),
                        row.getInt("same_ending")))
                // Tek satır gelir.
                .single();
    }
}
