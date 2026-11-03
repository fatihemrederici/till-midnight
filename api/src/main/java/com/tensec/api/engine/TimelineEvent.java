// Bu dosyanın ait olduğu paket.
package com.tensec.api.engine;

// Olan tek bir şey: şu saniyede şu nesne, şu kural yüzünden bir durumdan diğerine geçti.
// Etkisiz dokunuşta from ve to aynı olur, ruleId de null olur.
// Web tarafı animasyonu bu olayların listesine göre oynatacak.
public record TimelineEvent(int second, String object, String from, String to, String ruleId, String caption) {}
