# Music Digging

음악 검색 및 추천 서비스 프로젝트

## 기술 스택

### Application

* Java 17
* Spring Boot 3
* MyBatis
* MySQL
* Thymeleaf
* JavaScript (fetch API)

### DevOps / Infrastructure

* AWS EC2 (Ubuntu 24.04)
* Jenkins
* Docker / Docker Compose
* Nginx
* GitHub Webhook
* Gradle

## 실행 환경

* JDK 17
* MySQL 8.x
* AWS EC2
* Docker

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
       |      |
       |      +--> MySQL Container
       |      |       |
       |      |       +--> Health Check
       |      |
       |      +--> Spring Boot Container (:8080)
       |
       +--> Application Health Check
              |
              +--> /actuator/health
```

## GitHub Webhook 구성

* GitHub Repository의 Push Event를 Webhook으로 전달
* AWS EC2의 Nginx가 80 포트에서 Webhook 요청 수신
* Nginx Reverse Proxy를 통해 Jenkins 8081 포트로 요청 전달
* Jenkins의 GitHub Webhook Trigger가 Push Event를 감지하여 Pipeline 자동 실행
* 수동 `Build Now` 없이 `git push`만으로 Pipeline 실행

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

### 5. Application Health Check

Docker Compose 배포 이후 Spring Boot Actuator의 Health Endpoint를 이용하여 Application의 정상 기동 여부를 검증합니다.

```text
http://localhost:8080/actuator/health
```

Jenkins Pipeline에서 5초 간격으로 최대 12회 Application 상태를 확인합니다.

```text
Spring Boot Container Start
        |
        v
/actuator/health
        |
        +--> status: UP
        |        |
        |        v
        |   Pipeline SUCCESS
        |
        +--> 응답 실패
                 |
                 v
              5초 대기
                 |
                 v
              재시도
                 |
                 v
         최대 12회 실패
                 |
                 v
          Pipeline FAILURE
```

Application이 정상적으로 기동되지 않을 경우 Pipeline을 실패 처리하도록 구성했습니다.

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

## Spring Boot Application Health Check

Spring Boot Actuator를 이용하여 Application의 정상 기동 여부를 확인합니다.

```text
GET /actuator/health
```

정상 기동 시 다음과 같이 `UP` 상태를 반환합니다.

```json
{
  "status": "UP"
}
```

Jenkins는 배포 이후 해당 Endpoint를 호출하여 Application의 실제 응답 여부까지 검증합니다.

```text
Docker Deploy
     |
     v
Spring Boot Start
     |
     v
Actuator Health Check
     |
     +--> UP      → 배포 성공
     |
     +--> FAILURE → Pipeline 실패
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
Application Health Check
   |
   | status: UP
   v
Finished: SUCCESS
```

Jenkins Console Output에서 Application Health Check까지 정상적으로 수행되는 것을 확인했습니다.

```text
Started by GitHub push

BUILD SUCCESSFUL

Container music-digging-mysql Started
Container music-digging-mysql Healthy
Container music-digging-app Started

Spring Boot Application Health Check

Health Check 시도: 1/12
Application 기동 대기 중...

Health Check 시도: 2/12
Application 기동 대기 중...

Health Check 시도: 3/12
Application Health Check 성공

배포 성공

Finished: SUCCESS
```

Docker Container 상태:

```text
NAME                  STATUS
music-digging-app     Up
music-digging-mysql   Up (healthy)
```

이를 통해 별도의 EC2 SSH 접속 및 수동 배포 작업 없이 `git push`만으로 최신 Application을 자동 배포하고, Application의 정상 기동 여부까지 자동 검증할 수 있도록 구성했습니다.

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
   |
Application 상태 수동 확인
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
   +--> Application Health Check
   |
   v
Application 자동 배포 및 검증
```

배포 과정에서 수행하던 빌드, Docker 이미지 생성, 컨테이너 재배포 및 Application 상태 확인 과정을 Jenkins Pipeline으로 자동화했습니다.

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

