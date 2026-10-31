package com.huseyin.couriertracking.config;

import com.huseyin.couriertracking.model.Store;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.io.UncheckedIOException;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.within;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class StoreLoaderTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void shouldLoadAllStoresFromJson() {
        var file = new ClassPathResource("stores.json");

        StoreLoader loader = new StoreLoader(mapper, file);
        List<Store> stores = loader.findAll();

        assertThat(stores).hasSize(5);
        Store ortakoy = stores.get(3);
        assertThat(ortakoy.name()).isEqualTo("Ortaköy MMM Migros");
        assertThat(ortakoy.lat()).isCloseTo(41.055783, within(1e-7));
        assertThat(ortakoy.lng()).isCloseTo(29.0210292, within(1e-7));
    }

    @Test
    void shouldFailFastWhenFileIsMissing() {
        var missing = new ClassPathResource("findMe.json");

        assertThatThrownBy(() -> new StoreLoader(mapper, missing))
                .isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void shouldFailFastWhenStoreNamesAreDuplicated() {
        var duplicated = new ClassPathResource("stores-duplicate.json");

        assertThatThrownBy(() -> new StoreLoader(mapper, duplicated))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ataşehir MMM Migros");
    }
}
