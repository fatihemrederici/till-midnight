// Bu dosyanın ait olduğu paket.
package com.tensec.api.web;

// Listede bu eleman olmalı.
import static org.hamcrest.Matchers.hasItem;
// Sahte POST isteği oluşturur.
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// Cevaptaki JSON alanlarını kontrol eder.
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
// HTTP durum kodunu kontrol eder.
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Dosyayı UTF-8 okumak için.
import java.nio.charset.StandardCharsets;
// Test etiketi.
import org.junit.jupiter.api.Test;
// Spring'in nesne verme etiketi.
import org.springframework.beans.factory.annotation.Autowired;
// Uygulamayı test içinde başlatan etiket.
import org.springframework.boot.test.context.SpringBootTest;
// MockMvc'yi otomatik ayarlayan etiket.
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
// Projeye paketlenmiş dosyayı okur.
import org.springframework.core.io.ClassPathResource;
// İstek gövdesinin JSON olduğunu belirtmek için.
import org.springframework.http.MediaType;
// Gerçek sunucu açmadan HTTP isteği denemeye yarar.
import org.springframework.test.web.servlet.MockMvc;
// İstek sonucunu kontrol etmeye yarar.
import org.springframework.test.web.servlet.ResultActions;

// Uygulamayı başlat.
@SpringBootTest
// MockMvc'yi hazırla.
@AutoConfigureMockMvc
// /internal/validate adresinin testleri.
class InternalApiTest {

    // Spring, MockMvc nesnesini buraya verir.
    @Autowired
    private MockMvc mockMvc;

    // Bu metot bir test.
    @Test
    // Sağlam kafe sahnesi geçmeli ve çözüm bilgileri gelmeli.
    void acceptsTheCafeScene() throws Exception {
        // Sahneyi değiştirmeden gönder.
        validate(cafeJson())
                // 200 OK gelmeli.
                .andExpect(status().isOk())
                // Geçerli olmalı.
                .andExpect(jsonPath("$.valid").value(true))
                // 1276 ihtimal denenmeli.
                .andExpect(jsonPath("$.combinationsTried").value(1276))
                // Tek dokunuşla çözülmeli.
                .andExpect(jsonPath("$.minTouches").value(1))
                // 46 çözüm bulunmalı.
                .andExpect(jsonPath("$.solutionCount").value(46))
                // Zorluk 93 olmalı.
                .andExpect(jsonPath("$.difficulty").value(93));
    }

    // Bu metot bir test.
    @Test
    // Bozuk JSON hata fırlatmak yerine açıklama döndürmeli.
    void reportsBrokenJson() throws Exception {
        // Yarım JSON gönder.
        validate("{ not json")
                // İstek yine işlenmeli.
                .andExpect(status().isOk())
                // Geçersiz olmalı.
                .andExpect(jsonPath("$.valid").value(false));
    }

    // Bu metot bir test.
    @Test
    // Yanlış yazılmış alan reddedilmeli.
    void reportsUnknownFields() throws Exception {
        // title alanını bozup gönder.
        validate(cafeJson().replace("\"title\"", "\"titel\""))
                // Geçersiz olmalı.
                .andExpect(jsonPath("$.valid").value(false));
    }

    // Bu metot bir test.
    @Test
    // "null" metni uygulamayı düşürmemeli.
    void reportsAnEmptyScene() throws Exception {
        // Sadece null gönder.
        validate("null")
                // Hata mesajı gelmeli.
                .andExpect(jsonPath("$.errors", hasItem("Scene is empty")));
    }

    // Bu metot bir test.
    @Test
    // Var olmayan başlangıç durumu yakalanmalı.
    void reportsAnUnknownInitialState() throws Exception {
        // Mumun başlangıç durumunu boz.
        validate(cafeJson().replace("\"initialState\": \"lit\"", "\"initialState\": \"flaming\""))
                // Hata mesajı gelmeli.
                .andExpect(jsonPath("$.errors", hasItem("Object candle: initial state flaming does not exist")));
    }

