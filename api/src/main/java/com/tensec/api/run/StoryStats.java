// Bu dosyanın ait olduğu paket.
package com.tensec.api.run;

// Bir hikâyenin ve bir sonun istatistikleri.
public record StoryStats(
        // Toplam koşu.
        int runs,
        // Farklı oyuncu.
        int players,
        // Kurtulan koşular.
        int survived,
        // Bu sona ulaşan koşular.
        int sameEnding) {}
