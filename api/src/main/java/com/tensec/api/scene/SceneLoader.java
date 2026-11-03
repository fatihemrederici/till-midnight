package com.tensec.api.scene;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

public final class SceneLoader {

    // Strict on purpose: a typo in a hand-written or LLM-written scene must fail, not be ignored.
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private SceneLoader() {}

    public static Scene parse(String json) {
        return MAPPER.readValue(json, Scene.class);
    }

    public static Scene fromClasspath(String path) {
        try (InputStream in = SceneLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("Scene not found on classpath: " + path);
            }
            return MAPPER.readValue(in, Scene.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
