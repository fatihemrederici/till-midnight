// Bu dosyanın ait olduğu paket.
package com.tensec.api.stats;

// İki değer eşit değilse testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertEquals;
// Değer null değilse testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertNull;

// Motorun sonuç tipi.
import com.tensec.api.engine.SimulationResult;
// Tarih tipi.
import java.time.LocalDate;
// Liste tipi.
import java.util.List;
// Benzersiz kimlik tipi.
import java.util.UUID;
// Her testten önce çalışan metodu işaretler.
import org.junit.jupiter.api.BeforeEach;
// Test etiketi.
import org.junit.jupiter.api.Test;
// Spring'in nesne verme etiketi.
import org.springframework.beans.factory.annotation.Autowired;
// Uygulamayı test içinde başlatan etiket.
import org.springframework.boot.test.context.SpringBootTest;
// SQL çalıştırma aracı.
import org.springframework.jdbc.core.simple.JdbcClient;

// Uygulamayı başlat.
@SpringBootTest
// Gerçek PostgreSQL ile SQL testleri.
class AttemptRepositoryTest {

    // Testlerin kullandığı tarih.
    private static final LocalDate DAY = LocalDate.of(2026, 12, 3);

    // Test edilen depo.
    @Autowired
    private AttemptRepository repository;

    // Tabloyu temizlemek için.
    @Autowired
    private JdbcClient jdbc;

    // Her test temiz tabloyla başlasın.
    @BeforeEach
    void cleanTable() {
        // Tüm satırları sil.
        jdbc.sql("TRUNCATE attempts").update();
    }

    // Bu metot bir test.
    @Test
    // Hiç deneme yoksa sayılar sıfır olmalı.
    void reportsEmptyStats() {
        // İstatistiği al.
        SceneStats stats = repository.stats("cafe-candle", DAY);

        // Deneme yok.
        assertEquals(0, stats.attempts());
        // Ortanca hesaplanamaz.
        assertNull(stats.medianTriesToSolve());
        // Kayıp yok.
        assertEquals(List.of(), stats.losses());
    }

    // Bu metot bir test.
    @Test
    // Sayılar, ortanca ve kayıp dağılımı doğru hesaplanmalı.
    void calculatesStats() {
        // Birinci oyuncu.
        UUID anna = UUID.randomUUID();
        // İkinci oyuncu.
        UUID ben = UUID.randomUUID();
        // Üçüncü oyuncu.
        UUID cem = UUID.randomUUID();

        // Anna 1. denemede kazanır.
        save(anna, win());
        // Ben 3. denemede kazanır. Kazandıktan sonraki deneme sayılmaz.
        save(ben, loss(8));
        save(ben, loss(9));
        save(ben, win());
        save(ben, loss(8));
        // Cem hiç kazanamaz.
        save(cem, loss(8));

        // İstatistiği al.
        SceneStats stats = repository.stats("cafe-candle", DAY);

        // 6 deneme.
        assertEquals(6, stats.attempts());
        // 3 oyuncu.
        assertEquals(3, stats.players());
        // 2 kazanan.
        assertEquals(2, stats.solvers());
        // Ortanca: 1 ile 3'ün ortası.
        assertEquals(2.0, stats.medianTriesToSolve());
        // 8. saniyede 3, 9. saniyede 1 kayıp. Çok olan önce gelir.
        assertEquals(
                List.of(new SceneStats.LossSecond(8, 3), new SceneStats.LossSecond(9, 1)),
                stats.losses());
    }

    // Bu metot bir test.
    @Test
    // Aynı attemptId tekrar gelirse yeni satır açılmamalı. Döngü ortasındaki dokunuş böyle gelir.
    void updatesAnAttemptInsteadOfDuplicatingIt() {
        // Oyuncu.
        UUID player = UUID.randomUUID();
        // Deneme.
        UUID attempt = UUID.randomUUID();

        // Döngü başında kayıp olarak kaydedilir.
        repository.save(attempt, player, "cafe-candle", DAY, 0, loss(8));
        // Dokunuştan sonra kazanca döner.
        repository.save(attempt, player, "cafe-candle", DAY, 1, win());

        // İstatistiği al.
        SceneStats stats = repository.stats("cafe-candle", DAY);

        // Tek deneme kalmalı.
        assertEquals(1, stats.attempts());
        // Son hali geçerli olmalı.
        assertEquals(1, stats.solvers());
    }

    // Bu metot bir test.
    @Test
    // Başka oyuncu, bilinen bir attemptId ile sonucu değiştirememeli.
    void doesNotLetAnotherPlayerOverwriteAnAttempt() {
        // Deneme.
        UUID attempt = UUID.randomUUID();

        // Sahibi kaydeder.
        repository.save(attempt, UUID.randomUUID(), "cafe-candle", DAY, 0, loss(8));
        // Başkası üzerine yazmaya çalışır.
        repository.save(attempt, UUID.randomUUID(), "cafe-candle", DAY, 1, win());

        // Sonuç değişmemiş olmalı.
        assertEquals(0, repository.stats("cafe-candle", DAY).solvers());
    }

    // Bu metot bir test.
    @Test
    // Başka gün ve sahnenin denemeleri karışmamalı.
    void keepsScenesAndDaysApart() {
        // Oyuncu.
        UUID player = UUID.randomUUID();

        // Başka gün.
        repository.save(UUID.randomUUID(), player, "cafe-candle", DAY.plusDays(1), 0, loss(8));
        // Başka sahne.
        repository.save(UUID.randomUUID(), player, "tram-bend", DAY, 0, loss(8));

        // Bu sahne ve günde deneme olmamalı.
        assertEquals(0, repository.stats("cafe-candle", DAY).attempts());
    }

    // Yardımcı metot: yeni bir deneme kaydeder.
    private void save(UUID player, SimulationResult result) {
        // Rastgele kimlikle kaydet.
        repository.save(UUID.randomUUID(), player, "cafe-candle", DAY, 1, result);
    }

    // Yardımcı metot: kazanılmış sonuç.
    private SimulationResult win() {
        // Felaket yok.
        return new SimulationResult(true, 10, null, List.of());
    }

    // Yardımcı metot: kaybedilmiş sonuç.
    private SimulationResult loss(int second) {
        // Felaketle bitti.
        return new SimulationResult(false, second, "explosion", List.of());
    }
}