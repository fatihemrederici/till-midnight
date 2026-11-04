// Bu dosyanın ait olduğu paket.
package com.tensec.api.web;

// Kaydırma yönü.
import com.tensec.api.story.StoryEngine;
// Liste tipi.
import java.util.List;
// Benzersiz kimlik tipi.
import java.util.UUID;

// POST /runs isteği. Tarayıcı sonucu değil seçimleri gönderir; sonucu sunucu hesaplar.
public record RunRequest(String storyId, UUID runId, UUID playerId, List<StoryEngine.Side> choices) {}
