// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.story;

// Değiştirilebilir liste.
import java.util.ArrayList;
// Değiştirilebilir anahtar-değer.
import java.util.HashMap;
// Değiştirilebilir küme.
import java.util.HashSet;
// Liste arayüzü.
import java.util.List;
// Anahtar-değer arayüzü.
import java.util.Map;
// Küme arayüzü.
import java.util.Set;
// Bir nesneye uygulanan işlem.
import java.util.function.Consumer;
// JSON ağacı.
import tools.jackson.databind.JsonNode;
// JSON listesi.
import tools.jackson.databind.node.ArrayNode;
// Yeni JSON düğümü üretir.
import tools.jackson.databind.node.JsonNodeFactory;
// JSON nesnesi.
import tools.jackson.databind.node.ObjectNode;

// Kural dosyasını (story.json) ve dil dosyalarını (en.json...) tek hikâyede birleştirir.
// Sonuçtaki her metin {"en": "...", "tr": "...", "pl": "..."} biçimindedir.
// Eksik dil burada hata vermez, çünkü StoryValidator hepsini tek listede raporluyor.
final class StoryMerger {

    // Boş JSON düğümleri üretir.
    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    // Dil -> o dilin metin ağacı.
    private final Map<String, JsonNode> texts;
    // Dil -> kullanılan metinlerin yolları. Sonda fazlalık metinleri bulmaya yarar.
    private final Map<String, Set<String>> used = new HashMap<>();

    // Nesneyi sadece merge metodu üretir.
    private StoryMerger(Map<String, JsonNode> texts) {
        // Metinleri sakla.
        this.texts = texts;
        // Her dil için boş küme aç.
        texts.keySet().forEach(language -> used.put(language, new HashSet<>()));
    }

    // Birleştirilmiş hikâyeyi döndürür. Kullanılmayan metin varsa hata fırlatır.
    static ObjectNode merge(JsonNode rules, Map<String, JsonNode> texts) {
        // Çalışacak nesneyi oluştur.
        StoryMerger merger = new StoryMerger(texts);
        // Kuralların kopyası üzerinde çalış.
        ObjectNode story = (ObjectNode) rules.deepCopy();
        // Başlık ve uyanış.
        merger.addText(story, "title", "/title");
        merger.addText(story, "wake", "/wake");
        // Kehanet cümleleri.
        story.set("vision", merger.visionLines());
        // Göstergeler, kartlar ve sonlar.
        each(story, "meters", merger::addMeterTexts);
        each(story, "cards", card -> merger.addCardTexts(card, "/cards/" + id(card)));
        each(story, "endings", merger::addEndingTexts);
        // Hikâyede karşılığı olmayan metin yanlış yazılmış bir kimlik demektir.
        merger.failOnUnusedTexts(id(story));
        // Sonucu döndür.
        return story;
    }

    // Bir göstergenin adı.
    private void addMeterTexts(ObjectNode meter) {
        // Göstergenin adı.
        addText(meter, "label", "/meters/" + id(meter));
    }

    // Bir kartın ve iki seçiminin metinleri.
    private void addCardTexts(ObjectNode card, String path) {
        // Kişi ve metin.
        addText(card, "who", path + "/who");
        addText(card, "text", path + "/text");
        // Duruma göre değişen metinler sırayla eşleşir.
        addIndexedTexts(card, "variants", path + "/variants");
        // İki seçim.
        for (String side : List.of("left", "right")) {
            // Seçim yoksa atla. Eksik taraf, metni kullanılmamış olarak raporlanır.
            if (!(card.get(side) instanceof ObjectNode choice)) {
                continue;
            }
            // Seçim yazısı.
            addText(choice, "label", path + "/" + side + "/label");
            // Seçimin sonucu, sadece bazı seçimlerde var.
            addText(choice, "after", path + "/" + side + "/after");
            // Ölüm zincirindeki geçmiş zaman cümlesi.
            addText(choice, "past", path + "/" + side + "/past");
        }
    }

