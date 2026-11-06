// Tipler.
import type { Choice, Story } from "../game/types";

// Düğmenin altındaki küçük satır: Kilitliyse ne gerektiği, şartı varsa kaça mal olduğu.
export function ChoiceCost({
  story,
  choice,
  locked,
}: {
  story: Story;
  choice: Choice;
  locked: boolean;
}) {
  // Şartı olmayan seçimde gösterilecek bir şey yok.
  const needs = Object.entries(choice.requires ?? {});
  if (needs.length === 0) return null;
  // Göstergenin simgesi.
  const emoji = (id: string) => story.meters.find((meter) => meter.id === id)?.emoji;
  // Bedel: Kilitli değilken düşecek değer.
  const cost = (id: string) => choice.effects?.[id];
  return (
    <span className="cost" data-locked={locked}>
      {locked ? "🔒 " : ""}
      {needs.map(([id, need]) => (
        <span key={id}>
          {emoji(id)} {cost(id) ?? need}
        </span>
      ))}
    </span>
  );
}
