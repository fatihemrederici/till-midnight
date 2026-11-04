// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.web;

// Listede bu eleman olmalı.
import static org.hamcrest.Matchers.hasItem;
// Sahte POST isteği oluşturur.
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// Cevaptaki JSON alanlarını kontrol eder.
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

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
// Hikâye denetiminin testleri. Sağlam hikâyeyi bozup yakalandığını kontrol eder.
class InternalApiTest {

    // Spring, MockMvc nesnesini buraya verir.
    @Autowired
    private MockMvc mockMvc;

    // Bu metot bir test.
    @Test
    // Sağlam hikâye geçmeli.
    void acceptsTheStory() throws Exception {
        // Hikâyeyi gönder.
        validate(json())
                // Geçerli.
                .andExpect(jsonPath("$.valid").value(true))
                // Analiz gelmeli.
                .andExpect(jsonPath("$.analysis.paths").value(485));
    }

    // Bu metot bir test.
    @Test
    // Bozuk JSON açıklama döndürmeli.
    void reportsBrokenJson() throws Exception {
        // Yarım JSON.
        validate("{ not json").andExpect(jsonPath("$.valid").value(false));
    }

    // Bu metot bir test.
    @Test
    // Eksik çeviri yakalanmalı.
    void reportsAMissingTranslation() throws Exception {
        // Başlığın Lehçesini sil.
        validate(json().replace("\"pl\": \"Do północy\"", "\"pl\": \"\""))
                // Hata mesajı gelmeli.
                .andExpect(jsonPath("$.errors", hasItem("title: missing language pl")));
    }

    // Bu metot bir test.
    @Test
    // Olmayan sona giden seçim yakalanmalı.
    void reportsAnUnknownEnding() throws Exception {
        // Ölüm sonunu boz.
        validate(json().replace("\"ending\": \"balcony\"", "\"ending\": \"ghost\""))
                // Hata mesajı gelmeli.
                .andExpect(jsonPath("$.errors", hasItem("Card locked left: unknown ending ghost")));
    }

    // Yardımcı metot: hikâye dosyasını okur.
    private String json() throws Exception {
        // UTF-8 olarak oku.
        return new ClassPathResource("stories/first-day.json").getContentAsString(StandardCharsets.UTF_8);
    }

    // Yardımcı metot: POST /internal/validate isteği gönderir.
    private ResultActions validate(String json) throws Exception {
        // Gövdesi JSON olan isteği gönder.
        return mockMvc.perform(post("/internal/validate").contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
