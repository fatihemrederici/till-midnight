// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.story;

// Liste tipi.
import java.util.List;
// Anahtar-değer tipi.
import java.util.Map;

// Hikâyenin raporu. Olasılıklar, her kartta rastgele sola ya da sağa kaydıran bir oyuncuya göredir.
public record StoryAnalysis(
        // Toplam farklı yol sayısı.
        long paths,
        // En uzun yoldaki kart sayısı.
        int longestRun,
        // Son -> rastgele oyuncunun o sona ulaşma yüzdesi.
        Map<String, Double> endingPercent,
        // Hiçbir yolda çıkmayan kartlar.
        List<String> unreachableCards,
        // Hiçbir yolla ulaşılamayan sonlar.
        List<String> unreachableEndings,
        // Rastgele oyuncunun gece yarısına ulaşma yüzdesi.
        double survivalPercent) {}
