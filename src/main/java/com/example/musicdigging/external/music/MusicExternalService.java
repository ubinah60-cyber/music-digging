package com.example.musicdigging.external.music;

import com.example.musicdigging.dto.CreditDto;
import com.example.musicdigging.dto.RecommendationDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MusicExternalService {

    // 1. 크레딧 조회 (Mock)
    public List<CreditDto> getCredits(String title, String artist) {

        List<CreditDto> credits = new ArrayList<>();

        // 예시 (Ditto 기준)
        if (title.equalsIgnoreCase("Ditto")) {
            credits.add(new CreditDto("250", "PRODUCER"));
            credits.add(new CreditDto("Ylva Dimberg", "COMPOSER"));
        } else {
            credits.add(new CreditDto("Unknown Producer", "PRODUCER"));
        }

        return credits;
    }

    // 2. 해당 인물의 다른 곡 조회 (Mock)
    public List<RecommendationDto> getRecommendationsByCredit(List<CreditDto> credits) {

        List<RecommendationDto> result = new ArrayList<>();

        for (CreditDto credit : credits) {

            if (credit.getName().equals("250")) {
                result.add(new RecommendationDto("Attention", "NewJeans", "같은 프로듀서"));
                result.add(new RecommendationDto("Hype Boy", "NewJeans", "같은 프로듀서"));
            }

            if (credit.getName().equals("Ylva Dimberg")) {
                result.add(new RecommendationDto("OMG", "NewJeans", "같은 작곡가"));
            }
        }

        return result;
    }
}