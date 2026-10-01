> 이 문서는 1차 개선의 검증 기록입니다. 최신 공동명의·앨범 분산 보정 결과는 [아티스트 곡 자동완성 정렬 보정](artist-autocomplete-ranking.md)을 참고하세요.

# MusicBrainz 자동완성 개선 검증

검증일: 2026-10-01 (Asia/Seoul). 브랜치: feature/digger-ui. 최종 실행: http://localhost:8080/, local,musicbrainz 프로필.

## 한 곡만 표시되던 원인

수정 전 코드로 IntelliJ 비중단 로그포인트와 실제 API/Chrome 화면을 확인했다.

| 단계 | 칸예 검색에서 관측한 곡 수 |
| --- | ---: |
| MusicBrainz arid 검색 (limit=15) | 15 |
| 백엔드 아티스트 ID 필터 통과 | 15 |
| /api/music/autocomplete 응답 songs | 15 |
| React DOM의 곡 항목 | 1 |

직접적인 표시 제한은 frontend/src/App.jsx의 곡 그룹 limit: 1과 slice(0, group.limit)였다. 백엔드에는 이미 arid 기반 조회가 있었으나 15개 검색 결과의 원래 순서를 사용하고, 주 아티스트 우선·중복 정리·버전 분산 처리가 없었다. 아티스트 이름 일치 확인도 이름·별칭보다 검색 score에 의존했다.

사용자가 보고한 첫 곡은 Digital Girl (remix)이지만 이번 수정 전 재현에서는 Last Name이 첫 곡이었다. 검색 인덱스의 동점 후보 순서는 변할 수 있으므로 과거의 곡명을 재현했다고 주장하지 않는다. 표시 수 1개는 실제 DOM에서 확인했다. MusicBrainz의 칸예 아티스트 직접 검색은 0개였고 일반 한글 로마자 변환 kanye 검색에서 Ye의 Kanye 별칭을 찾았다.

## 구현

- 이름·sort-name·별칭을 비교하고, 신뢰할 수 있는 아티스트의 MBID로 녹음 100개를 조회한다. 한글 별칭으로 직접 찾은 경우도 같은 MBID 경로를 사용한다.
- 공식 발매 검색을 우선하고, 관련 후보가 5개 미만이면 일반 arid 검색을 한 번만 보충한다. 다른 아티스트나 제목 결과로 목록을 채우지 않는다.
- 주 아티스트 크레딧, 공식 발매 근거, 일반 녹음/버전, 조회된 공식 발매 수를 기준으로 정렬한다. 제목·버전 설명·크레딧·길이가 같은 녹음은 정리하고 실제 MBID를 보존한다.
- 서로 다른 곡을 먼저 보여 주며 동일 제목의 버전은 최대 2개, 전체는 최대 8곡이다. 적은 후보를 억지로 5곡까지 채우지 않는다. 리믹스·라이브 설명을 가진 녹음은 별도 후보로 남긴다.
- 정확한 이름·별칭 일치 아티스트가 유일하면 아티스트 검색을 우선한다. 동명이인/동명 제목이 있는 경우 곡 제목 일치와 발매 수가 많은 정확한 앨범 일치를 함께 판단한다.
- 아티스트 disambiguation은 표시하지 않고 아티스트 검색에 무관한 앨범은 숨긴다. 특정 아티스트·곡·MBID 목록은 하드코딩하지 않았다.
- 기존 1,100ms 요청 제어를 재사용한다. 성공 응답 원본은 최대 256개/10분 캐시, 동일 URL의 동시 요청은 한 번만 수행한다. 실패는 캐시하지 않는다.
- React는 450ms debounce와 이전 fetch 취소/응답 순서 검사, 최대 64개/5분 캐시, 한글 IME 조합 중 조회 보류를 사용한다. 곡 8개를 기존 스크롤 목록에 렌더링한다.
- Apple 전용 클라이언트·검색 서비스·Recording 변환 서비스·DTO·컨트롤러 경로·관련 테스트·토큰 안내를 제거했다.

## 실제 API와 React 결과

모두 실제 MusicBrainz 호출로 조회했으며 아래 곡 목록은 Chrome에서 렌더링한 목록과 일치한다.

| 검색어 | HTTP | focus | 아티스트 | 곡 | 앨범 | 화면 첫 그룹 |
| --- | ---: | --- | ---: | ---: | ---: | --- |
| 칸예 | 200 | artist | 1 | 8 | 0 | 아티스트 |
| Kanye | 200 | artist | 1 | 8 | 0 | 아티스트 |
| Runaway | 200 | song | 2 | 8 | 3 | 곡 |
| Graduation | 200 | album | 1 | 8 | 3 | 앨범 |

### 칸예

아티스트: Ye.

곡 목록(표시 순서):

