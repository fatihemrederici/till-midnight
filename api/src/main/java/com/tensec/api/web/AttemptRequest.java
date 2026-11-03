// Bu dosyanın ait olduğu paket.
package com.tensec.api.web;

// Dokunuş tipi.
import com.tensec.api.engine.Touch;
// Liste tipi.
import java.util.List;

// POST /attempt isteğinin gövdesi. Spring gelen JSON'u otomatik olarak buna çevirir.
public record AttemptRequest(String sceneId, List<Touch> touches) {

    // Nesne oluşurken çalışan kısım.
    public AttemptRequest {
        // touches gönderilmediyse null yerine boş liste koy.
        touches = touches == null ? List.of() : touches;
    }
}