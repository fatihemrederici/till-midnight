// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.story;

// Test kontrolleri.
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Kaydırma yönü.
import com.tillmidnight.api.story.StoryEngine.Side;
// Dosya okuma hatası.
import java.io.IOException;
// Metin kodlaması.
import java.nio.charset.StandardCharsets;
// Liste.
import java.util.List;
// Anahtar-değer.
import java.util.Map;
// JUnit test etiketi.
import org.junit.jupiter.api.Test;
// Her bölüm için aynı testi tekrar çalıştırmak için.
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
// Test kaynak dosyalarını okumak için.
import org.springframework.core.io.ClassPathResource;

// Motorun testleri. Spring açılmaz, motor doğrudan oluşturulur.
class StoryEngineTest {

    // Gerçek bölüm.
    private final Story story = StoryLoader.loadChapter("first-day");
    // Test edilen motor.
    private final StoryEngine engine = new StoryEngine();

    // Tüm bölümler.
    static List<String> chapters() {
        return StoryLoader.chapterIds();
    }

    // story-paths dosyasındaki her yol beklenen sonla biter. Web motoru aynı dosyayla test edilir.
    @ParameterizedTest
    @MethodSource("chapters")
    void everyKnownPathEndsAsExpected(String id) throws IOException {
        // Bölümü yükle.
        Story chapter = StoryLoader.loadChapter(id);
        // Satırlar "LRL stairs" biçiminde.
        List<String> lines = new ClassPathResource("story-paths/" + id + ".txt")
                .getContentAsString(StandardCharsets.UTF_8)
                .lines()
                .toList();
        // Her satırı oynat.
        for (String line : lines) {
            String[] parts = line.split(" ");
            List<Side> choices = parts[0].chars().mapToObj(c -> c == 'L' ? Side.LEFT : Side.RIGHT).toList();
            assertEquals(parts[1], engine.play(chapter, choices).endingId(), line);
        }
        // Dosya boş olmamalı.
        assertTrue(lines.size() > 100);
    }

    // Küçük bir test bölümü: Akıl biterse ölüm, bulunulan yere göre seçilir.
    private static final String MINI = """
            {"rules":{"formatVersion":2,"id":"mini","start":{"mind":1,"body":2},"maxMeter":5,
            "meters":[{"id":"mind","emoji":"x","endings":{"street":"m-street","home":"m-home","work":"m-work"}},
            {"id":"body","emoji":"x","endings":{"street":"b-street","home":"b-home","work":"b-work"}}],
            "traps":["oil"],
            "cards":[{"id":"a","time":"10:00","place":"street","emoji":"x",
            "left":{"effects":{"body":-1},"set":{"oil":true}},"right":{"set":{"water":true}}},
            {"id":"b","time":"11:00","place":"home","emoji":"x",
            "left":{"deaths":[{"when":{"oil":true},"ending":"fire"},{"when":{"water":true},"ending":"flood"}]},
            "right":{"effects":{"mind":-1}}}],
            "endings":[{"id":"m-street","won":false},{"id":"m-home","won":false},{"id":"m-work","won":false},
            {"id":"b-street","won":false},{"id":"b-home","won":false},{"id":"b-work","won":false},
            {"id":"fire","won":false},{"id":"flood","won":false},{"id":"win","won":true}]},
            "texts":{"en":{"title":"t","vision":["v"],"wake":"w","meters":{"mind":"M","body":"B"},
            "cards":{"a":{"who":"w","text":"t","left":{"label":"l","after":"a","past":"p"},"right":{"label":"r","after":"a","past":"p"}},
            "b":{"who":"w","text":"t","left":{"label":"l","after":"a","past":"p"},"right":{"label":"r","after":"a","past":"p"}}},
            "endings":{"m-street":{"title":"t","text":"t","hint":"h","omen":"o"},"m-home":{"title":"t","text":"t","hint":"h","omen":"o"},
            "m-work":{"title":"t","text":"t","hint":"h","omen":"o"},"b-street":{"title":"t","text":"t","hint":"h","omen":"o"},
            "b-home":{"title":"t","text":"t","hint":"h","omen":"o"},"b-work":{"title":"t","text":"t","hint":"h","omen":"o"},
            "fire":{"title":"t","text":"t","hint":"h","omen":"o"},"flood":{"title":"t","text":"t","hint":"h","omen":"o"},
            "win":{"title":"t","text":"t","hint":"h"}}}}}
            """;

    // Gösterge biterse ölüm, o kartın yerine göre seçilir.
    @Test
    void anEmptyMeterKillsYouWhereYouAre() {
        Story mini = StoryLoader.parsePack(MINI).story();
        // Evde akıl biter.
        assertEquals("m-home", engine.play(mini, List.of(Side.RIGHT, Side.RIGHT)).endingId());
    }

    // Tuzak kurulduysa tetikleyen seçim öldürür; aynı seçimde ilk tutan ölüm kazanır.
    @Test
    void aTrapKillsWhenItIsTriggered() {
        Story mini = StoryLoader.parsePack(MINI).story();
        assertEquals("fire", engine.play(mini, List.of(Side.LEFT, Side.LEFT)).endingId());
        assertEquals("flood", engine.play(mini, List.of(Side.RIGHT, Side.LEFT)).endingId());
    }

    // Küçük bölüm de doğrulayıcıdan geçer.
    @Test
    void theMiniStoryIsValid() {
        Story mini = StoryLoader.parsePack(MINI).story();
        assertEquals(List.of(), new StoryValidator().validate(mini, List.of("en")));
    }

    // Sakinleştirici aldın, bir saat sonra yıkanmış merdiveni seçtin.
    @Test
    void aSedativeKillsYouOnTheMoppedStairs() {
        // Uzaklaş, haberler, bekle, otur, hastayım, sakinleştirici, merdiven.
        StoryEngine.Result result = engine.play(story, List.of(
                Side.RIGHT, Side.RIGHT, Side.LEFT, Side.LEFT, Side.RIGHT, Side.LEFT, Side.LEFT));

        assertEquals("stairs", result.endingId());
        assertEquals(false, result.won());
        assertEquals(7, result.cardsPlayed());
    }

    // Yarım ya da fazla uzun koşu kabul edilmez.
    @Test
    void rejectsInvalidRuns() {
        assertThrows(IllegalArgumentException.class, () -> engine.play(story, List.of(Side.LEFT)));
        assertThrows(IllegalArgumentException.class, () -> engine.play(story, List.of(
                Side.RIGHT, Side.RIGHT, Side.LEFT, Side.LEFT, Side.RIGHT, Side.LEFT, Side.LEFT, Side.LEFT)));
    }

    // Hiç ayarlanmamış işaret false sayılır.
    @Test
    void treatsMissingFlagsAsFalse() {
        assertTrue(StoryEngine.holds(null, Map.of()));
        assertTrue(StoryEngine.holds(Map.of("elif", false), Map.of()));
    }
}
