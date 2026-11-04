// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.web;

// Kaydırma yönü.
import com.tillmidnight.api.story.StoryEngine;
// Liste tipi.
import java.util.List;
// Benzersiz kimlik tipi.
import java.util.UUID;

// POST /runs isteği. Tarayıcı sonucu değil seçimleri gönderir; sonucu sunucu hesaplar.
public record RunRequest(
        String storyId,
        String storyVersion,
        UUID runId,
        UUID playerId,
        List<StoryEngine.Side> choices) {}
