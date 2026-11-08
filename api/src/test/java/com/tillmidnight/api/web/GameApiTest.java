// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.web;

// Sahte GET isteği oluşturur.
// İki değer eşit değilse testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// Sahte POST isteği oluşturur.
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// Cevaptaki JSON alanlarını kontrol eder.
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
// HTTP durum kodunu kontrol eder.
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Hikâye okuyucu.
import com.tillmidnight.api.story.StoryLoader;
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
// MockMvc'yi otomatik ayarlayan etiket.
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
// İstek gövdesinin JSON olduğunu belirtmek için.
import org.springframework.http.MediaType;
// SQL çalıştırma aracı.
import org.springframework.jdbc.core.simple.JdbcClient;
// Gerçek sunucu açmadan HTTP isteği denemeye yarar.
import org.springframework.test.web.servlet.MockMvc;
// İstek sonucunu kontrol etmeye yarar.
import org.springframework.test.web.servlet.ResultActions;

// Uygulamayı başlat.
@SpringBootTest
// MockMvc'yi hazırla.
@AutoConfigureMockMvc
// Oyun API'sinin testleri. Gerçek PostgreSQL kullanır.
class GameApiTest {

    // Merdivende ölen kısa yol: 6 kart.
    private static final String STAIRS = "\"RIGHT\",\"RIGHT\",\"LEFT\",\"LEFT\",\"RIGHT\",\"LEFT\",\"LEFT\"";

    // Spring, MockMvc nesnesini buraya verir.
    @Autowired
    private MockMvc mockMvc;

    // Tabloyu temizlemek için.
    @Autowired
    private JdbcClient jdbc;

    // Her test temiz tabloyla başlasın.
    @BeforeEach
    void cleanTable() {
        // Tüm satırları sil.
        jdbc.sql("TRUNCATE runs").update();
    }

    // Bu metot bir test.
    @Test
    // Hikâye üç dilde gelmeli.
    void returnsAChapterInThreeLanguages() throws Exception {
        // İsteği gönder.
        mockMvc.perform(get("/stories/first-day"))
                // 200 OK.
                .andExpect(status().isOk())
                // İngilizce başlık.
                .andExpect(jsonPath("$.title.en").value("The First Day"))
                // Türkçe başlık.
                .andExpect(jsonPath("$.title.tr").value("İlk Gün"))
                // Lehçe başlık.
                .andExpect(jsonPath("$.title.pl").value("Pierwszy dzień"));
    }

    // Bu metot bir test.
    @Test
    // Bölüm listesi sırayla gelmeli, kurallar gitmemeli.
    void listsChaptersInOrder() throws Exception {
        // İsteği gönder.
        mockMvc.perform(get("/stories"))
                // 200 OK.
                .andExpect(status().isOk())
                // İlk bölüm.
                .andExpect(jsonPath("$[0].id").value("first-day"))
                // Ölümler listelenir, kurtuluşlar değil.
                .andExpect(jsonPath("$[0].deaths.length()").value(19))
                // Kurallar bu listede yok.
                .andExpect(jsonPath("$[0].cards").doesNotExist());
    }

    // Bu metot bir test.
    @Test
    // Olmayan bölüm 404 dönmeli.
    void returnsNotFoundForAnUnknownChapter() throws Exception {
        // İsteği gönder.
        mockMvc.perform(get("/stories/nope")).andExpect(status().isNotFound());
    }

    // Bu metot bir test.
    @Test
    // Sunucu sonucu seçimlerden hesaplamalı ve kaydetmeli.
    void calculatesAndSavesARun() throws Exception {
        // Koşuyu gönder.
        run(UUID.randomUUID(), UUID.randomUUID(), STAIRS)
                // 200 OK.
                .andExpect(status().isOk())
                // Son doğru.
                .andExpect(jsonPath("$.endingId").value("stairs"))
                // Kaybedildi.
                .andExpect(jsonPath("$.won").value(false))
                // Bir koşu kaydedildi.
                .andExpect(jsonPath("$.stats.runs").value(1))
                // Bu sona bir kişi ulaştı.
                .andExpect(jsonPath("$.stats.sameEnding").value(1));
    }

