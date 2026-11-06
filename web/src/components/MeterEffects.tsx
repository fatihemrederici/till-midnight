// Göz kapakları: Ortada oval bir açıklık kalır, kapanınca incelip kaybolur.
function Lids() {
  return <i className="lid" />;
}

// Sağlık düşükken gözler aralıklarla kapanıp açılır.
export function Eyelids({ level }: { level: number }) {
  // Etki yoksa çizme.
  if (level === 0) return null;
  return (
    <div className="eyelids loop" data-level={level} aria-hidden="true">
      <Lids />
    </div>
  );
}

// Sağlık düştüğü anda gözler bir kez kapanıp açılır. deep: Neredeyse tamamen kapanır.
export function Blink({ count, deep }: { count: number; deep: boolean }) {
  // Daha düşüş olmadı.
  if (count === 0) return null;
  // key değişince animasyon baştan oynar.
  return (
    <div className="eyelids once" key={count} data-deep={deep} aria-hidden="true">
      <Lids />
    </div>
  );
}
