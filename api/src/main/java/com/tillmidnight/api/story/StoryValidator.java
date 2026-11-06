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
// Küme arayüzü.
import java.util.Set;
// Spring'in "bu sınıfı sen yönet" etiketi.
import org.springframework.stereotype.Component;

// Hikâyenin yapı hatalarını bulur. Hataları tek tek listeler, çünkü AI bu mesajları okuyup düzeltecek.
@Component
public class StoryValidator {

    // Oyunun desteklediği diller. Her metin üçünde de olmalı.
    public static final List<String> LANGUAGES = List.of("en", "tr", "pl");

    // İzin verilen en fazla kart. Analiz süresini sınırlar.
    private static final int MAX_CARDS = 30;

    // Sadece verilen dilleri zorunlu sayar. Çeviri yapılmadan önce taslak kontrol etmeye yarar.
    public List<String> validate(Story story, List<String> languages) {
        // Tüm hatalardan, istenmeyen dillerin eksik uyarılarını çıkar.
        return validate(story).stream()
                .filter(error -> LANGUAGES.stream()
                        .filter(language -> !languages.contains(language))
                        .noneMatch(language -> error.endsWith(": missing language " + language)))
                .toList();
    }

    // Hataların listesini döndürür. Liste boşsa hikâye geçerlidir.
    public List<String> validate(Story story) {
        // Bulunan hatalar.
        List<String> errors = new ArrayList<>();
        // JSON'daki "null" metni boş hikâye üretir.
        if (story == null) {
            errors.add("Story is empty");
            return errors;
        }
        // Temel alanlar olmadan devam edilemez.
        if (story.id() == null || story.meters() == null || story.cards() == null || story.endings() == null
                || story.start() == null) {
            errors.add("id, start, meters, cards and endings are required");
            return errors;
        }
        // Format sürümü.
        if (story.formatVersion() != 1) {
            errors.add("formatVersion must be 1");
        }
        // Kart sayısı.
        if (story.cards().isEmpty() || story.cards().size() > MAX_CARDS) {
            errors.add("A story must have between 1 and " + MAX_CARDS + " cards");
        }
        // Başlık ve uyanış metni.
        checkText(story.title(), "title", errors);
        checkText(story.wake(), "wake", errors);
        // Kehanet cümleleri.
        if (story.vision() == null || story.vision().isEmpty()) {
            errors.add("vision is required");
        } else {
            story.vision().forEach(line -> checkText(line, "vision", errors));
        }

        // Son adlarını topla.
        Set<String> endingIds = new HashSet<>();
        // Aynı adda iki son olmasın.
        for (Story.Ending ending : story.endings()) {
            if (!endingIds.add(ending.id())) {
                errors.add("Duplicate ending: " + ending.id());
            }
            checkText(ending.title(), "Ending " + ending.id() + " title", errors);
            checkText(ending.text(), "Ending " + ending.id() + " text", errors);
            checkText(ending.hint(), "Ending " + ending.id() + " hint", errors);
            // Zincir adımları.
            if (ending.chain() != null) {
                ending.chain().forEach(step -> checkText(step.text(), "Ending " + ending.id() + " chain", errors));
            }
        }
        // Kurtuluş olmadan oyun kazanılamaz. Koşulsuz bir kurtuluş her zaman son çare olur.
        boolean hasFallback = story.endings().stream().anyMatch(e -> e.won() && e.when() == null);
        if (!hasFallback) {
            errors.add("A story needs a winning ending without a condition");
        }

        // Gösterge adlarını topla ve kontrol et.
        Set<String> meterIds = new HashSet<>();
        for (Story.Meter meter : story.meters()) {
            meterIds.add(meter.id());
            checkText(meter.label(), "Meter " + meter.id(), errors);
            // Göstergenin sonu var mı?
            if (!endingIds.contains(meter.ending())) {
                errors.add("Meter " + meter.id() + ": unknown ending " + meter.ending());
            }
            // Başlangıç değeri geçerli mi?
            Integer value = story.start().get(meter.id());
            if (value == null || value < 1 || value > story.maxMeter()) {
                errors.add("Meter " + meter.id() + ": start must be between 1 and " + story.maxMeter());
            }
        }

        // Kriz kartları: Metinleri ve iki seçimi olmalı.
        for (Story.Meter meter : story.meters()) {
            // Ekran etkisi bilinen türlerden olmalı.
            if (meter.effect() != null && !List.of("mind", "body", "trust").contains(meter.effect())) {
                errors.add("Meter " + meter.id() + ": unknown effect " + meter.effect());
            }
            if (meter.crisis() != null) {
                checkCrisis(meter.crisis(), "Crisis " + meter.id(), meterIds, endingIds, errors);
            }
        }

        // Kartları kontrol et.
        Set<String> cardIds = new HashSet<>();
        for (Story.Card card : story.cards()) {
            // Aynı adda iki kart olmasın.
            if (!cardIds.add(card.id())) {
                errors.add("Duplicate card: " + card.id());
            }
            String name = "Card " + card.id();
            checkText(card.who(), name + " who", errors);
            checkText(card.text(), name + " text", errors);
            // Değişken metinler.
            if (card.variants() != null) {
                card.variants().forEach(variant -> checkText(variant.text(), name + " variant", errors));
            }
            // İki taraf da olmalı.
            if (card.left() == null || card.right() == null) {
                errors.add(name + ": left and right are required");
                continue;
            }
            checkChoice(card.left(), name + " left", meterIds, endingIds, errors);
            checkChoice(card.right(), name + " right", meterIds, endingIds, errors);
            // İki taraf birden kilitlenirse oyuncu sıkışır.
            if (card.left().requires() != null && card.right().requires() != null) {
                errors.add(name + ": one side must always be free");
            }
        }
        // Tüm hataları döndür.
        return errors;
    }

