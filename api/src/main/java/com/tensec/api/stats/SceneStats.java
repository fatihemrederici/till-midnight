// Bu dosyanın ait olduğu paket.
package com.tensec.api.stats;

// Liste tipi.
import java.util.List;

// Bir sahnenin bir günlük istatistikleri.
public record SceneStats(
        // Toplam deneme.
        int attempts,
        // Farklı oyuncu sayısı.
        int players,
        // En az bir kez kazanan oyuncu sayısı.
        int solvers,
        // Kazananların ilk zaferine kadar yaptığı denemelerin ortancası. Kazanan yoksa null.
        Double medianTriesToSolve,
        // Hangi saniyede kaç kayıp oldu. En çok olandan aza sıralı.
        List<LossSecond> losses) {

    // Bir saniyedeki kayıp sayısı.
    public record LossSecond(int second, int count) {}
}