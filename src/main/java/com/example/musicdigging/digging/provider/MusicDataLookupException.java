package com.example.musicdigging.digging.provider;

public class MusicDataLookupException extends RuntimeException {

    public MusicDataLookupException(String message) {

        super(message);
    }

    public MusicDataLookupException(String message, Throwable cause) {

        super(message, cause);
    }
}