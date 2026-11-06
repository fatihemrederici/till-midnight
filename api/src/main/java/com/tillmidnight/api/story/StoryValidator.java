// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.story;

// Değiştirilebilir liste.
import java.util.ArrayList;
// Değiştirilebilir küme.
import java.util.HashSet;
// Liste arayüzü.
import java.util.List;
// Anahtar-değer arayüzü.
import java.util.Map;
// Akışta boş olabilen değerleri atlamak için.
import java.util.Objects;
// Küme arayüzü.
import java.util.Set;
// Akış: İç içe listeleri tek akışa çevirmek için.
import java.util.stream.Stream;
// Spring'in "bu sınıfı sen yönet" etiketi.
import org.springframework.stereotype.Component;

// Hikâyenin kurallara uyup uymadığını kontrol eder. Yolları denemez, onu StoryAnalyzer yapar.
// Buradaki kurallar oyunun tasarımıdır: Bedelsiz seçim yok, her ölümün bir sebebi var, her tuzak bir yerde öldürür.
@Component
public class StoryValidator {

    // Oyunun dilleri.
    public static final List<String> LANGUAGES = List.of("en", "tr", "pl");

    // Bir bölümdeki en fazla kart.
    private static final int MAX_CARDS = 40;

    // Hikâyeyi sadece verilen dillerle kontrol eder. Taslaklar tek dille de kontrol edilebilir.
    public List<String> validate(Story story, List<String> languages) {
        // Verilmeyen dillerin eksik metin hatalarını at.
        return validate(story).stream()
                .filter(error -> LANGUAGES.stream()
                        .filter(language -> !languages.contains(language))
                        .noneMatch(language -> error.endsWith(": missing language " + language)))
                .toList();
    }

    // Hikâyeyi tüm dillerle kontrol eder. Boş liste: hata yok.
    public List<String> validate(Story story) {
        // Bulunan hatalar.
        List<String> errors = new ArrayList<>();
        // Hikâye hiç yoksa devam edilemez.
        if (story == null) {
            errors.add("Story is empty");
            return errors;
        }
        // Zorunlu alanlar yoksa devam edilemez.
        if (story.id() == null || story.meters() == null || story.cards() == null || story.endings() == null
                || story.start() == null) {
            errors.add("id, start, meters, cards and endings are required");
            return errors;
        }
        // Format sürümü.
        if (story.formatVersion() != 2) {
            errors.add("formatVersion must be 2");
        }
        // Kart sayısı.
        if (story.cards().isEmpty() || story.cards().size() > MAX_CARDS) {
            errors.add("A story must have between 1 and " + MAX_CARDS + " cards");
        }
        // Başlık, uyanış ve kehanet.
        checkText(story.title(), "title", errors);
        checkText(story.wake(), "wake", errors);
        if (story.vision() == null || story.vision().isEmpty()) {
            errors.add("vision is required");
        } else {
            story.vision().forEach(line -> checkText(line, "vision", errors));
        }

        // Sonlar.
        Set<String> endingIds = new HashSet<>();
        Set<String> deathIds = new HashSet<>();
        for (Story.Ending ending : story.endings()) {
            // Aynı kimlik iki kez olmaz.
            if (!endingIds.add(ending.id())) {
                errors.add("Duplicate ending: " + ending.id());
            }
            // Ölümler ayrıca tutulur.
            if (!ending.won()) {
                deathIds.add(ending.id());
                // Akıl efekti her ölüm için bir cümleye ihtiyaç duyar.
                checkText(ending.omen(), "Ending " + ending.id() + " omen", errors);
            }
            // Metinler.
            checkText(ending.title(), "Ending " + ending.id() + " title", errors);
            checkText(ending.text(), "Ending " + ending.id() + " text", errors);
            checkText(ending.hint(), "Ending " + ending.id() + " hint", errors);
        }
        // Koşulsuz bir kurtuluş olmalı, yoksa gece yarısında hiçbir son tutmayabilir.
        if (story.endings().stream().noneMatch(e -> e.won() && e.when() == null)) {
            errors.add("A story needs a winning ending without a condition");
        }

        // Göstergeler: Her bölümde aynı ikisi, aynı sırada.
        List<String> meterIds = story.meters().stream().map(Story.Meter::id).toList();
        if (!meterIds.equals(Story.METERS)) {
            errors.add("Meters must be exactly " + Story.METERS);
        }
        for (Story.Meter meter : story.meters()) {
            // Adı.
            checkText(meter.label(), "Meter " + meter.id(), errors);
            // Her yer için bir ölüm.
            for (String place : Story.PLACES) {
                String ending = meter.endings() == null ? null : meter.endings().get(place);
                if (!deathIds.contains(ending)) {
                    errors.add("Meter " + meter.id() + ": needs a death for " + place);
                }
            }
            // Başlangıç değeri.
            Integer value = story.start().get(meter.id());
            if (value == null || value < 1 || value > story.maxMeter()) {
                errors.add("Meter " + meter.id() + ": start must be between 1 and " + story.maxMeter());
            }
        }

        // Bir seçimle doğru yapılan işaretler. Koşullardaki yazım hatalarını yakalamak için.
        Set<String> setFlags = new HashSet<>();
        story.cards().stream()
                .flatMap(card -> Stream.of(card.left(), card.right()))
                .filter(Objects::nonNull)
                .filter(choice -> choice.set() != null)
                .forEach(choice -> choice.set().forEach((flag, value) -> {
                    if (value) {
                        setFlags.add(flag);
                    }
                }));
        // Koşullarda kullanılan işaretler.
        Set<String> usedFlags = new HashSet<>();

        // Kartlar.
        Set<String> cardIds = new HashSet<>();
        String previousTime = "";
        for (Story.Card card : story.cards()) {
            // Aynı kimlik iki kez olmaz.
            if (!cardIds.add(card.id())) {
                errors.add("Duplicate card: " + card.id());
            }
            String name = "Card " + card.id();
            // Yer.
            if (!Story.PLACES.contains(card.place())) {
                errors.add(name + ": place must be one of " + Story.PLACES);
            }
            // Saat biçimi ve sırası.
            if (card.time() == null || !card.time().matches("[0-2][0-9]:[0-5][0-9]")) {
                errors.add(name + ": time must look like 14:30");
            } else if (card.time().compareTo(previousTime) < 0) {
                errors.add(name + ": time goes backwards");
            } else {
                previousTime = card.time();
            }
            // Ekranda gösterilen saat de saat biçiminde olmalı.
            if (card.shownTime() != null && !card.shownTime().matches("[0-2][0-9]:[0-5][0-9]")) {
                errors.add(name + ": shownTime must look like 14:30");
            }
            // Metinler.
            checkText(card.who(), name + " who", errors);
            checkText(card.text(), name + " text", errors);
            // Koşullar.
            checkFlags(card.when(), name, setFlags, usedFlags, errors);
            if (card.variants() != null) {
                for (Story.Variant variant : card.variants()) {
                    checkText(variant.text(), name + " variant", errors);
                    checkFlags(variant.when(), name + " variant", setFlags, usedFlags, errors);
                }
            }
            // İki seçim zorunlu.
            if (card.left() == null || card.right() == null) {
                errors.add(name + ": left and right are required");
                continue;
            }
            checkChoice(card, card.left(), name + " left", deathIds, setFlags, usedFlags, errors);
            checkChoice(card, card.right(), name + " right", deathIds, setFlags, usedFlags, errors);
        }
        // Kurtuluş koşulları da işaret kullanır.
        story.endings().forEach(e -> checkFlags(e.when(), "Ending " + e.id(), setFlags, usedFlags, errors));

        // Tuzaklar: Bir yerde kurulmalı ve bir yerde işe yaramalı.
        for (String trap : story.traps() == null ? List.<String>of() : story.traps()) {
            if (!setFlags.contains(trap)) {
                errors.add("Trap " + trap + " is never set");
            }
            if (!usedFlags.contains(trap)) {
                errors.add("Trap " + trap + " never matters");
            }
        }
        // Hatalar.
        return errors;
    }

