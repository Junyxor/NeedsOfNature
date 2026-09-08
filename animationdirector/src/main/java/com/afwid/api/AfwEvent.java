package com.afwid.api;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Minimal loader-agnostic array-backed event used by Animation Director's public hooks.
 * Keeps the Fabric-era register()/invoker() API shape without depending on Fabric API.
 */
public final class AfwEvent<T> {
    private final CopyOnWriteArrayList<T> listeners = new CopyOnWriteArrayList<>();
    private final Function<List<T>, T> invokerFactory;

    public AfwEvent(Function<List<T>, T> invokerFactory) {
        this.invokerFactory = Objects.requireNonNull(invokerFactory, "invokerFactory");
    }

    public void register(T listener) {
        this.listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public T invoker() {
        return this.invokerFactory.apply(List.copyOf(this.listeners));
    }
}
