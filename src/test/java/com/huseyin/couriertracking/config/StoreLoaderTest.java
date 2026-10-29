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
        Store atasehir = stores.get(3);
        assertThat(atasehir.name()).isEqualTo("Ortaköy MMM Migros");
        assertThat(atasehir.lat()).isCloseTo(41.055783, within(1e-7));
        assertThat(atasehir.lng()).isCloseTo(29.0210292, within(1e-7));
    }

    @Test
    void shouldFailFastWhenFileIsMissing() {
        var missing = new ClassPathResource("findMe.json");

        assertThatThrownBy(() -> new StoreLoader(mapper, missing))
                .isInstanceOf(UncheckedIOException.class);
    }
}
