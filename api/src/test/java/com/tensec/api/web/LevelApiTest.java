// Bu dosyanın ait olduğu paket.
package com.tensec.api.web;

// Sahte GET isteği oluşturur.
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// Sahte POST isteği oluşturur.
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// Cevaptaki JSON alanlarını kontrol eder.
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
// HTTP durum kodunu kontrol eder.
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Benzersiz kimlik tipi.
import java.util.UUID;
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
// Gerçek sunucu açmadan HTTP isteği denemeye yarar.
import org.springframework.test.web.servlet.MockMvc;

// Uygulamayı başlat.
@SpringBootTest
// MockMvc'yi hazırla.
@AutoConfigureMockMvc
// Bölüm sisteminin testleri.
class LevelApiTest {

    // Spring, MockMvc nesnesini buraya verir.
    @Autowired
    private MockMvc mockMvc;

    // Bu metot bir test.
    @Test
    // Yeni oyuncuda sadece ilk bölüm açık olmalı.
    void onlyTheFirstLevelIsOpenForANewPlayer() throws Exception {
        // Yeni oyuncuyla iste.
        mockMvc.perform(get("/levels").param("playerId", UUID.randomUUID().toString()))
                // 3 bölüm gelmeli.
                .andExpect(jsonPath("$.length()").value(3))
                // İlk bölüm kafe ve açık.
                .andExpect(jsonPath("$[0].sceneId").value("cafe-candle"))
                .andExpect(jsonPath("$[0].unlocked").value(true))
                // İkincisi kilitli.
                .andExpect(jsonPath("$[1].unlocked").value(false));
    }

    // Bu metot bir test.
    @Test
    // Kimlik gönderilmezse hata değil, yeni oyuncu görünümü gelmeli.
    void worksWithoutAPlayerId() throws Exception {
        // Kimliksiz iste.
        mockMvc.perform(get("/levels"))
                // 200 OK gelmeli.
                .andExpect(status().isOk())
                // İlk bölüm açık olmalı.
                .andExpect(jsonPath("$[0].unlocked").value(true));
    }

    // Bu metot bir test.
    @Test
    // Bölümü en az dokunuşla geçince yıldız alınmalı ve sıradaki açılmalı.
    void theBestSolutionEarnsAStarAndOpensTheNextLevel() throws Exception {
        // Oyuncu.
        UUID player = UUID.randomUUID();

        // 3. saniyede vana: tek dokunuşla en iyi çözüm.
        play(player, """
                [{"object": "machine", "second": 3}]
                """);

        // Listeyi iste.
        mockMvc.perform(get("/levels").param("playerId", player.toString()))
                // Kafe geçildi.
                .andExpect(jsonPath("$[0].completed").value(true))
                // Yıldız var.
                .andExpect(jsonPath("$[0].star").value(true))
                // Sıradaki açıldı.
                .andExpect(jsonPath("$[1].unlocked").value(true))
                // Ondan sonraki hâlâ kilitli.
                .andExpect(jsonPath("$[2].unlocked").value(false));
    }

    // Bu metot bir test.
    @Test
    // Fazla dokunuşla geçince bölüm açılır ama yıldız gelmez.
    void aLongerSolutionEarnsNoStar() throws Exception {
        // Oyuncu.
        UUID player = UUID.randomUUID();

        // İki dokunuşlu çözüm.
        play(player, """
                [{"object": "candle", "second": 1}, {"object": "machine", "second": 5}]
                """);

        // Listeyi iste.
        mockMvc.perform(get("/levels").param("playerId", player.toString()))
                // Geçildi.
                .andExpect(jsonPath("$[0].completed").value(true))
                // Yıldız yok.
                .andExpect(jsonPath("$[0].star").value(false));
    }

    // Bu metot bir test.
    @Test
    // Kaybedilen deneme bölümü geçirmemeli.
    void aLossDoesNotCompleteALevel() throws Exception {
        // Oyuncu.
        UUID player = UUID.randomUUID();

        // Dokunuşsuz deneme kaybeder.
        play(player, "[]");

        // Listeyi iste.
        mockMvc.perform(get("/levels").param("playerId", player.toString()))
                // Geçilmedi.
                .andExpect(jsonPath("$[0].completed").value(false));
    }

    // Bu metot bir test.
    @Test
    // Bölüm sahnesi adıyla açılmalı, olmayan sahne 404 dönmeli.
    void opensASceneById() throws Exception {
        // Var olan sahne.
        mockMvc.perform(get("/scene/tram-bend"))
                .andExpect(jsonPath("$.title").value("The Late Tram"));
        // Olmayan sahne.
        mockMvc.perform(get("/scene/nope")).andExpect(status().isNotFound());
        // Bir bölümün istatistiği.
        mockMvc.perform(get("/stats/tram-bend")).andExpect(jsonPath("$.attempts").isNumber());
    }

    // Yardımcı metot: kafe sahnesinde kayıtlı bir deneme oynar.
    private void play(UUID player, String touches) throws Exception {
        // İsteği gönder.
        mockMvc.perform(post("/attempt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sceneId": "cafe-candle", "touches": %s,
                                 "playerId": "%s", "attemptId": "%s"}
                                """.formatted(touches, player, UUID.randomUUID())))
                .andExpect(status().isOk());
    }
}
