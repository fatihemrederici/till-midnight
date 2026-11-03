package com.tensec.api.engine;

import java.util.List;

/** The outcome of one loop. disasterId is null when the player won. */
public record SimulationResult(boolean won, int endSecond, String disasterId, List<TimelineEvent> events) {}