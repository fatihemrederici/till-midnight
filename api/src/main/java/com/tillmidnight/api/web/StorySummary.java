// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.web;

// Liste tipi.
import java.util.List;
// Anahtar-değer tipi.
import java.util.Map;

// Bölüm seçme ekranındaki bir satır. Oyuncu seçene kadar bölümün tamamı gönderilmez.
public record StorySummary(String id, Map<String, String> title, List<String> deaths) {}