| 곡 | 아티스트 / 버전 | Recording MBID |
| --- | --- | --- |
| Bad News | Kanye West | 1818fc80-7c01-4dda-89e1-a24851ad1838 |
| On God | Kanye West | 7f45f9cc-30f5-4d94-810b-4ca2e2f2d966 |
| THIS A MUST | Ye | b697e162-d4f7-4bbb-99b2-1f13b4dc5c08 |
| Mean To Say | Kanye West & Malik Yusef feat. Vaughn Anthony | 715637fd-a553-4aee-bfad-9822893bb733 |
| Da Slumz | Kanye West & Malik Yusef feat. Raheem DeVaughn, Kumasi & Bun B | 7ae5b724-5707-4804-98dc-0f7e8d8290b6 |
| Elevated (So High) | Kanye West & Malik Yusef | 998c8375-9ea6-4e61-bd16-d21e80fcbf9a |
| Sexuality | Kanye West & Malik Yusef | aff5140f-d1d7-4aa8-93ad-48f428631b1e |
| Know God? | Kanye West & Malik Yusef feat. J. Ivy, Red Storm & Fatin Dantzler | b668617e-f5fe-4f30-be5a-818cf7686e00 |

앨범: 없음.

### Kanye

아티스트: Ye.

곡 목록(표시 순서):

| 곡 | 아티스트 / 버전 | Recording MBID |
| --- | --- | --- |
| Bad News | Kanye West | 1818fc80-7c01-4dda-89e1-a24851ad1838 |
| On God | Kanye West | 7f45f9cc-30f5-4d94-810b-4ca2e2f2d966 |
| THIS A MUST | Ye | b697e162-d4f7-4bbb-99b2-1f13b4dc5c08 |
| Mean To Say | Kanye West & Malik Yusef feat. Vaughn Anthony | 715637fd-a553-4aee-bfad-9822893bb733 |
| Da Slumz | Kanye West & Malik Yusef feat. Raheem DeVaughn, Kumasi & Bun B | 7ae5b724-5707-4804-98dc-0f7e8d8290b6 |
| Elevated (So High) | Kanye West & Malik Yusef | 998c8375-9ea6-4e61-bd16-d21e80fcbf9a |
| Sexuality | Kanye West & Malik Yusef | aff5140f-d1d7-4aa8-93ad-48f428631b1e |
| Know God? | Kanye West & Malik Yusef feat. J. Ivy, Red Storm & Fatin Dantzler | b668617e-f5fe-4f30-be5a-818cf7686e00 |

앨범: 없음.

### Runaway

아티스트: Runaway, Runaway.

곡 목록(표시 순서):

| 곡 | 아티스트 / 버전 | Recording MBID |
| --- | --- | --- |
| Runaway | 10cc | 4c436157-c034-4e57-87c4-770bc3160609 |
| Runaway | Sword | afd2fed5-3637-4bce-9cc3-dccb19361caf |
| Runaway | The Broken Homes | b62af859-cc02-473a-b4c0-3080e5cede14 |
| Runaway | Ultima Thule | 7dda9d55-5f06-4a4b-8f91-dbdb6f238700 |
| Runaway | Deee‐Lite | 21695a14-20fd-498a-b8f0-90e4f3822202 |
| Runaway | Nina Sky | 441adce8-f42a-453d-ac0b-a5add583dfa9 |
| Runaway | Charlie | 4aed7a5a-588f-4f71-b7a1-120537d80aaa |
| Runaway | Minus 8 feat. Billie | 3a71e922-be20-40ad-b74a-5eaadcd0062c |

앨범: Runaway — Bill Champlin · 1981; Runaway — Dakota · 1984; Runaway — Jerry Goldsmith · 1985.

### Graduation

아티스트: Graduation.

곡 목록(표시 순서):

| 곡 | 아티스트 / 버전 | Recording MBID |
| --- | --- | --- |
| graduation | ClariS | b7ff49e6-a3a1-4718-8356-6b99b97395e3 |
| Graduation | 三枝夕夏 IN db | ccec6828-fceb-4349-896e-22109b1679e4 |
| Graduation | Andrea Morricone | 2177cb3b-1c22-47a0-8ac9-9e2632fa788d |
| GRADUATION | JeanJass | 2bd98957-92d6-4f33-acac-2745293a678d |
| Graduation | Only Real | a58cf3c4-2fb6-4490-aa86-7114752c356a |
| Graduation | Stephen Endelman | 03dec483-672d-4538-8354-061c923cd7aa |
| Graduation | Wasted Time | 06ae3386-6489-4289-bf9b-2fa36c61c027 |
| Graduation | Freddie T and the People | 101bcd8c-8ace-4bd7-9dad-43f685191fb8 |

앨범: Graduation — Kanye West · 2007; Graduation — Sunglasses Kid · 2017; Graduation — 中村由真 · 1989.

## 검증 결과

