// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.story;

// İki değer eşit değilse testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertEquals;
// Beklenen hata fırlatılmazsa testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertThrows;
// Değer true değilse testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertTrue;

// Sırasını koruyan anahtar-değer.
import java.util.LinkedHashMap;
// Anahtar-değer arayüzü.
import java.util.Map;
// Test etiketi.
import org.junit.jupiter.api.Test;
// JSON ağacı.
import tools.jackson.databind.JsonNode;
// JSON okuyucu.
import tools.jackson.databind.json.JsonMapper;

// Kurallar ve dil dosyalarının birleşmesi.
class StoryMergerTest {

    // JSON okuyucu.
    private static final JsonMapper JSON = JsonMapper.builder().build();
    // En küçük kural dosyası.
    private static final String RULES = "{\"id\":\"mini\",\"meters\":[{\"id\":\"mind\"}]}";

    // Bu metot bir test.
    @Test
    // Aynı yerdeki metinler dillerine göre birleşmeli.
    void combinesLanguagesIntoOneText() {
        // İki dil.
        JsonNode story = StoryMerger.merge(JSON.readTree(RULES), texts(
                "{\"title\":\"Hello\",\"meters\":{\"mind\":\"Mind\"}}",
                "{\"title\":\"Merhaba\",\"meters\":{\"mind\":\"Akıl\"}}"));

        // Başlık iki dilde.
        assertEquals("Merhaba", story.at("/title/tr").asString());
        // Gösterge etiketi kuraldaki sırayla eşleşir.
        assertEquals("Akıl", story.at("/meters/0/label/tr").asString());
    }

    // Bu metot bir test.
    @Test
    // Seçimin duruma göre değişen sonuç metni, kuraldaki sırasıyla eşleşmeli.
    void combinesChoiceVariants() {
        // Tek kartlı, sağ seçiminde bir varyant olan bölüm.
        String rules = "{\"id\":\"mini\",\"cards\":[{\"id\":\"road\",\"right\":{\"variants\":[{\"when\":{\"elif\":true}}]}}]}";
        JsonNode story = StoryMerger.merge(JSON.readTree(rules), texts(
                "{\"cards\":{\"road\":{\"right\":{\"variants\":[\"Elif pulls you back.\"]}}}}",
                "{\"cards\":{\"road\":{\"right\":{\"variants\":[\"Elif seni çekiyor.\"]}}}}"));

        // Varyantın metni iki dilde.
        assertEquals("Elif seni çekiyor.", story.at("/cards/0/right/variants/0/text/tr").asString());
    }

    // Bu metot bir test.
    @Test
    // Eksik dil burada hata vermez, doğrulayıcının listesine düşer.
    void leavesMissingTextsForTheValidator() {
        // Türkçe başlık yok.
        JsonNode story = StoryMerger.merge(JSON.readTree(RULES), texts("{\"title\":\"Hello\"}", "{}"));

        // Sadece İngilizce gelmeli.
        assertTrue(story.at("/title/tr").isMissingNode());
    }

    // Bu metot bir test.
    @Test
    // Yanlış yazılmış kimliğin metni sessizce kaybolmamalı.
    void rejectsTextsThatNothingUses() {
        // Olmayan bir gösterge.
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> StoryMerger.merge(
                JSON.readTree(RULES), texts("{\"meters\":{\"ghost\":\"Ghost\"}}", "{}")));

        // Mesaj dili ve yeri söylemeli.
        assertTrue(error.getMessage().contains("en/meters/ghost"));
    }

    // Yardımcı metot: İngilizce ve Türkçe metin ağaçları.
    private Map<String, JsonNode> texts(String english, String turkish) {
        // Sıralı harita.
        Map<String, JsonNode> texts = new LinkedHashMap<>();
        // İngilizce.
        texts.put("en", JSON.readTree(english));
        // Türkçe.
        texts.put("tr", JSON.readTree(turkish));
        // Haritayı döndür.
        return texts;
    }
}
