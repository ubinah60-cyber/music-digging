package com.example.musicdigging.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreditDto {

    private String name;
    private String role; // COMPOSER / PRODUCER
}