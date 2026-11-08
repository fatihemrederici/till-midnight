// Test araçları.
import { describe, expect, it } from "vitest";
// Test edilen seçimler.
import { ambienceFor, musicFor } from "./score";

describe("background score", () => {
  // Müzik gün ilerledikçe değişmeli ve son saatte kendi parçasına geçmeli.
  it("picks music by time of day", () => {
    expect(musicFor("08:14")).toBe("morning");
    expect(musicFor("13:50")).toBe("day");
    expect(musicFor("17:40")).toBe("day");
    expect(musicFor("21:00")).toBe("night");
    expect(musicFor("23:55")).toBe("last-hour");
  });

  // Ortam sesi yere göre değişmeli, akşam evde yağmur başlamalı.
  it("picks ambience by place and evening rain", () => {
    expect(ambienceFor("street", "08:40")).toBe("street");
    expect(ambienceFor("work", "14:00")).toBe("work");
    expect(ambienceFor("home", "16:00")).toBe("home");
    expect(ambienceFor("home", "19:30")).toBe("home-rain");
  });
});
