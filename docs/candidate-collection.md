# 샘플 기반 다중 경로 후보 수집

시작곡의 크레딧, 가수, 장르+발매연도를 각각 조회하여 후보를 합친다.
크레딧 후보가 충분해도 다른 경로를 함께 실행한다. 점수 계산과 최종 추천 순위는 아직 구현하지 않는다.

## 실행 및 확인

기존 local 프로필과 DB/환경변수 설정으로 앱을 실행한 뒤 다음 주소를 호출한다.

`GET /api/digging?recordingId=track-001`

| 시작곡 | 예상 결과 |
| --- | --- |
| track-001 | SUCCESS. track-002, track-005, track-004, track-003 총 4곡 |
| track-004 | 크레딧이 없어도 SUCCESS. 같은 가수의 track-001, track-002 |
| track-007 | 수집 정보가 없어 NO_CANDIDATES |
| missing | TRACK_NOT_FOUND |

track-002는 세 경로에서 발견되지만 한 번만 반환한다. 기존 `connections`는 공통 크레딧이며,
새 `reasons`는 전체 수집 근거다. 가수 또는 장르로만 연결되면 `connections`는 빈 목록이다.

```json
{
  "recordingId": "track-002",
  "title": "세 경로가 겹치는 곡",
  "connections": [{"personId":"person-001","personName":"작곡가 A","role":"COMPOSER"}],
  "reasons": [
    {"type":"CREDIT","value":"COMPOSER:person-001","fromYear":null,"toYear":null},
    {"type":"SAME_ARTIST","value":"artist-001","fromYear":null,"toYear":null},
    {"type":"GENRE_AND_YEAR","value":"hip hop","fromYear":2017,"toYear":2023}
  ]
}
```

현재 순서는 수집 순서이며 추천 점수 순서가 아니다. 시작곡 제외와 중복 통합은 recordingId 기준이다.
서로 다른 recordingId의 라이브/리마스터 등 버전 통합은 이번 범위에 포함하지 않는다.

## 소스 흐름

1. `DiggingService`가 `MusicDataProvider.findTrackById`로 시작곡을 조회한다.
2. 크레딧별 `findTracksByCredit` → `CreditCandidateGenerator`.
3. 중복 없는 가수 ID별 `findTracksByArtist` → `ArtistCandidateGenerator`.
4. 장르별 `findTracksByGenreAndYearRange` → `GenreYearCandidateGenerator`.
5. `CandidateMerger`가 시작곡을 제외하고 곡 ID별 후보와 근거를 통합한다.

가수는 표시 이름이 아닌 ID로 비교하며 협업곡의 여러 가수도 지원한다.
장르는 앞뒤 공백 제거와 소문자 변환 후 정확히 비교한다. 장르 동의어/인접 장르 매핑은 아직 없다.
연도는 recording의 최초 발매연도로 정의하며 미상은 null, 가수/장르 미상은 빈 목록을 사용한다.
연도+장르 경로는 시작곡 연도 ±3년(양 끝 포함)이다. 연도/장르가 없으면 그 경로만 건너뛴다.
연도 범위는 크레딧/가수 경로에는 적용하지 않는다.

신규 가수/장르 조회는 호출당 최대 30건이며 시작곡도 조회 건수에 포함될 수 있다.
크레딧 조회는 기존 인터페이스를 유지한다. 실제 외부 연동 시 크레딧 조회 상한, 페이지 수와
전체 요청 예산도 별도로 정해야 한다. 한 경로의 조회 예외는 현재 요청 전체로 전파한다.
`NO_CREDITS` enum은 호환 목적으로 남겨두지만 다중 경로 서비스에서는 반환하지 않는다.
세 인자 `DigTrack`/`DigCandidate` 생성자는 기존 크레딧 테스트와 호출부의 호환을 위해 유지한다.

## 테스트

Windows PowerShell:

```powershell
.\gradlew.bat test --tests "com.example.musicdigging.digging.*"
```

Linux/macOS:

```sh
bash gradlew test --tests 'com.example.musicdigging.digging.*'
```

샘플 Provider → Service 통합 테스트와 MockMvc API 테스트는 외부 API/DB 없이 실행한다.
전체 Spring 컨텍스트 테스트는 기존 DB 및 외부 서비스 환경변수 설정이 별도로 필요하다.

## 다음 MusicBrainz 연동 작업

- 실제 `MusicDataProvider` 구현체에서 기존 두 메서드와 신규 가수/장르+연도 메서드를 구현한다.
- 조회 응답을 가수 목록, 최초 발매연도, 정규화된 장르가 포함된 `DigTrack`으로 변환한다.
- 실제 API의 지원 필드와 검색 방식을 확인하고 누락 데이터 및 호출 제한을 처리한다.
- 샘플/실제 Provider 선택 프로필을 정리한다.

이번 변경에는 MusicBrainz HTTP 호출, 파서 추가, 프로필 변경을 포함하지 않는다.
