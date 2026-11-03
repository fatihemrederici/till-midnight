// Bu dosyanın ait olduğu paket.
package com.tensec.api.web;

// Metnin içermemesi gereken parçayı kontrol eder.
import static org.hamcrest.Matchers.not;
// Metnin içermesi gereken parçayı kontrol eder.
import static org.hamcrest.Matchers.containsString;
// Sahte GET isteği oluşturur.
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// Sahte POST isteği oluşturur.
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// Cevabın içeriğini kontrol eder.
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
// Cevaptaki JSON alanlarını kontrol eder.
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
// HTTP durum kodunu kontrol eder.
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sahne tipi.
import com.tensec.api.scene.Scene;
// Test etiketi.
import org.junit.jupiter.api.Test;
// Spring'in nesne verme etiketi.
import org.springframework.beans.factory.annotation.Autowired;
// MockMvc'yi otomatik ayarlayan etiket.
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
// Uygulamayı test içinde başlatan etiket.
import org.springframework.boot.test.context.SpringBootTest;
// İstek gövdesinin JSON olduğunu belirtmek için.
import org.springframework.http.MediaType;
// Gerçek sunucu açmadan HTTP isteği denemeye yarar.
import org.springframework.test.web.servlet.MockMvc;

// Uygulamayı başlat.
@SpringBootTest
// MockMvc'yi hazırla.
@AutoConfigureMockMvc
// API adreslerinin testleri.
class GameApiTest {

    // Spring, MockMvc nesnesini buraya verir.
    @Autowired
    private MockMvc mockMvc;

    // Spring, sahne kataloğunu da buraya verir.
    @Autowired
    private SceneCatalog catalog;

    // Bu metot bir test.
    @Test
    // Bugünün sahnesi nesneleri ve başlangıç emojilerini döndürmeli.
        void returnsTodaysScene() throws Exception {
        // Kataloğa göre bugünün sahnesi.
        Scene today = catalog.today();
        // Sahnenin ilk nesnesi.
        Scene.SceneObject first = today.objects().get(0);

        // İsteği gönder.
        mockMvc.perform(get("/scene/today"))
                // 200 OK gelmeli.
                .andExpect(status().isOk())
                // Sahne adı doğru olmalı.
                .andExpect(jsonPath("$.id").value(today.id()))
                // Nesne sayısı doğru olmalı.
                .andExpect(jsonPath("$.objects.length()").value(today.objects().size()))
                // İlk nesnenin başlangıç emojisi doğru olmalı.
                .andExpect(jsonPath("$.objects[0].emoji").value(first.states().get(first.initialState()).emoji()));
    }

    // Bu metot bir test.
    @Test
    // Sahne cevabı çözümü ele vermemeli. Hile yapılamamasının sebebi bu.
    void doesNotLeakTheRules() throws Exception {
        // İsteği gönder.
        mockMvc.perform(get("/scene/today"))
                // Tetikleyiciler görünmemeli.
                .andExpect(content().string(not(containsString("triggers"))))
                // Felaketler görünmemeli.
                .andExpect(content().string(not(containsString("disasters"))))
                // Dokunuş kuralları görünmemeli.
                .andExpect(content().string(not(containsString("\"touches\""))));
    }

    // Bu metot bir test.
    @Test
    // Dokunmazsan 8. saniyede patlamalı.
    void playsAnAttemptWithoutTouches() throws Exception {
        // Dokunuşsuz deneme gönder.
        attempt("""
                {"sceneId": "cafe-candle", "touches": []}
                """)
                // 200 OK gelmeli.
                .andExpect(status().isOk())
                // Kaybetmiş olmalı.
                .andExpect(jsonPath("$.won").value(false))
                // 8. saniyede bitmeli.
                .andExpect(jsonPath("$.endSecond").value(8))
                // 7 olay gelmeli.
                .andExpect(jsonPath("$.events.length()").value(7));
    }

    // Bu metot bir test.
    @Test
    // Doğru dokunuş kazandırmalı.
    void winsWithTheRightTouch() throws Exception {
        // 3. saniyede vanayı aç.
        attempt("""
                {"sceneId": "cafe-candle", "touches": [{"object": "machine", "second": 3}]}
                """)
                // Kazanmış olmalı.
                .andExpect(jsonPath("$.won").value(true))
                // Felaket cümlesi olmamalı.
                .andExpect(jsonPath("$.disasterCaption").isEmpty());
    }

    // Bu metot bir test.
    @Test
    // Geçersiz dokunuş 400 ve açıklama döndürmeli.
    void rejectsAnInvalidTouch() throws Exception {
        // Saniyesi aralık dışında dokunuş gönder.
        attempt("""
                {"sceneId": "cafe-candle", "touches": [{"object": "cat", "second": 99}]}
                """)
                // 400 gelmeli.
                .andExpect(status().isBadRequest())
                // Hata mesajı olmalı.
                .andExpect(jsonPath("$.error").value("Touch second out of range: 99"));
    }

    // Bu metot bir test.
    @Test
    // Olmayan sahne 404 döndürmeli.
    void returnsNotFoundForAnUnknownScene() throws Exception {
        // Olmayan sahne gönder.
        attempt("""
                {"sceneId": "nope", "touches": []}
                """)
                // 404 gelmeli.
                .andExpect(status().isNotFound());
    }

    // Yardımcı metot: POST /attempt isteği gönderir.
    private org.springframework.test.web.servlet.ResultActions attempt(String json) throws Exception {
        // Gövdesi JSON olan isteği gönder.
        return mockMvc.perform(post("/attempt").contentType(MediaType.APPLICATION_JSON).content(json));
    }
}