// Bu dosyanın ait olduğu paket.
package com.tensec.api.web;

// Dokunuş tipi.
import com.tensec.api.engine.Touch;
// Liste tipi.
import java.util.List;
// Benzersiz kimlik tipi.
import java.util.UUID;

// POST /attempt isteğinin gövdesi. Spring gelen JSON'u otomatik olarak buna çevirir.
// playerId ve attemptId gönderilmezse deneme oynanır ama kaydedilmez.
public record AttemptRequest(String sceneId, List<Touch> touches, UUID playerId, UUID attemptId) {

    // Nesne oluşurken çalışan kısım.
    public AttemptRequest {
        // touches gönderilmediyse null yerine boş liste koy.
        touches = touches == null ? List.of() : touches;
    }
}