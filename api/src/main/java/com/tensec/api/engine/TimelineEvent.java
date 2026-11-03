package com.tensec.api.engine;

/** One state change: at this second, this object moved from one state to another because of this rule. */
public record TimelineEvent(int second, String object, String from, String to, String ruleId, String caption) {}