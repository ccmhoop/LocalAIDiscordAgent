package com.discord.LocalAIDiscordAgent.discord.data;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

public final class DiscGlobalDataContextHolder {

    public static final String KEY = DiscGlobalDataContextHolder.class.getName() + ".discGlobalData";

    private DiscGlobalDataContextHolder() {
    }

    public static Context put(Context context, DiscGlobalData data) {
        return context.put(KEY, data);
    }

    public static Mono<DiscGlobalData> get() {
        return Mono.deferContextual(ctx ->
                ctx.hasKey(KEY)
                        ? Mono.just(ctx.get(KEY))
                        : Mono.error(new IllegalStateException("DiscGlobalData not found in Reactor Context"))
        );
    }
}