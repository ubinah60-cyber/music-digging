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

---

# CI/CD Pipeline

GitHub Webhook과 Jenkins를 연동하여 `git push` 발생 시 빌드부터 Docker 이미지 생성 및 컨테이너 배포까지 자동으로 수행하도록 CI/CD Pipeline을 구성했습니다.

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
       |
       +--> Gradle Build
       |      |
       |      +--> Spring Boot JAR 생성
       |
       +--> Docker Image Build
       |      |
       |      +--> music-digging:latest
       |
       +--> Docker Compose Deploy
              |
              +--> MySQL Container
              |       |
              |       +--> Health Check
              |
              +--> Spring Boot Container (:8080)
```

## GitHub Webhook 구성

- GitHub Repository의 Push Event를 Webhook으로 전달
- AWS EC2의 Nginx가 80 포트에서 Webhook 요청 수신
- Nginx Reverse Proxy를 통해 Jenkins 8081 포트로 요청 전달
- Jenkins의 GitHub Webhook Trigger가 Push Event를 감지하여 Pipeline 자동 실행
- 수동 `Build Now` 없이 `git push`만으로 Pipeline 실행

```text
GitHub
   |
   | POST /github-webhook/
   v
Nginx :80
   |
   | Reverse Proxy
   v
Jenkins :8081
```

---

# Jenkins Pipeline

Jenkins Pipeline은 다음 단계로 구성했습니다.

### 1. Source Checkout

GitHub Repository의 최신 소스를 Jenkins Workspace로 Checkout합니다.

### 2. Gradle Build

```bash
./gradlew clean build -x test --no-daemon
```

Spring Boot 프로젝트를 빌드하여 실행 가능한 JAR 파일을 생성합니다.

```text
build/libs/*.jar
```

### 3. Docker Image Build

Docker Compose의 `build` 설정과 Dockerfile을 이용하여 Jenkins가 새로운 애플리케이션 이미지를 생성합니다.

Dockerfile:

```dockerfile
FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
```

Git Push 이후 Jenkins가 생성한 최신 JAR 파일이 Docker Image에 포함됩니다.

```text
Gradle Build
     |
     v
build/libs/*.jar
     |
     v
Docker Build
     |
     v
music-digging:latest
```

### 4. Docker Compose Deploy

Jenkins에서 다음 명령을 자동 실행합니다.

```bash
docker compose -p music-digging down
docker compose -p music-digging up -d --build
```

기존 컨테이너를 종료하고 새로운 이미지 기반으로 컨테이너를 재생성합니다.

---

# Docker Container 구성

Docker Compose를 이용하여 Application과 MySQL을 컨테이너로 구성했습니다.

```text
Docker Compose
     |
     +---------------------+
     |                     |
     v                     v
Spring Boot              MySQL
Container                Container
:8080                    :3306
                           |
                           v
                       Volume
```

## MySQL Health Check

Spring Boot Application이 MySQL보다 먼저 실행되어 DB Connection에 실패하는 문제를 방지하기 위해 MySQL Health Check를 구성했습니다.

```yaml
healthcheck:
  test: ["CMD-SHELL", "mysqladmin ping -h localhost -u$${MYSQL_USER} -p$${MYSQL_PASSWORD} --silent"]
  interval: 10s
  timeout: 5s
  retries: 10
  start_period: 30s
```

Application은 MySQL이 `healthy` 상태가 된 이후 실행됩니다.

```yaml
depends_on:
  mysql:
    condition: service_healthy
```

실행 순서:

```text
MySQL Container Start
        |
        v
Health Check
        |
        v
MySQL Healthy
        |
        v
Spring Boot Container Start
```

---

# Container 자동 복구

EC2 또는 Docker Daemon 재시작 이후 컨테이너가 자동으로 다시 실행될 수 있도록 Restart Policy를 적용했습니다.

```yaml
restart: unless-stopped
```

Application과 MySQL 모두 동일하게 적용했습니다.

```text
EC2 Start / Reboot
       |
       v
Docker Daemon Start
       |
       +--> MySQL Container 자동 시작
       |
       +--> Application Container 자동 시작
```

Restart Policy 적용 확인:

```text
/music-digging-app   → unless-stopped
/music-digging-mysql → unless-stopped
```

---

# CI/CD 동작 검증

실제 Git Push를 통해 전체 Pipeline이 자동으로 수행되는 것을 검증했습니다.

```text
git push
   |
   v
GitHub Webhook
   |
   v
Jenkins Trigger
   |
   v
Gradle Build
   |
   | BUILD SUCCESSFUL
   v
Docker Image Build
   |
   | COPY build/libs/*.jar app.jar
   v
music-digging:latest
   |
   v
Docker Compose Deploy
   |
   +--> MySQL Started
   |
   +--> MySQL Healthy
   |
   +--> Application Started
   |
   v
Finished: SUCCESS
```

Jenkins Console Output에서 다음 과정을 확인했습니다.

```text
Started by GitHub push

BUILD SUCCESSFUL

[app internal] load build definition from Dockerfile

[app 3/3] COPY build/libs/*.jar app.jar

[app] exporting to image

naming to docker.io/library/music-digging:latest

Container music-digging-mysql Started
Container music-digging-mysql Healthy
Container music-digging-app Started

배포 성공

Finished: SUCCESS
```

Docker Container 상태:

```text
NAME                  STATUS
music-digging-app     Up
music-digging-mysql   Up (healthy)
```

이를 통해 별도의 EC2 SSH 접속 및 수동 배포 작업 없이 `git push`만으로 최신 Application을 자동 배포할 수 있도록 구성했습니다.

---

# CI/CD 구축 결과

### 기존 배포 방식

```text
Source 수정
   |
git push
   |
EC2 SSH 접속
   |
Gradle Build
   |
Docker Image Build
   |
Docker Container 재배포
```

### 자동화 이후

```text
Source 수정
   |
git push
   |
   v
GitHub Webhook
   |
   v
Jenkins
   |
   +--> Gradle Build
   +--> Docker Image Build
   +--> Docker Compose Deploy
   |
   v
Application 자동 배포
```

배포 과정에서 수행하던 빌드, Docker 이미지 생성 및 컨테이너 재배포 과정을 Jenkins Pipeline으로 자동화했습니다.

---

# DB 생성

```sql
CREATE DATABASE music_digging;
```

---

# 로컬 실행

```bash
./gradlew bootRun
```

또는 `MusicDiggingApplication` 실행

---

# 주요 기능

- 음악 검색
- 아티스트 정보 조회 (MusicBrainz API)
- 앨범 정보 조회 (MusicBrainz API)

---

# API

```text
GET /api/music/list
GET /api/music/search?keyword=NewJeans
GET /api/music/artist?name=NewJeans
GET /api/music/albums?artistName=NewJeans
```

---

# DevOps Roadmap

## Phase 1. AWS / Docker 기반 배포 환경 구축
- [x] AWS EC2 인스턴스 구축 (Ubuntu 24.04)
- [x] EC2 Application 실행 환경 구성
- [x] Docker 설치 및 실행 환경 구성
- [x] Spring Boot Dockerfile 작성
- [x] MySQL Docker Container 구성
- [x] Spring Boot Docker Container 구성
- [x] Docker Compose를 이용한 App / MySQL 통합 관리
- [x] Docker Volume을 이용한 MySQL 데이터 영속화
- [x] Container Restart Policy (`unless-stopped`) 적용

## Phase 2. Jenkins CI 구축
- [x] Jenkins 설치 및 실행 환경 구성
- [x] Jenkirns Pipeline 구축
- [x] GitHub Repository Souce Checkout 자동화
- [x] Gradle Build 자동화
- [x] Jenkins Credentials를 이용한 환경변수 관리
- [x] Jenkins Build 성공 / 실패 처리

## Phase 3. GitHub Webhook 자동화
- [x] Nginx 설치
- [x] Nginx Reverse Proxy 구성
- [x] Nginx :80 → Jenkins :8081 연동
- [x] GitHub Webhook 구성
- [x] Git Push Event → Jenkins Pipeline 자동 Trigger
- [x] Git Push → Source Checkout → Gradle Build 자동화

## Phase 4. Jenkins → Docker CD 구축
- [x] Jenkins Gradle Build 결과물(JAR) 생성
- [x] Dockerfile을 이용한 Application Image Build
- [x] Jenkins에서 Docker Image 자동 Build
- [x] `music-digging:latest` Image 자동 갱신
- [x] 기존 Application Container 자동 종료/제거
- [x] Docker Compose 기반 Container 자동 재생성
- [x] Git Push → Docker Deploy 전체 Pipeline 자동화

## Phase 5. Container 안정성 개선
- [x] MySQL Health Check 구성
- [x] MySQL Healthy 이후 Application 기동
- [x] App / MySQL Restart Policy 적용
- [x] EC2 재부팅 시 Container 자동 복구
- [ ] Spring Boot Application Health Check
- [ ] Jenkins 배포 후 Application 응답 검증
- [ ] Application 기동 실패 시 Pipeline 실패 처리
- [ ] 배포 실패 시 기존 버전 Rollback

## Phase 6. 무중단 / 안정적 배포
- [ ] 배포 중 서비스 중단 구간 분석
- [ ] Docker Image Version Tag 적용
- [ ] `latest` 의존 제거
- [ ] 이전 Docker Image 보관
- [ ] 배포 실패 시 이전 Image 자동 Rollback
- [ ] Blue-Green 또는 Rolling Deployment 구조 검토

## Phase 7. Kubernetes
- [ ] Kubernetes 기본 구조 학습
- [ ] Spring Boot Deployment 작성
- [ ] MySQL Deployment / StatefulSet 구성
- [ ] Service를 이용한 Container Networking
- [ ] ConfigMap을 이용한 환경설정 관리
- [ ] Secret을 이용한 민감정보 관리
- [ ] Liveness Probe 구성
- [ ] Readiness Probe 구성
- [ ] Jenkins → Kubernetes 자동 배포
- [ ] Rolling Update 적용
- [ ] Kubernetes Rollback 검증

## Phase 8. Terraform / IaC
- [ ] Terraform 기본 구조 학습
- [ ] AWS Provider 구성
- [ ] VPC / Subnet 코드화
- [ ] Security Group 코드화
- [ ] EC2 Infrastructure 코드화
- [ ] Terraform Variable / Output 구성
- [ ] `terraform plan` / `apply` 실습
- [ ] AWS Infrastructure 자동 생성
- [ ] Terraform State 관리
- [ ] 전체 Infrastructure 재현 테스트

## Phase 9. Monitoring / 운영 자동화
- [ ] Application / Container 로그 관리
- [ ] Docker Resource Monitoring
- [ ] Prometheus 구축
- [ ] Grafana Dashboard 구성
- [ ] Application Metrics 수집
- [ ] CPU / Memory Monitoring
- [ ] 장애 상황 Monitoring
- [ ] 배포 / 장애 알림 구성

---

## Current Progress

현재 CI/CD Pipeline:

```text
Developer
   |
   | git push
   v
GitHub
   |
   | Webhook
   v
Nginx
   |
   | Reverse Proxy
   v
Jenkins
   |
   +--> Source Checkout
   |
   +--> Gradle Build
   |
   +--> Docker Image Build
   |
   +--> Docker Compose Deploy
            |
            +--> MySQL Health Check
            |
            +--> Spring Boot Start