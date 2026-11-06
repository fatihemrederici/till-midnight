// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.story;

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
        // Kuralların parmak izi. Dosyalarda yazmaz, yükleyici hesaplar. Sadece çeviri değişirse aynı kalır.
        String version,
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
        // Göstergeler: Her bölümde aynı ikisi, akıl ve sağlık.
        List<Meter> meters,
        // Tuzak işaretleri: Oyuncuya görünmez, kurulunca sadece hafif bir ses çalar.
        List<String> traps,
        // Kartlar, oynanacak sırayla.
        List<Card> cards,
        // Tüm sonlar: ölümler ve kurtuluşlar.
        List<Ending> endings,
        // Oyuncunun gerçekten bulabileceği ölümler. Dosyada yazmaz, sunucu açılırken analizden hesaplanır.
        List<String> deaths) {

    // Aynı hikâyeyi bulunabilen ölümlerle döndürür.
    public Story withDeaths(List<String> found) {
        // Record değişmez, bu yüzden yeni bir kopya.
        return new Story(formatVersion, id, version, title, vision, wake, start, maxMeter, meters, traps, cards, endings, found);
    }

    // Kartların geçebileceği yerler. Gösterge ölümü bu yere göre seçilir.
    public static final List<String> PLACES = List.of("street", "home", "work");
    // Her bölümdeki göstergeler, bu sırayla.
    public static final List<String> METERS = List.of("mind", "body");

    // Bir gösterge. Sıfıra inince oyun biter; endings, o an bulunulan yere göre hangi sonun geleceğini söyler.
    public record Meter(String id, String emoji, Map<String, String> label, Map<String, String> endings) {}

    // Bir kart. when doluysa sadece koşul tutarsa çıkar. place: sokak, ev ya da iş.
    // shownTime: Ekranda gösterilen saat, gerçek saatten farklıysa. Örneğin ileri giden bir saat.
    public record Card(
            String id,
            String time,
            String shownTime,
            String place,
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
            // Koşulu tutan ilk ölüm oyunu bitirir.
            List<Death> deaths,
            Map<String, String> after,
            // Ölüm zincirinde bu seçimi anlatan geçmiş zaman cümlesi.
            Map<String, String> past) {}

    // Bu seçim, koşul tutarsa oyuncuyu öldürür. Koşul yoksa her zaman öldürür.
    public record Death(Map<String, Boolean> when, String ending) {}

    // Bir son. won true ise kurtuluştur; when, hangi kurtuluşun seçileceğini belirler.
    // omen: Akıl düşükken kartın yerine bir anlığına geçen, bu ölümü ima eden cümle.
    public record Ending(
            String id,
            boolean won,
            Map<String, Boolean> when,
            Map<String, String> title,
            Map<String, String> text,
            Map<String, String> hint,
            Map<String, String> omen) {}
}
