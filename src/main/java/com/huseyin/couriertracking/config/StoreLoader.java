package com.huseyin.couriertracking.config;

import com.huseyin.couriertracking.model.Store;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

@Slf4j
@Component
public class StoreLoader {

    private final List<Store> stores;

    public StoreLoader(ObjectMapper mapper, @Value("${courier.stores-file}") Resource storesFile) {
        this.stores = load(mapper, storesFile);
        log.info("{} stores loaded", stores.size());
    }

    private List<Store> load(ObjectMapper mapper, Resource file) {
        try (InputStream is = file.getInputStream()) {
            List<Store> loaded = mapper.readValue(is, new TypeReference<>() {
            });
            if (loaded.isEmpty()) {
                throw new IllegalStateException("No stores found in " + file);
            }
            return List.copyOf(loaded);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }

    public List<Store> findAll() {
        return stores;
    }
}
