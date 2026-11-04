// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.story;

// Dosya okurken çıkabilecek hata tipi.
import java.io.IOException;
// Dosyayı bayt bayt okumaya yarayan tip.
import java.io.InputStream;
// IOException'ı metot imzasına yazmak zorunda kalmamak için kullandığımız hata tipi.
import java.io.UncheckedIOException;
// Sırasını koruyan anahtar-değer.
import java.util.LinkedHashMap;
// Liste arayüzü.
import java.util.List;
// Anahtar-değer arayüzü.
import java.util.Map;
// Jackson ayarı: JSON'dan okuma özellikleri.
import tools.jackson.databind.DeserializationFeature;
// JSON ağacı.
import tools.jackson.databind.JsonNode;
// Jackson'ın JSON çeviricisi. Swift'teki JSONDecoder gibi.
import tools.jackson.databind.json.JsonMapper;

// Bölüm klasörlerini okuyup Story nesnesine çeviren sınıf.
// Klasör düzeni için docs/story-format.md dosyasına bak.
public final class StoryLoader {

    // Bilinmeyen alan varsa hata ver: AI ya da biz yanlış yazarsak sessizce geçmesin.
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    // Hikâye dosyalarının projeye paketlendiği klasör.
    private static final String ROOT = "stories/";

    // Bu sınıftan nesne üretilemez, çünkü tüm metotlar static.
    private StoryLoader() {}

    // chapters.json dosyasındaki bölüm kimlikleri, oynanacak sırayla.
    public static List<String> chapterIds() {
        // Dosyayı oku ve kimlikleri topla.
        return readTree(ROOT + "chapters.json").path("chapters").valueStream().map(JsonNode::asString).toList();
    }

    // Bir bölümün kurallarını ve dillerini okuyup tek hikâyede birleştirir.
    public static Story loadChapter(String id) {
        // Bölümün klasörü.
        String folder = ROOT + id + "/";
        // Dil -> metin ağacı.
        Map<String, JsonNode> texts = new LinkedHashMap<>();
        // Her dilin dosyasını oku.
        for (String language : StoryValidator.LANGUAGES) {
            texts.put(language, readTree(folder + language + ".json"));
        }
        // Kurallarla birleştir ve nesneye çevir.
        return MAPPER.treeToValue(StoryMerger.merge(readTree(folder + "story.json"), texts), Story.class);
    }

    // Birleşik hikâye JSON'unu Story nesnesine çevirir.
    public static Story parse(String json) {
        // Metni çevir.
        return MAPPER.readValue(json, Story.class);
    }

    // Projeye paketlenmiş bir JSON dosyasını okur.
    private static JsonNode readTree(String path) {
        // try (...): iş bitince dosyayı otomatik kapatır.
        try (InputStream in = StoryLoader.class.getClassLoader().getResourceAsStream(path)) {
            // Dosya yoksa anlaşılır bir hatayla dur.
            if (in == null) {
                throw new IllegalArgumentException("Story file not found: " + path);
            }
            // Dosyayı oku.
            return MAPPER.readTree(in);
        } catch (IOException exception) {
            // Hatayı metot imzasını değiştirmeyen tipe çevir.
            throw new UncheckedIOException(exception);
        }
    }
}
