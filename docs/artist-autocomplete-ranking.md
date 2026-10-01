# 아티스트 곡 자동완성 정렬 보정

2026-10-01, feature/digger-ui. 실제 MusicBrainz, local,musicbrainz 프로필. 최종 API/Chrome 검증 주소: http://localhost:8080/.

## 원인과 후보 수집 확인

기존 코드도 MusicBrainz 후보 100개를 정렬한 뒤 8개를 선택했다. 먼저 8개로 자르는 문제가 아니었다. 첫 artist-credit MBID만으로 주 아티스트를 판별하여 Kanye West & Malik Yusef를 단독 주 아티스트와 같은 등급으로 처리한 것이 직접적인 원인이었다. 같은 앨범의 곡을 분산하는 처리도 없었다.

수정 전 IntelliJ 로그포인트로 정렬 직전 후보 100개를 확인했다. 정확한 원곡 제목 Runaway 0개, Stronger 0개였다. 최종 보충 재현의 정렬 직전 후보는 241개(고유 Recording MBID 239개)였으며 Runaway 0개, Stronger 3개였다. 이 후보 전체를 정렬·중복 정리·앨범 분산한 후 8개를 선택했다. 검색 인덱스 동점 결과가 바뀔 수 있으므로 수치는 해당 재현의 스냅샷이다.

Runaway의 부재는 정렬이나 8개 제한 이전의 후보 수집 범위에서 발생했다. 기본 공식 녹음 첫 100개와 최대 3개 샘플 앨범에서 찾지 못했다. 전체 음반 목록이나 다음 페이지를 끝까지 조회하지 않는다. Stronger는 앨범 보충으로 선택 가능해졌고 최종 첫 번째 결과가 됐다.

후보 수집은 기본 arid 공식 녹음 검색 최대 100개, 일반 공식 앨범 메타데이터 최대 100개 조회, 단독 주 아티스트 앨범을 발매 수·최초 발매일·MBID 순으로 최대 3개 선택, 각 rgid의 녹음 최대 100개 보충으로 제한한다. 일반 후보는 최대 400개, 후보가 5개 미만일 때의 한 번의 추가 조회까지 포함해 최대 500개다. 기존 10분 캐시와 1,100ms 요청 간격을 재사용하며 입력마다 전체 목록을 조회하지 않는다. 최초 캐시 미스의 칸예 응답은 약 10.7초였고, 같은 MBID의 재검색에서는 보충 데이터를 재사용한다.

## 크레딧과 정렬 기준

이름 문자열 대신 artist-credit 안의 MBID와 각 크레딧의 joinphrase를 사용한다. joinphrase는 현재 아티스트 뒤의 연결 표현이다.

1. 검색 아티스트가 유일한 주 아티스트: 단독 크레딧 또는 검색 아티스트 feat./ft./featuring 다른 아티스트. 피처링 구간이 시작한 후 나오는 쉼표·& 연결도 피처링 구간으로 유지한다.
2. 공동 주 아티스트: 피처링 구간 이전에 서로 다른 MBID가 둘 이상 존재한다. 대상 아티스트가 첫 번째 또는 두 번째여도 공동 주 아티스트다. &·and·쉼표·알 수 없는 연결은 공동명의로 보수적으로 처리한다.
3. 검색 아티스트가 피처링 참여자: 다른 주 아티스트 뒤의 featuring 구간에서 대상 MBID가 나타난다.
4. 대상 MBID가 artist-credit에 없으면 제외한다. composer/producer 관계에만 나타나는 MBID나 표시 이름의 우연한 일치로 추가하지 않는다.

각 참여 순위 안에서는 공식 발매 근거 → 일반 스튜디오 녹음 → 확인된 공식 발매 수 → 반환된 전체 발매 수 → MBID 순으로 정렬한다. clean/explicit/album version은 스튜디오 녹음으로 취급하며 리믹스·라이브·데모 등은 다음으로 둔다. **Recording 검색 score는 사용하지 않는다.** 발매 수는 후보 데이터의 수록 정도를 나타내는 정렬 근거이며 재생 수나 인기 점수가 아니다. Artist 검색 score는 이름/별칭 매칭의 신뢰도 확인에만 사용한다.

각 참여 순위 안에서 동일 앨범 최대 2곡을 먼저 선택하고, 다른 앨범의 후보를 검토한 뒤 빈 자리가 있을 때만 앨범 제한을 완화한다. 따라서 단독 주 아티스트의 남은 곡이 공동 주 아티스트보다 먼저 올 수 있다. 국가·에디션을 넘어 같은 release-group MBID로 묶으며, 알려진 Compilation은 원래 앨범 분산에서 제외한다. 앨범 메타데이터가 없으면 이름으로 같은 앨범이라고 추측하지 않는다. 동일 곡의 여러 버전은 최대 2개이고 서로 다른 곡을 먼저 보여 준다.

## 실제 칸예/Kanye 결과와 선택 근거

두 검색 모두 Ye MBID 164f0d73-1234-4e2c-8743-d77bf2191051을 확인했고, 동일한 8곡을 반환했다. 아래 앨범 열은 해당 곡의 원래 아티스트 앨범이다. 곡이 다른 앨범/믹스테이프에도 수록된 경우에는 해당 release-group MBID들도 분산 검사에 반영한다.

