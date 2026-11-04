// Bu dosyanın ait olduğu paket.
package com.tensec.api.run;

// Benzersiz kimlik tipi.
import java.util.UUID;

// Kaydedilecek koşu.
public record SavedRun(
        UUID runId, UUID playerId, String storyId, String endingId, boolean won, int cardsPlayed) {}
