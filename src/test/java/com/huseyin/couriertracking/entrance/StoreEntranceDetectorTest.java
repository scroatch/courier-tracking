package com.huseyin.couriertracking.entrance;

import com.huseyin.couriertracking.entity.CourierStorePresence;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class StoreEntranceDetectorTest {
    private static final Instant T0 = Instant.parse("2026-10-31T12:00:00Z");

    private final StoreEntranceDetector detector = new StoreEntranceDetector(new EntranceProperties(100, Duration.ofSeconds(60)));

    @Test
    void noPresenceOutsideRadius_noChange() {
        assertThat(detector.detect(null, 150, T0)).isEqualTo(PresenceChange.NONE);
    }

    @Test
    void noPresenceInsideRadius_entrance() {
        assertThat(detector.detect(null, 99, T0)).isEqualTo(PresenceChange.ENTRANCE);
    }

    @Test
    void noPresenceExactlyOnRadius_entrance() {
        assertThat(detector.detect(null, 100, T0)).isEqualTo(PresenceChange.ENTRANCE);
    }

    @Test
    void staysInsideRadius_noChange() {
        CourierStorePresence presence = presenceInside();
        assertThat(detector.detect(presence, 90, T0)).isEqualTo(PresenceChange.NONE);
    }

    @Test
    void leavesRadius_exit() {
        CourierStorePresence presence = presenceInside();
        assertThat(detector.detect(presence, 120, T0)).isEqualTo(PresenceChange.EXIT);
    }

    @Test
    void staysOutsideRadius_noChange() {
        CourierStorePresence presence = presenceExitedAt(T0);
        assertThat(detector.detect(presence, 200, T0.plusSeconds(30))).isEqualTo(PresenceChange.NONE);
    }

    @Test
    void reentersWithinInterval_reentryTooSoon() {
        CourierStorePresence presence = presenceExitedAt(T0);
        assertThat(detector.detect(presence, 90, T0.plusSeconds(40))).isEqualTo(PresenceChange.REENTRY_TOO_SOON);
    }

    @Test
    void reentersExactlyAtInterval_entrance() {
        CourierStorePresence presence = presenceExitedAt(T0);
        assertThat(detector.detect(presence, 70, T0.plus(Duration.ofSeconds(60)))).isEqualTo(PresenceChange.ENTRANCE);
    }

    @Test
    void reentersOneSecondBeforeInterval_reentryTooSoon() {
        CourierStorePresence presence = presenceExitedAt(T0);
        assertThat(detector.detect(presence, 70, T0.plus(Duration.ofSeconds(59)))).isEqualTo(PresenceChange.REENTRY_TOO_SOON);
    }

    private CourierStorePresence presenceExitedAt(Instant exitTime) {
        CourierStorePresence presence = new CourierStorePresence("c1", "Ataşehir");
        presence.exit(exitTime);
        return presence;
    }

    private CourierStorePresence presenceInside() {
        CourierStorePresence presence = new CourierStorePresence("c1", "Ataşehir");
        presence.enter();
        return presence;
    }
}