// Bu dosyanın ait olduğu paket.
package com.tensec.api.stats;

// Motorun sonuç tipi.
import com.tensec.api.engine.SimulationResult;
// Tarih tipi.
import java.time.LocalDate;
// Liste tipi.
import java.util.List;
// Benzersiz kimlik tipi.
import java.util.UUID;
// Spring'in SQL çalıştırma aracı.
import org.springframework.jdbc.core.simple.JdbcClient;
// "Bu sınıf veritabanıyla konuşur" etiketi. @Component'in özel hali.
import org.springframework.stereotype.Repository;

// Denemelerle ilgili tüm SQL burada.
@Repository
public class AttemptRepository {

    // SQL çalıştıran araç.
    private final JdbcClient jdbc;

    // Spring JdbcClient'ı buraya kendisi verir.
    public AttemptRepository(JdbcClient jdbc) {
        // Aracı sakla.
        this.jdbc = jdbc;
    }

    // Denemeyi kaydeder. Aynı attemptId tekrar gelirse satırı günceller.
    // Tarayıcı döngü başında ve dokunuşta iki kez istek attığı için bu gerekli.
    public void save(
            UUID attemptId, UUID playerId, String sceneId, LocalDate date, int touchCount, SimulationResult result) {
        // Sorguyu hazırla. :isim parametredir; değerleri SQL'e elle eklemek SQL injection açığı yaratırdı.
        jdbc.sql("""
                INSERT INTO attempts
                    (attempt_id, player_id, scene_id, played_on, touch_count, won, end_second, disaster_id)
                VALUES
                    (:attemptId, :playerId, :sceneId, :date, :touchCount, :won, :endSecond, :disasterId)
                ON CONFLICT (attempt_id) DO UPDATE SET
                    touch_count = EXCLUDED.touch_count,
                    won = EXCLUDED.won,
                    end_second = EXCLUDED.end_second,
                    disaster_id = EXCLUDED.disaster_id
                WHERE attempts.player_id = EXCLUDED.player_id
                """)
                // Her parametrenin değerini ver.
                .param("attemptId", attemptId)
                .param("playerId", playerId)
                .param("sceneId", sceneId)
                .param("date", date)
                .param("touchCount", touchCount)
                .param("won", result.won())
                .param("endSecond", result.endSecond())
                .param("disasterId", result.disasterId())
                // Sorguyu çalıştır.
                .update();
    }

    // Bir sahnenin verilen gündeki istatistiklerini hesaplar.
    public SceneStats stats(String sceneId, LocalDate date) {
        // Üç sayıyı tek sorguda al. FILTER: sadece kazanılan satırları say.
        Totals totals = jdbc.sql("""
                SELECT COUNT(*) AS attempts,
                       COUNT(DISTINCT player_id) AS players,
                       COUNT(DISTINCT player_id) FILTER (WHERE won) AS solvers
                FROM attempts
                WHERE scene_id = :sceneId AND played_on = :date
                """)
                .param("sceneId", sceneId)
                .param("date", date)
                // Sonuç satırını Totals record'una çevir.
                .query(Totals.class)
                // Tek satır gelir.
                .single();

        // Ortancayı hesapla ve kayıpları al.
        return new SceneStats(
                totals.attempts(),
                totals.players(),
                totals.solvers(),
                medianTriesToSolve(sceneId, date),
                losses(sceneId, date));
    }

    // Kazananlar ilk zaferlerine kaçıncı denemede ulaştı? Ortancasını döndürür.
    private Double medianTriesToSolve(String sceneId, LocalDate date) {
        // WITH: sorguyu adımlara böler. ROW_NUMBER, her oyuncunun denemelerini sırayla numaralar.
        return jdbc.sql("""
                WITH numbered AS (
                    SELECT player_id,
                           won,
                           ROW_NUMBER() OVER (PARTITION BY player_id ORDER BY created_at, attempt_id) AS try_number
                    FROM attempts
                    WHERE scene_id = :sceneId AND played_on = :date
                ),
                first_wins AS (
                    SELECT player_id, MIN(try_number) AS tries
                    FROM numbered
                    WHERE won
                    GROUP BY player_id
                )
                SELECT PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY tries)
                FROM first_wins
                """)
                .param("sceneId", sceneId)
                .param("date", date)
                // Tek bir sayı döner. Kazanan yoksa null olur.
                .query(Double.class)
                .optional()
                .orElse(null);
    }

    // Hangi saniyede kaç kayıp olduğunu döndürür.
    private List<SceneStats.LossSecond> losses(String sceneId, LocalDate date) {
        // GROUP BY: aynı saniyedeki satırları tek satırda toplar.
        return jdbc.sql("""
                SELECT end_second AS second, COUNT(*) AS count
                FROM attempts
                WHERE scene_id = :sceneId AND played_on = :date AND NOT won
                GROUP BY end_second
                ORDER BY count DESC, second
                """)
                .param("sceneId", sceneId)
                .param("date", date)
                // Her satırı LossSecond record'una çevir.
                .query(SceneStats.LossSecond.class)
                // Hepsini listeye topla.
                .list();
    }

    // Toplam sorgusunun sonucu. Sadece bu sınıf kullanır.
    private record Totals(int attempts, int players, int solvers) {}
}
