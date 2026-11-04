// Bu dosyanın ait olduğu paket.
package com.tensec.api.engine;

// Liste tipi.
import java.util.List;

// Kehanet: oyuncu hiçbir şey yapmazsa olacak her şey. İlk felakette durmaz, sona kadar gider.
public record Foresight(List<TimelineEvent> events, List<FiredDisaster> disasters) {

    // Gerçekleşen bir felaket: kaçıncı saniyede, hangisi, ekranda hangi cümleyle.
    public record FiredDisaster(int second, String id, String caption) {}
}