* 음악 검색
* 아티스트 정보 조회 (MusicBrainz API)
* 앨범 정보 조회 (MusicBrainz API)

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

* [x] AWS EC2 인스턴스 구축 (Ubuntu 24.04)
* [x] EC2 Application 실행 환경 구성
* [x] Docker 설치 및 실행 환경 구성
* [x] Spring Boot Dockerfile 작성
* [x] MySQL Docker Container 구성
* [x] Spring Boot Docker Container 구성
* [x] Docker Compose를 이용한 App / MySQL 통합 관리
* [x] Docker Volume을 이용한 MySQL 데이터 영속화
* [x] Container Restart Policy (`unless-stopped`) 적용

## Phase 2. Jenkins CI 구축

* [x] Jenkins 설치 및 실행 환경 구성
* [x] Jenkins Pipeline 구축
* [x] GitHub Repository Source Checkout 자동화
* [x] Gradle Build 자동화
* [x] Jenkins Credentials를 이용한 환경변수 관리
* [x] Jenkins Build 성공 / 실패 처리

## Phase 3. GitHub Webhook 자동화

* [x] Nginx 설치
* [x] Nginx Reverse Proxy 구성
* [x] Nginx :80 → Jenkins :8081 연동
* [x] GitHub Webhook 구성
* [x] Git Push Event → Jenkins Pipeline 자동 Trigger
* [x] Git Push → Source Checkout → Gradle Build 자동화

## Phase 4. Jenkins → Docker CD 구축

* [x] Jenkins Gradle Build 결과물(JAR) 생성
* [x] Dockerfile을 이용한 Application Image Build
* [x] Jenkins에서 Docker Image 자동 Build
* [x] `music-digging:latest` Image 자동 갱신
* [x] 기존 Application Container 자동 종료/제거
* [x] Docker Compose 기반 Container 자동 재생성
* [x] Git Push → Docker Deploy 전체 Pipeline 자동화

## Phase 5. Container 안정성 개선

* [x] MySQL Health Check 구성
* [x] MySQL Healthy 이후 Application 기동
* [x] App / MySQL Restart Policy 적용
* [x] EC2 재부팅 시 Container 자동 복구
* [x] Spring Boot Application Health Check
* [x] Jenkins 배포 후 Application 응답 검증
* [x] Application 기동 실패 시 Pipeline 실패 처리

## Phase 6. 무중단 / 안정적 배포

* [x] 배포 중 서비스 중단 구간 분석
* [x] Docker Image Version Tag 적용
* [x] `latest` 의존 제거
* [x] Git Commit SHA 기반 Image Version 관리
* [x] 이전 Docker Image 보관
* [x] 배포 실패 시 이전 Image 자동 Rollback
* [x] Blue-Green Deployment 구조 구현
* [x] Blue / Green Application Container 분리
* [x] 신규 Slot 배포 후 Application Health Check
* [x] Health Check 성공 시 Nginx Traffic 자동 전환
* [x] Traffic 전환 후 Application 응답 검증
* [x] Traffic 전환 실패 시 기존 Slot 자동 복귀

## Phase 7. Kubernetes

* [ ] Kubernetes 기본 구조 학습
* [ ] 로컬 Kubernetes 환경 구축 (Minikube 또는 Kind)
* [ ] Spring Boot Deployment 작성
* [ ] Spring Boot Service 구성
* [ ] Application Replica 구성
* [ ] MySQL Deployment / StatefulSet 구성
* [ ] Service를 이용한 Container Networking
* [ ] ConfigMap을 이용한 환경설정 관리
* [ ] Secret을 이용한 민감정보 관리
* [ ] Liveness Probe 구성
* [ ] Readiness Probe 구성
* [ ] Rolling Update 적용
* [ ] Kubernetes Rollback 검증
* [ ] Jenkins → Kubernetes 자동 배포

