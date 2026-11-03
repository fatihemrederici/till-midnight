// Bu dosyanın ait olduğu paket.
package com.tensec.api.engine;

// Sıralı liste tipi.
import java.util.List;

// Bir döngünün sonucu: kazandı mı, kaçıncı saniyede bitti, hangi felaket oldu, neler yaşandı.
// Oyuncu kazandıysa disasterId null olur.
public record SimulationResult(boolean won, int endSecond, String disasterId, List<TimelineEvent> events) {}
