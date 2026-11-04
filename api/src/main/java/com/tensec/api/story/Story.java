// Bu dosyanın ait olduğu paket.
package com.tensec.api.story;

// Liste tipi.
import java.util.List;
// Anahtar-değer tipi.
import java.util.Map;

// Hikâye JSON dosyasının Java karşılığı. Kurallar için docs/story-format.md dosyasına bak.
// Metinler Map<dil, metin> olarak tutulur: {"en": "...", "tr": "...", "pl": "..."}.
public record Story(
        // Format sürümü.
        int formatVersion,
        // Hikâyenin adı, örneğin "first-day".
        String id,
        // Başlık, üç dilde.
        Map<String, String> title,
        // Başlangıçtaki kehanet cümleleri.
        List<Map<String, String>> vision,
        // Kehanetten uyanış cümlesi.
        Map<String, String> wake,
        // Göstergelerin başlangıç değerleri.
        Map<String, Integer> start,
        // Bir göstergenin çıkabileceği en yüksek değer.
        int maxMeter,
        // Göstergeler: akıl, sağlık, güven.
        List<Meter> meters,
        // Kartlar, oynanacak sırayla.
        List<Card> cards,
        // Tüm sonlar: ölümler ve kurtuluşlar.
        List<Ending> endings) {

    // Bir gösterge. Sıfıra inerse ending ile biter.
    public record Meter(String id, String emoji, Map<String, String> label, String ending) {}

    // Bir kart. when doluysa sadece koşul tutarsa çıkar.
    public record Card(
            String id,
            String time,
            String emoji,
            Map<String, String> who,
            Map<String, String> text,
            List<Variant> variants,
            Map<String, Boolean> when,
            Choice left,
            Choice right) {}

    // Duruma göre değişen kart metni.
    public record Variant(Map<String, Boolean> when, Map<String, String> text) {}

    // Kartı sola ya da sağa kaydırmanın sonucu.
    public record Choice(
            Map<String, String> label,
            Map<String, Integer> effects,
            Map<String, Boolean> set,
            Death death,
            Map<String, String> after) {}

    // Bu seçim, koşul tutarsa oyuncuyu öldürür.
    public record Death(Map<String, Boolean> when, String ending) {}

    // Bir son. won true ise kurtuluştur; when, hangi kurtuluşun seçileceğini belirler.
    public record Ending(
            String id,
            boolean won,
            Map<String, Boolean> when,
            Map<String, String> title,
            Map<String, String> text,
            List<ChainStep> chain,
            Map<String, String> hint) {}

    // Ölüm ekranındaki zincirin bir adımı.
    public record ChainStep(Map<String, Boolean> when, Map<String, String> text) {}
}
