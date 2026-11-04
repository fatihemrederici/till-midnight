// Bu dosyanın ait olduğu paket. Klasör yolu (com/tillmidnight/api) ile aynı olmak zorunda.
package com.tillmidnight.api;

// Uygulamayı başlatan Spring sınıfını içeri alıyoruz.
import org.springframework.boot.SpringApplication;
// Otomatik ayarları açan etiketi içeri alıyoruz.
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Bu etiket Spring'e "ana sınıf bu, gerekli ayarları kendin yap" der.
@SpringBootApplication
// Uygulamanın ana sınıfı.
public class ApiApplication {

    // Java programı her zaman main metodundan başlar.
    public static void main(String[] args) {
        // Spring Boot'u ve web sunucusunu başlatır.
        SpringApplication.run(ApiApplication.class, args);
    }
}