    // Bir sonun metinleri.
    private void addEndingTexts(ObjectNode ending) {
        // Metinlerin dil dosyasındaki yeri.
        String path = "/endings/" + id(ending);
        // Başlık, metin ve ipucu.
        addText(ending, "title", path + "/title");
        addText(ending, "text", path + "/text");
        addText(ending, "hint", path + "/hint");
        // Akıl düşükken görünen ölüm cümlesi.
        addText(ending, "omen", path + "/omen");
    }

    // Listedeki her öğeye sırayla metnini ekler.
    private void addIndexedTexts(ObjectNode parent, String field, String path) {
        // Liste yoksa yapılacak iş yok.
        JsonNode items = parent.get(field);
        if (items == null) {
            return;
        }
        // Öğeleri sırayla dön.
        for (int i = 0; i < items.size(); i++) {
            addText((ObjectNode) items.get(i), "text", path + "/" + i);
        }
    }

    // Kehanet cümleleri kural dosyasında yok, sayısı dil dosyalarından gelir.
    private ArrayNode visionLines() {
        // En uzun listeyi bul. Kısa kalan dil eksik metin olarak raporlanır.
        int count = texts.values().stream().mapToInt(text -> text.path("vision").size()).max().orElse(0);
        // Cümleler.
        ArrayNode lines = NODES.arrayNode();
        // Her cümleyi dillerden topla.
        for (int i = 0; i < count; i++) {
            lines.add(textObject("/vision/" + i));
        }
        // Listeyi döndür.
        return lines;
    }

    // Bir alana metin nesnesini ekler. Hiçbir dilde yoksa alan eklenmez.
    private void addText(ObjectNode target, String field, String path) {
        // Metni dillerden topla.
        ObjectNode text = textObject(path);
        // Boş değilse ekle.
        if (!text.isEmpty()) {
            target.set(field, text);
        }
    }

    // Bir yoldaki metni tüm dillerden toplar: {"en": "...", "tr": "..."}.
    private ObjectNode textObject(String path) {
        // Boş nesne.
        ObjectNode text = NODES.objectNode();
        // Dilleri dön.
        texts.forEach((language, tree) -> {
            // Metni bul.
            JsonNode value = tree.at(path);
            // Varsa ekle ve kullanıldı diye işaretle.
            if (value.isString()) {
                text.set(language, value);
                used.get(language).add(path);
            }
        });
        // Nesneyi döndür.
        return text;
    }

    // Dil dosyalarında kullanılmayan metin varsa durur.
    private void failOnUnusedTexts(String storyId) {
        // Bulunan fazlalıklar.
        List<String> unused = new ArrayList<>();
        // Dilleri dön.
        texts.forEach((language, tree) -> collectUnused(tree, "", language, unused));
        // Fazlalık varsa hikâye açılmaz.
        if (!unused.isEmpty()) {
            throw new IllegalArgumentException("Story " + storyId + " has unused texts: " + unused);
        }
    }

    // Metin ağacını gezip kullanılmayan yolları toplar.
    private void collectUnused(JsonNode node, String path, String language, List<String> unused) {
        // Yaprak düğüm bir metindir.
        if (node.isString()) {
            if (!used.get(language).contains(path)) {
                unused.add(language + path);
            }
        } else if (node.isArray()) {
            // Liste öğelerini sırayla gez.
            for (int i = 0; i < node.size(); i++) {
                collectUnused(node.get(i), path + "/" + i, language, unused);
            }
        } else {
            // Nesne alanlarını gez.
            for (Map.Entry<String, JsonNode> entry : node.properties()) {
                collectUnused(entry.getValue(), path + "/" + entry.getKey(), language, unused);
            }
        }
    }

    // Bir listedeki her nesneye işlem uygular.
    private static void each(ObjectNode story, String field, Consumer<ObjectNode> action) {
        // Listenin öğelerini dön.
        story.path(field).forEach(item -> action.accept((ObjectNode) item));
    }

    // Düğümün kimliği.
    private static String id(JsonNode node) {
        // id alanını metin olarak döndür.
        return node.path("id").asString();
    }
}
