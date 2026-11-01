package com.huseyin.couriertracking.entrance;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class StoreEntranceMetricsListenerTest {

    private static final Instant T0 = Instant.parse("2026-10-31T12:00:00Z");
    private static final String ATASEHIR = "Ataşehir MMM Migros";
    private static final String NOVADA = "Novada MMM Migros";

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final StoreEntranceMetricsListener listener = new StoreEntranceMetricsListener(registry);

    @Test
    void entrances_areCountedPerStore() {
        listener.onEntrance(new CourierEnteredStoreEvent("courier-1", ATASEHIR, T0));
        listener.onEntrance(new CourierEnteredStoreEvent("courier-2", ATASEHIR, T0));
        listener.onEntrance(new CourierEnteredStoreEvent("courier-1", NOVADA, T0));

        assertThat(entranceCount(ATASEHIR)).isEqualTo(2);
        assertThat(entranceCount(NOVADA)).isEqualTo(1);
    }

    private double entranceCount(String store) {
        return registry.get("courier.store.entrances").tag("store", store).counter().count();
    }
}