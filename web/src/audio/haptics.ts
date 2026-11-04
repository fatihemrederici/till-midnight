// Telefonu titretir. Desteklemeyen cihazlarda hiçbir şey yapmaz (iPhone Safari gibi).
export function vibrate(pattern: number | number[]): void {
  try {
    // Destek varsa titret.
    navigator.vibrate?.(pattern);
  } catch {
    // Titreşim olmadan da oyun çalışır.
  }
}
