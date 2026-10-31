package com.huseyin.couriertracking.entrance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class StoreEntranceLogListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEntrance(CourierEnteredStoreEvent event){
        log.info("Courier {} entered {} at {}", event.courierId(), event.storeName(), event.enteredAt());
    }
}
