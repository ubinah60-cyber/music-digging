package com.example.musicdigging.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Music {

    private Long id;
    private String title;
    private String artist;
    private String album;
    private String genre;
    private Integer releaseYear;
    private LocalDateTime createdAt;
}