| 순서 | 곡 | 주 아티스트/연결 표현 근거 | 앨범 | 확인된 공식 발매 수 / 전체 반환 발매 수 | 선택 이유 |
| --- | --- | --- | --- | --- | --- |
| 1 | Stronger | Ye MBID 단독 | Graduation | 25 / 38 | 단독 주 아티스트·스튜디오 녹음, 공식 발매 근거 우선 |
| 2 | Can’t Tell Me Nothing | Ye MBID 단독 | Graduation | 23 / 27 | 단독 주 아티스트·스튜디오 녹음, 공식 발매 근거 우선 |
| 3 | Heard ’Em Say | Ye MBID + feat. Adam Levine | Late Registration | 22 / 23 | Ye 단독 주 아티스트·외부 아티스트 피처링, 다른 앨범 자리 우선 |
| 4 | Gold Digger | Ye MBID + feat. Jamie Foxx | Late Registration | 19 / 27 | Ye 단독 주 아티스트·외부 아티스트 피처링, 다른 앨범 자리 우선 |
| 5 | All Falls Down | Ye MBID + feat. Syleena Johnson | The College Dropout | 19 / 24 | Ye 단독 주 아티스트·외부 아티스트 피처링, 다른 앨범 자리 우선 |
| 6 | Two Words | Ye MBID + feat. Mos Def, The Harlem Boys Choir | The College Dropout | 15 / 23 | Ye 단독 주 아티스트·외부 아티스트 피처링, 다른 앨범 자리 우선 |
| 7 | All Mine | Ye MBID 단독 | ye | 7 / 8 | 단독 주 아티스트·스튜디오 녹음, 앞선 앨범 2곡 제한으로 다른 앨범 우선 |
| 8 | Yikes | Ye MBID 단독 | ye | 7 / 7 | 단독 주 아티스트·스튜디오 녹음, 앞선 앨범 2곡 제한으로 다른 앨범 우선 |

Graduation, Late Registration, The College Dropout, ye에서 각각 2곡씩 선택됐다. 공동명의 Kanye West & Malik Yusef 곡은 CO_PRIMARY로 분류하여 다음 순위로 내렸다. 이번에는 PRIMARY 곡만으로 8개를 채워 공동명의 곡이 표시되지 않는다. 전체 후보 정렬에서 앞서 있던 Roses, Diamonds From Sierra Leone 등의 곡도 Late Registration의 2곡 우선 제한을 받는다.

Recording MBID:

| 곡 | MBID |
| --- | --- |
| Stronger | 8009d470-912b-42fa-8261-b88c488eb935 |
| Can’t Tell Me Nothing | 4efea515-d670-4715-b99c-71df72f980fa |
| Heard ’Em Say | 0ee6c11f-0952-4381-aa87-abc7446e5314 |
| Gold Digger | ca2d4efe-89e3-4e63-9f69-27a2ec6cbe96 |
| All Falls Down | 951bdee5-4718-4455-b435-d46de3833331 |
| Two Words | 69775f74-f723-4244-a3fe-4735f72f4545 |
| All Mine | 874e02c8-07b9-4c4e-9380-364764060350 |
| Yikes | 78af9f5c-4773-4aac-ab46-ed6b9e2cb513 |

## 검증

- ./gradlew.bat test: 78개 통과, 실패/오류 0개. 크레딧 MBID·joinphrase, 공동명의 순서, featuring 구간의 & 처리, 작곡/제작 참여만 있는 곡 제외, 국가별 에디션의 동일 앨범 묶음, 앨범 2곡 우선과 빈 자리 보충, 9번째 이후 후보도 고려, clean/explicit 처리, 녹음 검색 score 무시, 최대 3개 앨범 보충을 검증했다.
- npm.cmd run build: 성공. 기존 UI 소스/다크 테마/CSS는 이번 보정에서 수정하지 않았다.
- 실제 칸예, Kanye, Runaway, Graduation API 모두 HTTP 200. 칸예/Kanye에 동일 8곡, 무관한 앨범 없음. Runaway는 제목 일치 곡 그룹 우선, Graduation은 Kanye West 앨범 그룹 우선 유지.
- Chrome에서 위 8곡 렌더링과 재검색 추가 요청 0회를 확인했다. Stronger 선택 후 /api/digging?recordingId=8009d470-912b-42fa-8261-b88c488eb935 → SUCCESS, 후보 35곡.
- 디버거 로그포인트/검증용 세션을 제거했고 최신 애플리케이션은 8080에서 실행 중이다.

수정 파일: MusicRecordingSelector.java, MusicAutocompleteService.java, MusicRecordingSelectorTest.java, MusicAutocompleteServiceTest.java, README.md, 이 기록 및 기존 검증 기록의 최신 문서 안내. 특정 아티스트/곡 목록이나 유료 외부 API를 추가하지 않았다. 기존 작업 트리 및 ui/ 이미지를 보존했고 커밋·푸시는 하지 않았다.

검색 점수나 발매 수를 Apple Music 인기곡 순서로 해석하지 않는다. MusicBrainz의 메타데이터와 제한된 후보 표본 안에서 크레딧의 직접성과 앨범 다양성을 우선한 결과다. 후보 표본 밖의 곡을 보장하지 않는다.
