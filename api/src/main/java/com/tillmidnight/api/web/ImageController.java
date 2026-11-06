// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.web;

// Bölüm klasöründen fotoğraf okumak için.
import com.tillmidnight.api.story.StoryLoader;
// Tarayıcının fotoğrafı ne kadar saklayabileceği.
import java.time.Duration;
// HTTP cevabının parçaları.
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
// Web etiketleri.
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

// Kartların fotoğraflarını verir: /stories/first-day/images/bus.jpg
@RestController
public class ImageController {

    // Fotoğraflar değişmez sayılır: Tarayıcı bir gün boyunca tekrar istemez.
    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofDays(1)).cachePublic();

    // Bir kartın fotoğrafı. Yoksa 404.
    @GetMapping("/stories/{chapterId}/images/{file}")
    public ResponseEntity<byte[]> image(@PathVariable String chapterId, @PathVariable String file) {
        // Dosyayı oku; yoksa 404 dön.
        return StoryLoader.readImage(chapterId, file)
                .map(bytes -> ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).cacheControl(CACHE).body(bytes))
                .orElse(ResponseEntity.notFound().build());
    }
}