## Phase 8. Terraform / IaC

* [ ] Terraform 기본 구조 학습
* [ ] AWS Provider 구성
* [ ] VPC / Subnet 코드화
* [ ] Security Group 코드화
* [ ] EC2 Infrastructure 코드화
* [ ] Terraform Variable / Output 구성
* [ ] `terraform plan` / `apply` 실습
* [ ] AWS Infrastructure 자동 생성
* [ ] Terraform State 관리
* [ ] 전체 Infrastructure 재현 테스트

## Phase 9. Monitoring / 운영 자동화

* [ ] Application / Container 로그 관리
* [ ] Docker Resource Monitoring
* [ ] Prometheus 구축
* [ ] Grafana Dashboard 구성
* [ ] Application Metrics 수집
* [ ] CPU / Memory Monitoring
* [ ] 장애 상황 Monitoring
* [ ] 배포 / 장애 알림 구성

---

## Current Progress

현재 CI/CD 및 Blue-Green Deployment Pipeline:

```text
Developer
   |
   | git push
   v
GitHub
   |
   | Webhook
   v
Nginx :80
   |
   +--> /github-webhook/
   |         |
   |         v
   |     Jenkins :8081
   |         |
   |         +--> Source Checkout
   |         |
   |         +--> Gradle Build
   |         |
   |         +--> Git Commit SHA 확인
   |         |
   |         +--> Docker Image Build
   |         |       |
   |         |       +--> music-digging:{commit-sha}
   |         |
   |         +--> 현재 Active Slot 확인
   |         |
   |         +--> Inactive Slot 신규 버전 배포
   |         |       |
   |         |       +--> Blue  :8080
   |         |       |
   |         |       +--> Green :8082
   |         |
   |         +--> 신규 Slot Actuator Health Check
   |         |       |
   |         |       +--> 실패 → 기존 Active 서비스 유지
   |         |
   |         +--> 성공 → Nginx Traffic Switch
   |         |
   |         +--> Nginx 경유 Application Health Check
   |                 |
   |                 +--> 성공 → Pipeline SUCCESS
   |                 |
   |                 +--> 실패 → 기존 Slot으로 Traffic Rollback
   |
   +--> /
             |
             v
       Active Application
       Blue :8080
          or
       Green :8082
```

Git Push 발생 시 Jenkins Pipeline이 자동으로 Source Checkout, Gradle Build, Docker Image 생성 및 배포를 수행합니다.

Docker Image는 Git Commit SHA를 Version Tag로 사용하여 배포 버전을 식별할 수 있도록 구성했으며, `latest` Tag에 의존하지 않고 이전 버전의 Image를 보관하여 Rollback이 가능하도록 구성했습니다.

Application 배포는 Blue-Green 방식으로 구성했습니다. 현재 운영 중인 Active Slot을 유지한 상태에서 반대쪽 Inactive Slot에 신규 버전을 먼저 배포하고, Spring Boot Actuator Health Check가 성공한 경우에만 Nginx Traffic을 신규 Slot으로 전환합니다.

Traffic 전환 이후에는 Nginx를 경유하여 Application 상태를 다시 검증하며, 최종 검증에 실패할 경우 기존 Active Slot으로 Traffic을 자동 복구하도록 구성했습니다.

현재 Application Slot은 다음과 같이 구성되어 있습니다.

```text
Blue  Application : 8080
Jenkins           : 8081
Green Application : 8082
MySQL              : 3307 → 3306
Nginx              : 80
```

이를 통해 Git Push부터 Build, Docker Image Version 관리, 신규 Application 배포, Health Check, Traffic 전환 및 실패 시 Rollback까지 자동화된 CI/CD 환경을 구축했습니다.

다음 단계에서는 Kubernetes를 도입하여 Application Replica, Service, ConfigMap / Secret, Liveness / Readiness Probe 및 Rolling Update 기반의 Container Orchestration 환경을 구축할 예정입니다.

