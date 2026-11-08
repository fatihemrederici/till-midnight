// Test araçları.
import { describe, expect, it } from "vitest";
// Test edilen seçimler.
import { ambienceFor, musicFor } from "./score";

describe("background score", () => {
  // Müzik eve gelene kadar gündüz, evde akşam, 22:00'den sonra gece olmalı.
  it("picks music by place and time", () => {
    expect(musicFor("08:14", "street")).toBe("day");
    expect(musicFor("14:00", "work")).toBe("day");
    expect(musicFor("12:30", "home")).toBe("evening");
    expect(musicFor("21:00", "home")).toBe("evening");
    expect(musicFor("22:00", "home")).toBe("night");
    expect(musicFor("23:55", "home")).toBe("night");
  });

  // Ortam sesi yere göre değişmeli, akşam evde yağmur başlamalı.
  it("picks ambience by place and evening rain", () => {
    expect(ambienceFor("street", "08:40")).toBe("street");
    expect(ambienceFor("work", "14:00")).toBe("work");
    expect(ambienceFor("home", "16:00")).toBe("home");
    expect(ambienceFor("home", "19:30")).toBe("home-rain");
  });
});
