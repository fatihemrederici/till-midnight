// Bu dosyanın ait olduğu paket.
package com.tensec.api.web;

// Anahtar-değer tipi.
import java.util.Map;
// HTTP durum kodları.
import org.springframework.http.HttpStatus;
// Bu hata tipi yakalanınca çalışacak metodu işaretler.
import org.springframework.web.bind.annotation.ExceptionHandler;
// Metodun döndüreceği HTTP kodunu belirler.
import org.springframework.web.bind.annotation.ResponseStatus;
// Tüm controller'lardaki hataları tek yerde yakalar.
import org.springframework.web.bind.annotation.RestControllerAdvice;

// API hatalarını anlaşılır JSON cevaplarına çevirir.
@RestControllerAdvice
public class ApiExceptionHandler {

    // Motor geçersiz dokunuşta IllegalArgumentException fırlatır, burada yakalıyoruz.
    @ExceptionHandler(IllegalArgumentException.class)
    // Yoksa 500 (sunucu hatası) dönerdi. Oysa hata istekte, bu yüzden 400 dönüyoruz.
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(IllegalArgumentException exception) {
        // Cevap: {"error": "Too many touches..."}
        return Map.of("error", exception.getMessage());
    }
}