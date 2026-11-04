// Bu dosyanın ait olduğu paket.
package com.tensec.api.web;

// Bölüm listesindeki bir satır.
public record LevelView(
        // Bölüm numarası: 1, 2, 3...
        int number,
        // Sahnenin adı. Bölüm açılırken kullanılır.
        String sceneId,
        // Başlık.
        String title,
        // Oynanabilir mi?
        boolean unlocked,
        // Geçildi mi?
        boolean completed,
        // En az dokunuşla çözüldü mü?
        boolean star) {}
