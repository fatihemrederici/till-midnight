// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.story;

// Dosya okurken çıkabilecek hata tipi.
import java.io.IOException;
// Dosyayı bayt bayt okumaya yarayan tip.
import java.io.InputStream;
// IOException'ı metot imzasına yazmak zorunda kalmamak için kullandığımız hata tipi.
import java.io.UncheckedIOException;
// Jackson ayarı: JSON'dan okuma özellikleri.
import tools.jackson.databind.DeserializationFeature;
// Jackson'ın JSON çeviricisi. Swift'teki JSONDecoder gibi.
import tools.jackson.databind.json.JsonMapper;

// JSON hikâyeleri Story nesnesine çeviren sınıf.
public final class StoryLoader {

    // Bilinmeyen alan varsa hata ver: AI ya da biz yanlış yazarsak sessizce geçmesin.
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    // Bu sınıftan nesne üretilemez, çünkü tüm metotlar static.
    private StoryLoader() {}

    // JSON metnini Story nesnesine çevirir.
    public static Story parse(String json) {
        // Metni çevir.
        return MAPPER.readValue(json, Story.class);
    }

    // Projeye paketlenmiş hikâye dosyasını okur.
    public static Story fromClasspath(String path) {
        // try (...): iş bitince dosyayı otomatik kapatır.
        try (InputStream in = StoryLoader.class.getClassLoader().getResourceAsStream(path)) {
            // Dosya yoksa anlaşılır bir hatayla dur.
            if (in == null) {
                throw new IllegalArgumentException("Story not found on classpath: " + path);
            }
            // Dosyayı çevir.
            return MAPPER.readValue(in, Story.class);
        } catch (IOException e) {
            // Hatayı metot imzasını değiştirmeyen tipe çevir.
            throw new UncheckedIOException(e);
        }
    }
}
