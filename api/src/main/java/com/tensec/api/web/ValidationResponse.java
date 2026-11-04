// Bu dosyanın ait olduğu paket.
package com.tensec.api.web;

// Analiz raporu.
import com.tensec.api.story.StoryAnalysis;
// Liste tipi.
import java.util.List;

// POST /internal/validate cevabı. Yapı hatası varsa analiz çalışmaz ve null kalır.
public record ValidationResponse(boolean valid, List<String> errors, StoryAnalysis analysis) {}