    // Bu metot bir test.
    @Test
    // Aynı koşu tekrar gelirse iki kez sayılmamalı.
    void countsARetriedRunOnce() throws Exception {
        // Koşu kimliği.
        UUID runId = UUID.randomUUID();
        // Oyuncu.
        UUID player = UUID.randomUUID();

        // İki kez gönder.
        run(runId, player, STAIRS);
        // İkincide hâlâ tek koşu görünmeli.
        run(runId, player, STAIRS).andExpect(jsonPath("$.stats.runs").value(1));
    }

    // Bu metot bir test.
    @Test
    // İstatistikler oyuncuları ve sonları ayırmalı.
    void separatesPlayersAndEndings() throws Exception {
        // Aynı oyuncudan iki koşu.
        UUID player = UUID.randomUUID();
        run(UUID.randomUUID(), player, STAIRS);
        run(UUID.randomUUID(), player, STAIRS);
        // Başka oyuncudan başka bir son: kamyon.
        run(UUID.randomUUID(), UUID.randomUUID(), "\"RIGHT\",\"LEFT\",\"RIGHT\"")
                .andExpect(jsonPath("$.endingId").value("horn"))
                // Üç koşu.
                .andExpect(jsonPath("$.stats.runs").value(3))
                // İki oyuncu.
                .andExpect(jsonPath("$.stats.players").value(2))
                // Bu sona bir koşu.
                .andExpect(jsonPath("$.stats.sameEnding").value(1));
    }

    // Bu metot bir test.
    @Test
    // Yarım koşu ve olmayan hikâye reddedilmeli.
    void rejectsInvalidRequests() throws Exception {
        // Yarım koşu 400.
        run(UUID.randomUUID(), UUID.randomUUID(), "\"LEFT\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Run is not finished"));
        // Olmayan hikâye 404.
        mockMvc.perform(post("/runs").contentType(MediaType.APPLICATION_JSON).content("{\"storyId\":\"nope\"}"))
                .andExpect(status().isNotFound());
    }

    // Bu metot bir test.
    @Test
    // Eski sürümle oynanan koşu yeni kurallarla oynatılmamalı.
    void rejectsARunFromAnOldVersion() throws Exception {
        // Yanlış sürümle gönder.
        send(UUID.randomUUID(), UUID.randomUUID(), STAIRS, "old-version").andExpect(status().isConflict());
        // Kayıt oluşmamalı.
        assertEquals(0, jdbc.sql("SELECT COUNT(*) FROM runs").query(Integer.class).single());
    }

    // Bu metot bir test.
    @Test
    // Koşu, oynandığı sürümle kaydedilmeli.
    void savesTheStoryVersion() throws Exception {
        // Koşuyu gönder.
        run(UUID.randomUUID(), UUID.randomUUID(), STAIRS).andExpect(status().isOk());
        // Sürüm yazılmış olmalı.
        assertEquals(version(), jdbc.sql("SELECT story_version FROM runs").query(String.class).single());
    }

    // Yardımcı metot: Hikâyenin güncel sürümüyle POST /runs gönderir.
    private ResultActions run(UUID runId, UUID player, String choices) throws Exception {
        // Güncel sürümü kullan.
        return send(runId, player, choices, version());
    }

    // Yardımcı metot: Hikâyenin güncel sürümü.
    private String version() {
        // Klasörden okuyup hesapla.
        return StoryLoader.loadChapter("first-day").version();
    }

    // Yardımcı metot: POST /runs isteği gönderir.
    private ResultActions send(UUID runId, UUID player, String choices, String version) throws Exception {
        // Gövdesi JSON olan isteği gönder.
        return mockMvc.perform(post("/runs").contentType(MediaType.APPLICATION_JSON).content("""
                {"storyId": "first-day", "storyVersion": "%s", "runId": "%s", "playerId": "%s", "choices": [%s]}
                """.formatted(version, runId, player, choices)));
    }

    // Kartın fotoğrafı JPEG olarak gelir; başka klasöre çıkmaya çalışan istek bulunamaz.
    @Test
    void servesCardImagesSafely() throws Exception {
        mockMvc.perform(get("/stories/first-day/images/bus.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));
        mockMvc.perform(get("/stories/first-day/images/..%2Fstory.json")).andExpect(status().isNotFound());
        mockMvc.perform(get("/stories/first-day/images/ghost.jpg")).andExpect(status().isNotFound());
    }
}
