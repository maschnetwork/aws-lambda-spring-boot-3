package com.unicorn.store.service;

import com.unicorn.store.data.UnicornPublisher;
import com.unicorn.store.model.Unicorn;
import com.unicorn.store.model.UnicornEventType;
import org.springframework.stereotype.Service;

@Service
public class UnicornService {
    private final UnicornPublisher unicornPublisher;

    public UnicornService(UnicornPublisher unicornPublisher) {
        this.unicornPublisher = unicornPublisher;
    }

    public Unicorn createUnicorn(Unicorn unicorn) {
        unicornPublisher.publish(unicorn, UnicornEventType.UNICORN_CREATED);
        return unicorn;
    }
}
