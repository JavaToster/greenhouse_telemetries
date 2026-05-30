package com.example.greenhouse_telemetries.store;

public interface GenericStore<T, ID> {
    T findById(ID id);

    T save(T t);
}
