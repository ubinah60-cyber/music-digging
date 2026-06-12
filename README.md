# Music Digging

음악 검색 및 추천 서비스 프로젝트

## 기술 스택

- Java 17
- Spring Boot 3
- MyBatis
- MySQL
- Thymeleaf
- JavaScript (fetch API)

## 실행 환경

- JDK 17
- MySQL 8.x

## DB 생성

CREATE DATABASE music_digging;

## 실행

./gradlew bootRun

또는

MusicDiggingApplication 실행

## 주요 기능

- 음악 검색
- 아티스트 정보 조회 (MusicBrainz API)
- 앨범 정보 조회 (MusicBrainz API)

## API

GET /api/music/list

GET /api/music/search?keyword=NewJeans

GET /api/music/artist?name=NewJeans

GET /api/music/albums?artistName=NewJeans