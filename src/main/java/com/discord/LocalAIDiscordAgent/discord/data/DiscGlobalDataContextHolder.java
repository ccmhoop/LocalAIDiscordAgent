package com.discord.LocalAIDiscordAgent.discord.data;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

public final class DiscGlobalDataContextHolder {

    private DiscGlobalDataContextHolder() {
    }

    public static final String KEY = DiscGlobalDataContextHolder.class.getName() + ".discGlobalData";

    public static Context put(Context context, DiscGlobalData data) {
        return context.put(KEY, data);
    }

    public static Mono<DiscGlobalData> get() {
        return Mono.deferContextual(ctx -> {
            if (!ctx.hasKey(KEY)) {
                return Mono.error(new IllegalStateException("DiscGlobalData not found in Reactor Context"));
            }
            return Mono.just(ctx.get(KEY));
        });
    }
}