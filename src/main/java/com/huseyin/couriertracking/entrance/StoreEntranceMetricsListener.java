package com.huseyin.couriertracking.entrance;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class StoreEntranceMetricsListener {

    private final MeterRegistry meterRegistry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEntrance(CourierEnteredStoreEvent event) {
        meterRegistry.counter("courier.store.entrances", "store", event.storeName()).increment();
    }
}
