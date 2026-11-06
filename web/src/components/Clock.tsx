// Havaalanı tabelası gibi saat: Değişen her rakam yukarıdan çevrilerek gelir.
export function Clock({ value, lie }: { value: string; lie: boolean }) {
  return (
    <div className="time" data-lie={lie} aria-label={value}>
      {[...value].map((char, i) =>
        char === ":" ? (
          <span className="colon" key={i} aria-hidden="true">
            :
          </span>
        ) : (
          // key rakamı da içerir: Sadece değişen rakam yeniden çizilir ve çevrilir.
          <span className="digit" key={`${i}-${char}`} aria-hidden="true">
            {char}
          </span>
        ),
      )}
    </div>
  );
}