- ./gradlew.bat test bootJar: 성공. 72개 테스트, 실패 0개, 오류 0개. 아티스트 ID 조회, 한글 별칭/로마자 fallback, 제목 충돌, 중복과 버전 보존, 관련성 필터, 캐시 만료·동시 요청·실패 재시도·요청 간격을 검증했다.
- frontend의 npm.cmd run build: 성공. 빌드된 정적 파일로 / 화면을 확인했다.
- Chrome에서 칸예와 Kanye의 8곡 렌더링, Runaway의 곡 그룹 우선, Graduation의 앨범 그룹 우선과 Kanye West 앨범을 확인했다. formerly 문자열은 표시되지 않았다.
- 브라우저에서 칸예를 다시 입력했을 때 자동완성 API 추가 요청 0회로 캐시를 확인했다.
- 통제된 브라우저 fetch로 빠른 입력 debounce, IME 조합 보류/종료 후 검색, Old 요청 취소 뒤 New 결과 유지도 확인했다. 이 입력 제어 검증은 외부 API 데이터 검증과 별도로 수행했다.
- 실제 곡 Bad News 선택 후 /api/digging?recordingId=1818fc80-7c01-4dda-89e1-a24851ad1838 호출: SUCCESS, 후보 35곡. 브라우저에도 디깅 후보 35곡 안내가 표시됐다.
- git diff --check: 통과. 디버거의 에이전트 로그포인트와 세션은 정리했다. 수정한 애플리케이션을 8080에서 다시 실행했다.

## 수정 파일

| 파일 | 변경 |
| --- | --- |
| frontend/src/App.jsx | 8곡 표시, 캐시·IME·취소 제어, Apple 변환 흐름 제거 |
| frontend/README.md | MusicBrainz 검색/디깅 실행 안내 |
| src/main/java/com/example/musicdigging/service/MusicAutocompleteService.java | 이름·별칭 매칭, 아티스트 ID 조회, 검색 의도와 결과 선별 |
| src/main/java/com/example/musicdigging/service/MusicRecordingSelector.java | 주 아티스트 순위, 중복 정리, 버전 분산 |
| src/main/java/com/example/musicdigging/digging/provider/musicbrainz/MusicBrainzApiClient.java | 기존 간격 제어에 캐시/동시 요청 중복 방지 추가 |
| src/main/java/com/example/musicdigging/controller/MusicAutocompleteController.java | Apple Recording 변환 경로/의존성 제거 |
| src/test/java/com/example/musicdigging/service/MusicAutocompleteServiceTest.java | 검색 의도·별칭·관련성 회귀 테스트 |
| src/test/java/com/example/musicdigging/service/MusicRecordingSelectorTest.java | 중복·버전·주 아티스트 테스트 |
| src/test/java/com/example/musicdigging/digging/provider/musicbrainz/MusicBrainzApiClientCacheTest.java | 캐시·간격·실패·동시 요청 테스트 |
| README.md | 검색·실행·요청 제한·비용/이용 조건 안내 |
| src/main/resources/static/digger/index.html, assets/ | React 빌드 산출물 갱신 |
| docs/search-autocomplete-validation.md | 이 검증 기록 |

기존 작업 트리의 다른 변경, 다크 테마 스타일, ui/ 이미지를 보존했다. 이번 작업에서 frontend/src/styles.css는 수정하지 않았다. 커밋과 푸시는 하지 않았다.

제거한 파일: AppleMusicApiClient.java, AppleMusicSearchService.java, AppleMusicRecordingResolver.java, RecordingResolution.java, AppleMusicSearchServiceTest.java, AppleMusicRecordingResolverTest.java. 이 파일들은 작업 시작 시 커밋되지 않은 파일이었다.

## 비용 및 한계

새 외부 API/서비스, 유료 기능, Apple 유료 가입과 토큰 발급을 도입하지 않았다. [MusicBrainz API](https://musicbrainz.org/doc/MusicBrainz_API)는 비상업적 사용 무료, 기본 [IP당 초당 1회](https://musicbrainz.org/doc/MusicBrainz_API/Rate_Limiting) 제한이다. 상업적 운영은 별도 플랜/협의가 필요하며 현재 공개 제품의 [Bronze는 월 $100부터](https://metabrainz.org/supporters/account-type)다. [핵심 데이터 CC0와 부가 데이터 CC BY-NC-SA 3.0](https://musicbrainz.org/doc/About/Data_License)은 공개 API 상업 운영 조건과 별개다.

이 결과는 제한된 MusicBrainz 후보 내에서 관련성과 다양성을 개선한 목록이다. 전체 음반 목록이나 Apple Music 인기곡 순서를 재현하지 않는다. 원본 검색의 동점 후보 순서가 변하면 캐시 만료 이후 목록도 바뀔 수 있다. 한글 별칭/로마자 표기 부족으로 일부 아티스트를 찾지 못할 수 있다.

브라우저 fetch 취소는 이미 진행 중인 서버 외부 호출을 중단하지 않는다. 간격 제어/캐시는 프로세스 단위이므로 같은 외부 IP를 공유하는 여러 서버를 운영할 때는 중앙 요청 제어가 필요하다.
