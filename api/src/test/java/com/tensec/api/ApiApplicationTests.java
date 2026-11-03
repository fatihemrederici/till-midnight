// Bu dosyanın ait olduğu paket.
package com.tensec.api;

// Test etiketini içeri alıyoruz.
import org.junit.jupiter.api.Test;
// Uygulamayı test içinde başlatan etiketi içeri alıyoruz.
import org.springframework.boot.test.context.SpringBootTest;

// Bu etiket test sırasında tüm Spring uygulamasını gerçekten başlatır.
@SpringBootTest
// Test sınıfı.
class ApiApplicationTests {

    // Bu metot bir test.
    @Test
    // İçi boş: uygulama hatasız açılırsa test geçer.
    void contextLoads() {}
}
