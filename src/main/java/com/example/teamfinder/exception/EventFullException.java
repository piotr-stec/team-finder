package com.example.teamfinder.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class EventFullException extends RuntimeException {

    public EventFullException() {
        super("Event is full. No more spots available.");
    }

    public EventFullException(String message) {
        super(message);
    }
}