    // Bu metot bir test.
    @Test
    // Tetikleyicinin beklediği durum sahnede olmalı.
    void reportsAnUnknownStateInATrigger() throws Exception {
        // Perdenin beklediği durumu boz.
        validate(cafeJson().replace("\"state\": \"smoking\", \"delay\": 3", "\"state\": \"ghost\", \"delay\": 3"))
                // Hata mesajı gelmeli.
                .andExpect(jsonPath("$.errors", hasItem("Trigger curtain-ignites: object curtain has no state ghost")));
    }

    // Bu metot bir test.
    @Test
    // Izgara dışındaki nesne yakalanmalı.
    void reportsAnObjectOutsideTheGrid() throws Exception {
        // Makineyi ızgaranın dışına taşı.
        validate(cafeJson().replace("\"x\": 5, \"y\": 1", "\"x\": 9, \"y\": 1"))
                // Hata mesajı gelmeli.
                .andExpect(jsonPath("$.errors", hasItem("Object machine is outside the grid")));
    }

    // Bu metot bir test.
    @Test
    // Çok büyük maxTouches çözücüye ulaşmamalı, yoksa sunucu kilitlenirdi.
    void rejectsTooManyTouchesBeforeRunningTheSolver() throws Exception {
        // maxTouches değerini büyüt.
        validate(cafeJson().replace("\"maxTouches\": 2", "\"maxTouches\": 50"))
                // Hata mesajı gelmeli.
                .andExpect(jsonPath("$.errors", hasItem("maxTouches must be between 1 and 3")))
                // Çözücü çalışmamış olmalı.
                .andExpect(jsonPath("$.combinationsTried").isEmpty());
    }

    // Bu metot bir test.
    @Test
    // Çözümsüz sahne reddedilmeli.
    void reportsASceneWithNoSolution() throws Exception {
        // Perde en baştan "hanging" olduğu ve bu haline dokunulamadığı için felaket hep olur.
        String json = cafeJson().replace(
                "\"when\": [{ \"object\": \"curtain\", \"state\": \"burning\" }]",
                "\"when\": [{ \"object\": \"curtain\", \"state\": \"hanging\" }]");

        // Sahneyi gönder.
        validate(json)
                // Geçersiz olmalı.
                .andExpect(jsonPath("$.valid").value(false))
                // Hata mesajı gelmeli.
                .andExpect(jsonPath("$.errors", hasItem("Scene has no solution")));
    }

    // Bu metot bir test.
    @Test
    // Dokunmadan kazanılan sahne reddedilmeli.
    void reportsASceneThatNeedsNoTouch() throws Exception {
        // İki felaketi de kendiliğinden hiç oluşmayacak hale getir.
        String json = cafeJson()
                .replace(
                        "\"when\": [{ \"object\": \"machine\", \"state\": \"exploded\" }]",
                        "\"when\": [{ \"object\": \"machine\", \"state\": \"safe\" }]")
                .replace(
                        "\"when\": [{ \"object\": \"curtain\", \"state\": \"burning\" }]",
                        "\"when\": [{ \"object\": \"curtain\", \"state\": \"torn_down\" }]");

        // Sahneyi gönder.
        validate(json)
                // Hata mesajı gelmeli.
                .andExpect(jsonPath("$.errors", hasItem("Scene can be won without touching anything")));
    }

    // Yardımcı metot: kafe sahnesinin JSON metnini okur.
    private String cafeJson() throws Exception {
        // Dosyayı UTF-8 olarak oku.
        return new ClassPathResource("scenes/cafe-candle.json").getContentAsString(StandardCharsets.UTF_8);
    }

    // Yardımcı metot: POST /internal/validate isteği gönderir.
    private ResultActions validate(String json) throws Exception {
        // Gövdesi JSON olan isteği gönder.
        return mockMvc.perform(post("/internal/validate").contentType(MediaType.APPLICATION_JSON).content(json));
    }
}