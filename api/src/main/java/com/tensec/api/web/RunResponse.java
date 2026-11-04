// Bu dosyanın ait olduğu paket.
package com.tensec.api.web;

// İstatistik tipi.
import com.tensec.api.run.StoryStats;

// POST /runs cevabı: sunucunun hesapladığı son ve istatistikler.
public record RunResponse(String endingId, boolean won, StoryStats stats) {}
