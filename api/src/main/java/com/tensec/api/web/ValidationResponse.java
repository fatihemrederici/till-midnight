// Bu dosyanın ait olduğu paket.
package com.tensec.api.web;

// Çözücünün raporu.
import com.tensec.api.solver.SolveResult;
// Liste tipi.
import java.util.List;

// POST /internal/validate cevabı.
public record ValidationResponse(
        // Sahne kullanılabilir mi?
        boolean valid,
        // Bulunan hatalar. Geçerliyse boş.
        List<String> errors,
        // Denenen kombinasyon sayısı. Çözücü çalışmadıysa null.
        Integer combinationsTried,
        // En az dokunuş sayısı. Çözücü çalışmadıysa null.
        Integer minTouches,
        // Gereksiz dokunuşsuz çözüm sayısı. Çözücü çalışmadıysa null.
        Integer solutionCount,
        // Zorluk puanı. Çözücü çalışmadıysa null.
        Integer difficulty) {

    // Çözücüye kadar gelemeyen sahne için cevap.
    public static ValidationResponse invalid(List<String> errors) {
        // Sayılar boş kalır.
        return new ValidationResponse(false, errors, null, null, null, null);
    }

    // Çözücünün çalıştığı sahne için cevap.
    public static ValidationResponse solved(List<String> errors, SolveResult result) {
        // Hata yoksa sahne geçerlidir.
        return new ValidationResponse(
                errors.isEmpty(),
                errors,
                result.combinationsTried(),
                result.minTouches(),
                result.minimalSolutions().size(),
                result.difficulty());
    }
}