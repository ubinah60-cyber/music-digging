package com.example.musicdigging.dto;

import lombok.Data;

@Data
public class TrackDto {

    private Integer trackNumber;

    private String title;

    private String length;

    private String recordingId;
}