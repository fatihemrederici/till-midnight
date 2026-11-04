// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.web;

// İstatistik tipi.
import com.tillmidnight.api.run.StoryStats;

// POST /runs cevabı: sunucunun hesapladığı son ve istatistikler.
public record RunResponse(String endingId, boolean won, StoryStats stats) {}