    // Bir seçimi kontrol eder.
    private void checkChoice(
            Story.Card card, Story.Choice choice, String name, Set<String> deathIds,
            Set<String> setFlags, Set<String> usedFlags, List<String> errors) {
        // Metinler.
        checkText(choice.label(), name + " label", errors);
        checkText(choice.past(), name + " past", errors);
        // Göstergeler sadece akıl ve sağlık olabilir.
        if (choice.effects() != null) {
            for (String meter : choice.effects().keySet()) {
                if (!Story.METERS.contains(meter)) {
                    errors.add(name + ": unknown meter " + meter);
                }
            }
        }
        // Ölümler.
        List<Story.Death> deaths = choice.deaths() == null ? List.of() : choice.deaths();
        boolean alwaysDies = false;
        for (Story.Death death : deaths) {
            // Bilinen bir ölüm olmalı.
            if (!deathIds.contains(death.ending())) {
                errors.add(name + ": unknown ending " + death.ending());
            }
            checkFlags(death.when(), name, setFlags, usedFlags, errors);
            // Her ölümün bir sebebi olmalı: Ya kendi koşulu ya da kartın koşulu en az bir işarete bağlı.
            if (!hasTrueFlag(death.when()) && !hasTrueFlag(card.when())) {
                errors.add(name + ": a death needs a trap or a card condition");
            }
            // Koşulsuz ölüm her zaman öldürür.
            if (death.when() == null) {
                alwaysDies = true;
            }
        }
        // Sonuç metni, seçim her zaman öldürmüyorsa zorunlu.
        if (!alwaysDies) {
            checkText(choice.after(), name + " after", errors);
        }
        // Bedelsiz seçim yok: Gösterge düşürür, bir işaret kurar ya da öldürebilir.
        boolean costs = (choice.effects() != null && choice.effects().values().stream().anyMatch(v -> v < 0))
                || (choice.set() != null && choice.set().containsValue(true))
                || !deaths.isEmpty();
        if (!costs) {
            errors.add(name + ": every choice needs a cost");
        }
    }

    // Koşuldaki işaretlerin bir yerde kurulduğunu kontrol eder ve kullanılanları kaydeder.
    private void checkFlags(
            Map<String, Boolean> when, String name, Set<String> setFlags, Set<String> usedFlags, List<String> errors) {
        // Koşul yoksa kontrol edecek bir şey yok.
        if (when == null) {
            return;
        }
        for (String flag : when.keySet()) {
            // Hiçbir seçim bu işareti kurmuyorsa büyük ihtimalle yazım hatası.
            if (!setFlags.contains(flag)) {
                errors.add(name + ": flag " + flag + " is never set");
            }
            usedFlags.add(flag);
        }
    }

    // Koşulda doğru olması beklenen en az bir işaret var mı?
    private static boolean hasTrueFlag(Map<String, Boolean> when) {
        // Koşul yoksa yok.
        return when != null && when.containsValue(true);
    }

    // Metnin üç dilde de dolu olup olmadığını kontrol eder.
    private void checkText(Map<String, String> text, String name, List<String> errors) {
        // Hiç yoksa.
        if (text == null) {
            errors.add(name + ": text is missing");
            return;
        }
        // Her dil.
        for (String language : LANGUAGES) {
            String value = text.get(language);
            if (value == null || value.isBlank()) {
                errors.add(name + ": missing language " + language);
            }
        }
    }
}
