// Bu dosyanın ait olduğu paket.
package com.tensec.api.engine;

// Oyuncunun tek bir dokunuşu: hangi nesne, kaçıncı saniye.
public record Touch(String object, int second) {}
