// Bu dosyanın ait olduğu paket.
package com.tensec.api.scene;

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

// JSON sahnelerini Scene nesnesine çeviren sınıf. final: bu sınıftan miras alınamaz.
public final class SceneLoader {

    // Tek bir çevirici oluşturup hepsinde onu kullanıyoruz. static final: sınıfa ait ve sabit.
    private static final JsonMapper MAPPER = JsonMapper.builder()
            // Bilinmeyen alan varsa hata ver. Böylece "title" yerine "titel" yazılırsa sessizce geçmez.
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            // Ayarlarla çeviriciyi oluştur.
            .build();

    // private constructor: bu sınıftan nesne üretilemez, çünkü tüm metotlar static.
    private SceneLoader() {}

    // JSON metnini alıp Scene nesnesi döndürür.
    public static Scene parse(String json) {
        // Metni Scene tipine çevir.
        return MAPPER.readValue(json, Scene.class);
    }

    // Projeye paketlenmiş bir JSON dosyasını okuyup Scene nesnesi döndürür.
    public static Scene fromClasspath(String path) {
        // try (...): iş bitince dosyayı otomatik kapatır, hata olsa bile.
        try (InputStream in = SceneLoader.class.getClassLoader().getResourceAsStream(path)) {
            // Dosya bulunamazsa in null gelir.
            if (in == null) {
                // Anlaşılır bir hata mesajıyla dur.
                throw new IllegalArgumentException("Scene not found on classpath: " + path);
            }
            // Dosyanın içeriğini Scene tipine çevir.
            return MAPPER.readValue(in, Scene.class);
        // Dosya okunurken hata çıkarsa buraya düşer.
        } catch (IOException e) {
            // Hatayı metot imzasını değiştirmeyen tipe çevirip tekrar fırlat.
            throw new UncheckedIOException(e);
        }
    }
}
