package com.huseyin.couriertracking.entrance;

import com.huseyin.couriertracking.entity.CourierStorePresence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class StoreEntranceDetector {

    private final EntranceProperties properties;

    public PresenceChange detect(CourierStorePresence current, double distanceMeters, Instant time) {
        boolean insideNow = distanceMeters <= properties.radiusMeters();

        if (current == null) {
            return insideNow ? PresenceChange.ENTRANCE : PresenceChange.NONE;
        }

        if (current.isInside()) {
            return insideNow ? PresenceChange.NONE : PresenceChange.EXIT;
        }

        if (!insideNow) {
            return PresenceChange.NONE;
        }

        Duration sinceLastExit = Duration.between(current.getLastExitAt(), time);
        return sinceLastExit.compareTo(properties.reentryInterval()) >= 0
                ? PresenceChange.ENTRANCE
                : PresenceChange.REENTRY_TOO_SOON;
    }

}
