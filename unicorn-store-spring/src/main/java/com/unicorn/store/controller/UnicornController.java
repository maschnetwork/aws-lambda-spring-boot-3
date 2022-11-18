package com.unicorn.store.controller;

import com.unicorn.store.model.Unicorn;
import com.unicorn.store.service.UnicornService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@RestController
public class UnicornController {

    private final UnicornService unicornService;
    private static final Logger logger = LoggerFactory.getLogger(UnicornController.class);

    public UnicornController(UnicornService unicornService) {
        this.unicornService = unicornService;
    }

    @PostMapping("/unicorns")
    public ResponseEntity<Unicorn> createUnicorn(@RequestBody Unicorn unicorn) {
        try {
            var savedUnicorn = unicornService.createUnicorn(unicorn);
            return ResponseEntity.ok(savedUnicorn);
        } catch (Exception e) {
            String errorMsg = "Error creating unicorn";
            logger.error(errorMsg, e);
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, errorMsg, e);
        }
    }
}
