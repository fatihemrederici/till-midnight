// Bu dosyanın ait olduğu paket.
package com.tensec.api.solver;

// Dokunuş tipini motor paketinden içeri alıyoruz.
import com.tensec.api.engine.Touch;
// Liste tipi.
import java.util.List;

// Çözücünün raporu.
public record SolveResult(
        // Denenen toplam dokunuş kombinasyonu.
        int combinationsTried,
        // Bunlardan kaç tanesi kazandırdı.
        int winningCombinations,
        // Kazanmak için gereken en az dokunuş. Çözüm yoksa -1.
        int minTouches,
        // Gereksiz dokunuş içermeyen çözümler.
        List<List<Touch>> minimalSolutions,
        // Zorluk puanı: 0 çok kolay, 100 çözümü yok.
        int difficulty) {

    // Sahnenin en az bir çözümü var mı? Record'lara kendi metotlarımızı da ekleyebiliriz.
    public boolean solvable() {
        // Liste boş değilse çözüm vardır.
        return !minimalSolutions.isEmpty();
    }
}