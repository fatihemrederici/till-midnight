package com.tensec.api.engine;

/** A player's touch: this object, at this second. */
public record Touch(String object, int second) {}