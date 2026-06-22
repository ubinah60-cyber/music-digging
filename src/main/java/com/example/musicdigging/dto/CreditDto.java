package com.example.musicdigging.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreditDto {

    private String name;
    private String role; // COMPOSER / PRODUCER
}