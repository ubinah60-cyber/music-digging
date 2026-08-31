# Music Digging

음악 검색 및 추천 서비스 프로젝트

## 기술 스택

### Application
- Java 17
- Spring Boot 3
- MyBatis
- MySQL
- Thymeleaf
- JavaScript (fetch API)

### DevOps / Infrastructure
- AWS EC2 (Ubuntu 24.04)
- Jenkins
- Docker / Docker Compose
- Nginx
- GitHub Webhook
- Gradle

## 실행 환경

- JDK 17
- MySQL 8.x
- AWS EC2
- Docker

## CI/CD Pipeline

GitHub Webhook과 Jenkins를 연동하여 `git push` 발생 시 자동으로 CI/CD 파이프라인이 실행되도록 구성했습니다.

```text
Local Development
       |
       | git push
       v
     GitHub
       |
       | Webhook
       v
  Nginx (:80)
       |
       | Reverse Proxy
       v
 Jenkins (:8081)
       |
       +--> Source Checkout
       +--> Gradle Build
       +--> Docker Build
       +--> Docker Compose Deploy
```

### Webhook 구성

- GitHub Repository의 push 이벤트를 Webhook으로 전달
- AWS EC2의 Nginx가 80 포트에서 Webhook 요청 수신
- Nginx Reverse Proxy를 통해 Jenkins 8081 포트로 요청 전달
- Jenkins의 GitHub Webhook Trigger가 push 이벤트를 감지하여 Pipeline 자동 실행
- 수동 `Build Now` 없이 Git push만으로 빌드 및 배포 프로세스 시작

### 구현 및 검증

- Jenkins Pipeline을 이용한 Gradle 빌드 자동화
- Docker Compose 기반 애플리케이션 및 MySQL 컨테이너 구성
- GitHub Webhook → Nginx → Jenkins 연동 완료
- Git push 후 Jenkins 빌드가 자동으로 생성되는 것을 확인하여 Webhook Trigger 동작 검증

## DB 생성

```sql
CREATE DATABASE music_digging;
```

## 실행

```bash
./gradlew bootRun
```

또는 `MusicDiggingApplication` 실행

## 주요 기능

- 음악 검색
- 아티스트 정보 조회 (MusicBrainz API)
- 앨범 정보 조회 (MusicBrainz API)

## API

```text
GET /api/music/list
GET /api/music/search?keyword=NewJeans
GET /api/music/artist?name=NewJeans
GET /api/music/albums?artistName=NewJeans
```