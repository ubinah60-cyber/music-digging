// 역할: 디깅 API의 잘못된 요청과 외부 음악 데이터 조회 실패를 HTTP 응답으로 변환한다.
package com.example.musicdigging.controller;

import com.example.musicdigging.digging.provider.MusicDataLookupException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = DiggingController.class)
public class DiggingExceptionHandler {

    // 잘못된 recordingId 등의 입력 오류를 400으로 반환한다.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidArgument(
            IllegalArgumentException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "code", "INVALID_REQUEST",
                        "message", exception.getMessage()
                ));
    }

    // 필수 요청 파라미터 누락을 400으로 반환한다.
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingParameter(
            MissingServletRequestParameterException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "code", "MISSING_PARAMETER",
                        "message",
                        "필수 요청 파라미터가 없습니다: "
                                + exception.getParameterName()
                ));
    }

    // MusicBrainz 조회 실패를 정상적인 '후보 없음'과 구분해 502로 반환한다.
    @ExceptionHandler(MusicDataLookupException.class)
    public ResponseEntity<Map<String, String>> handleMusicDataLookupFailure(
            MusicDataLookupException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(Map.of(
                        "code", "MUSIC_DATA_LOOKUP_FAILED",
                        "message", exception.getMessage()
                ));
    }
}