    // Bir kriz kartını kontrol eder.
    private void checkCrisis(
            Story.Card card, String name, Set<String> meterIds, Set<String> endingIds, List<String> errors) {
        // Kişi ve metin.
        checkText(card.who(), name + " who", errors);
        checkText(card.text(), name + " text", errors);
        // İki taraf da olmalı.
        if (card.left() == null || card.right() == null) {
            errors.add(name + ": left and right are required");
            return;
        }
        checkChoice(card.left(), name + " left", meterIds, endingIds, errors);
        checkChoice(card.right(), name + " right", meterIds, endingIds, errors);
    }

    // Bir seçimi kontrol eder.
    private void checkChoice(
            Story.Choice choice, String name, Set<String> meterIds, Set<String> endingIds, List<String> errors) {
        // Seçimin yazısı.
        checkText(choice.label(), name + " label", errors);
        // Etkiler var olan göstergelere mi?
        if (choice.effects() != null) {
            for (Map.Entry<String, Integer> effect : choice.effects().entrySet()) {
                if (!meterIds.contains(effect.getKey())) {
                    errors.add(name + ": unknown meter " + effect.getKey());
                }
            }
        }
        // Şartlar var olan göstergelere mi?
        if (choice.requires() != null) {
            for (String meter : choice.requires().keySet()) {
                if (!meterIds.contains(meter)) {
                    errors.add(name + ": unknown meter " + meter);
                }
            }
        }
        // Ölüm var olan bir sona mı gidiyor?
        boolean alwaysDies = choice.death() != null && choice.death().when() == null;
        if (choice.death() != null && !endingIds.contains(choice.death().ending())) {
            errors.add(name + ": unknown ending " + choice.death().ending());
        }
        // Her zaman öldürmüyorsa, sonraki karta geçişte bir metin gerekir.
        if (!alwaysDies) {
            checkText(choice.after(), name + " after", errors);
        }
    }

    // Bir metnin üç dilde de dolu olduğunu kontrol eder.
    private void checkText(Map<String, String> text, String name, List<String> errors) {
        // Hiç metin yok.
        if (text == null) {
            errors.add(name + ": text is missing");
            return;
        }
        // Her dili dön.
        for (String language : LANGUAGES) {
            // Dil eksik ya da boş.
            String value = text.get(language);
            if (value == null || value.isBlank()) {
                errors.add(name + ": missing language " + language);
            }
        }
    }
}
