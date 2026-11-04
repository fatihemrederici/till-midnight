// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.story;

// Liste tipi.
import java.util.List;
// Anahtar-değer tipi.
import java.util.Map;

// Hikâyedeki tüm olası yolların raporu.
public record StoryAnalysis(
        // Toplam yol sayısı.
        int paths,
        // En uzun yoldaki kart sayısı.
        int longestRun,
        // Son -> o sona giden yol sayısı.
        Map<String, Integer> endingCounts,
        // Hiçbir yolda çıkmayan kartlar.
        List<String> unreachableCards,
        // Hiçbir yolla ulaşılamayan sonlar.
        List<String> unreachableEndings,
        // Yüzde kaç yol kurtuluşla bitiyor.
        int survivalPercent) {}
