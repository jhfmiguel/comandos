package com.comandos.security.api;

public record CurrentActor(
    Long accountId,
    String login,
    boolean authenticated
) {
    public static CurrentActor anonymous() {
        return new CurrentActor(null, null, false);
    }
}
