// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.story;

// Dosya okurken çıkabilecek hata tipi.
import java.io.IOException;
// Dosyayı bayt bayt okumaya yarayan tip.
import java.io.InputStream;
// IOException'ı metot imzasına yazmak zorunda kalmamak için kullandığımız hata tipi.
import java.io.UncheckedIOException;
// Özet hesaplama.
import java.security.MessageDigest;
// Özet algoritması bulunamazsa çıkan hata.
import java.security.NoSuchAlgorithmException;
// Baytları onaltılık metne çevirir.
import java.util.HexFormat;
// Sırasını koruyan anahtar-değer.
import java.util.LinkedHashMap;
// Liste arayüzü.
import java.util.List;
// Anahtar-değer arayüzü.
import java.util.Map;
// Bulunamayabilecek bir değer.
import java.util.Optional;
// Jackson ayarı: JSON'dan okuma özellikleri.
import tools.jackson.databind.DeserializationFeature;
// JSON ağacı.
import tools.jackson.databind.JsonNode;
// Jackson'ın JSON çeviricisi. Swift'teki JSONDecoder gibi.
import tools.jackson.databind.json.JsonMapper;
// JSON nesnesi.
import tools.jackson.databind.node.ObjectNode;

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
        // Kuralları oku.
        JsonNode rules = readTree(folder + "story.json");
        // Kimlik klasörün adıyla aynı olmalı, yoksa kayıtlar yanlış bölüme yazılır.
        if (!id.equals(rules.path("id").asString())) {
            throw new IllegalArgumentException("Chapter folder " + id + " has a different id in story.json");
        }
        // Birleştir.
        Story story = build(rules, texts);
        // Yazılan her fotoğraf gerçekten var olmalı, yoksa oyunda boş bir çerçeve görünür.
        for (Story.Card card : story.cards()) {
            if (card.image() != null && StoryLoader.class.getClassLoader().getResource(folder + "images/" + card.image()) == null) {
                throw new IllegalArgumentException("Card " + card.id() + ": image not found: " + card.image());
            }
        }
        return story;
    }

    // Bir bölümün fotoğrafını okur. Bölüm ya da dosya yoksa boş döner.
    public static Optional<byte[]> readImage(String chapterId, String file) {
        // Dosya adı sadece harf, rakam ve tire olabilir: Başka klasöre çıkılamasın.
        if (!chapterId.matches("[a-z0-9-]+") || !file.matches("[a-z0-9-]+\\.jpg")) {
            return Optional.empty();
        }
        // try (...): iş bitince dosyayı otomatik kapatır.
        try (InputStream in = StoryLoader.class.getClassLoader().getResourceAsStream(ROOT + chapterId + "/images/" + file)) {
            return in == null ? Optional.empty() : Optional.of(in.readAllBytes());
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    // Python'dan gelen {"rules": {...}, "texts": {"en": {...}}} paketini okur.
    public static Pack parsePack(String json) {
        // Paketi oku.
        JsonNode pack = MAPPER.readTree(json);
        // Dil -> metin ağacı.
        Map<String, JsonNode> texts = new LinkedHashMap<>();
        // Paketteki dilleri dön.
        pack.path("texts").properties().forEach(entry -> texts.put(entry.getKey(), entry.getValue()));
        // En az bir dil olmalı.
        if (!pack.path("rules").isObject() || texts.isEmpty()) {
            throw new IllegalArgumentException("Pack needs rules and at least one language in texts");
        }
        // Hikâyeyi ve dilleri döndür.
        return new Pack(build(pack.get("rules"), texts), List.copyOf(texts.keySet()));
    }

    // Okunan paket: Hikâye ve içindeki diller.
    public record Pack(Story story, List<String> languages) {}

    // Kuralları ve dilleri birleştirip Story nesnesine çevirir.
    private static Story build(JsonNode rules, Map<String, JsonNode> texts) {
        // Birleştir.
        ObjectNode merged = StoryMerger.merge(rules, texts);
        // Sürümü ekle.
        merged.put("version", versionOf(rules));
        // Nesneye çevir.
        return MAPPER.treeToValue(merged, Story.class);
    }

    // Kuralların SHA-256 özetinin ilk 12 karakteri. Elle yazılan sürüm numarası unutulurdu, bu unutulmaz.
    private static String versionOf(JsonNode rules) {
        try {
            // Özeti hesapla.
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(MAPPER.writeValueAsBytes(rules));
            // Onaltılık metne çevirip kısalt.
            return HexFormat.of().formatHex(digest).substring(0, 12);
        } catch (NoSuchAlgorithmException exception) {
            // SHA-256 her Java'da vardır, buraya gelinmez.
            throw new IllegalStateException(exception);
        }
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
