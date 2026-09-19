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
* Kubernetes / K3s / Kind
* containerd
* Nginx
* GitHub Webhook
* Gradle

## 실행 환경

* JDK 17
* MySQL 8.x
* AWS EC2
* Docker
* K3s

---

# Current Production Architecture

현재 운영 환경은 AWS EC2에 설치한 K3s를 중심으로 구성되어 있습니다. 기존 Docker Compose 및 Blue-Green 배포 환경에서 검증한 이미지 버전 관리, Health Check, 자동 Rollback 경험을 Kubernetes 기반 Rolling Update 구조로 확장했습니다.

```text
User
  |
  | HTTP :80
  v
Nginx
  |
  | proxy_pass http://127.0.0.1:30080
  v
K3s NodePort Service :30080
  |
  +--> Spring Boot Pod 1
  |
  +--> Spring Boot Pod 2
            |
            | JDBC
            v
      MySQL Headless Service
            |
            v
      MySQL StatefulSet
            |
            v
      PersistentVolumeClaim
```

## 현재 운영 구성

| 구분 | 구성 |
| --- | --- |
| Cloud | AWS EC2 `t3.medium` |
| OS | Ubuntu 24.04 |
| Kubernetes | K3s |
| Container Runtime | containerd |
| Image Build | Docker |
| CI/CD | Jenkins + GitHub Webhook |
| Reverse Proxy | Nginx `:80` |
| Application Service | NodePort `:30080` |
| Application | Spring Boot Pod 2개 |
| Database | MySQL 8.0 StatefulSet |
| Storage | K3s `local-path` PVC |

외부에는 Nginx의 HTTP 80 포트만 공개하고, Kubernetes NodePort와 MySQL은 외부에 직접 공개하지 않습니다.

## 현재 CI/CD Pipeline

GitHub `main` 브랜치에 Push가 발생하면 Webhook을 통해 Jenkins Pipeline이 자동 실행됩니다.

```text
git push
   |
   v
GitHub Webhook
   |
   v
Jenkins
   |
   +--> Source Checkout
   |
   +--> Gradle Build
   |
   +--> Git Commit SHA 확인
   |
   +--> Docker Image Build
   |       |
   |       +--> music-digging:{commit-sha}
   |
   +--> K3s containerd Image Import
   |
   +--> Deployment Image Update
   |
   +--> Rolling Update
   |
   +--> Nginx 경유 Health Check
           |
           +--> 성공: Pipeline SUCCESS
           |
           +--> 실패: 이전 Image 자동 Rollback
```

### Pipeline 단계

1. GitHub Repository의 최신 소스를 Checkout합니다.
2. Gradle로 Spring Boot 실행 JAR을 생성합니다.
3. Git Commit SHA를 Docker Image Tag로 사용합니다.
4. Docker Image를 생성합니다.
5. 배포 스크립트가 Image를 K3s containerd로 Import합니다.
6. Kubernetes Deployment의 Image를 변경합니다.
7. Pod 2개를 Rolling Update 방식으로 교체합니다.
8. Nginx를 경유해 Actuator Health Endpoint를 검증합니다.
9. 최종 검증 실패 시 배포 전 Image로 자동 Rollback합니다.

배포 이미지 예시:

```text
music-digging:cbddb5d
```

`latest` 대신 Git Commit SHA를 사용하여 실행 중인 소스 버전을 식별하고 이전 버전으로 되돌릴 수 있도록 구성했습니다.

### 배포 안정성

Application Deployment에는 Liveness Probe와 Readiness Probe를 적용했습니다.

```text
Liveness  : /actuator/health/liveness
Readiness : /actuator/health/readiness
```

* Liveness Probe로 비정상 Application Container를 감지하고 재시작합니다.
* Readiness Probe가 성공한 Pod만 Service Traffic을 전달받습니다.
* Replica를 2개로 구성하여 Rolling Update 중 기존 정상 Pod가 요청을 처리합니다.
* Nginx Health Check는 5초 간격으로 최대 12회 재시도합니다.
* Health Check가 최종 실패하면 배포 전 Image로 자동 Rollback합니다.

### Jenkins 배포 권한 분리

Jenkins가 K3s 전체 관리자 명령을 직접 실행하지 않도록 Root 소유의 전용 배포 스크립트를 구성했습니다.

```text
/usr/local/bin/deploy-music-digging-k3s.sh
```

Jenkins에는 해당 스크립트만 비밀번호 없이 실행할 수 있는 제한된 sudo 권한을 부여했습니다. 배포 스크립트는 Git Commit SHA 형식의 Image Tag만 입력받으며 다음 작업을 수행합니다.

* Docker Image 존재 여부 확인
* 임시 Image Tar 생성
* K3s containerd Image Import
* 현재 운영 Image 기록
* Deployment Rolling Update
* Nginx Health Check
* 실패 시 이전 Image Rollback
* 임시 파일 삭제

## Kubernetes Manifest

`k8s` 디렉터리의 Manifest를 로컬 Kind와 운영 K3s 환경에서 함께 사용합니다.

```text
k8s/
├── app-configmap.yaml
├── app-deployment.yaml
├── app-service.yaml
├── mysql-service.yaml
└── mysql-statefulset.yaml
```

| Manifest | 역할 |
| --- | --- |
| `app-configmap.yaml` | Spring Boot 일반 환경설정 관리 |
| `app-deployment.yaml` | Application Replica, Probe 및 Rolling Update 관리 |
| `app-service.yaml` | Application NodePort Service 구성 |
| `mysql-service.yaml` | MySQL Headless Service 구성 |
| `mysql-statefulset.yaml` | MySQL Pod와 PVC 관리 |

DB 계정, 비밀번호 및 외부 API Key가 포함된 Kubernetes Secret은 Git에 저장하지 않고 운영 서버에서 별도로 생성합니다.

## Kubernetes 운영 명령어

### Pod 상태 확인

```bash
sudo k3s kubectl get pods -n music-digging
```

### Pod별 Image와 Ready 상태 확인

```bash
sudo k3s kubectl get pods -n music-digging \
  -o "custom-columns=NAME:.metadata.name,IMAGE:.spec.containers[*].image,READY:.status.containerStatuses[*].ready"
```

### Deployment 상세 상태 확인

```bash
sudo k3s kubectl describe deployment music-digging-app -n music-digging
```

### Rolling Update 상태 확인

```bash
sudo k3s kubectl rollout status \
  deployment/music-digging-app \
  -n music-digging
```

### 배포 이력 확인

```bash
sudo k3s kubectl rollout history \
  deployment/music-digging-app \
  -n music-digging
```

### 이전 Revision으로 수동 Rollback

```bash
sudo k3s kubectl rollout undo \
  deployment/music-digging-app \
  -n music-digging
```

### Nginx를 통한 Application Health Check

```bash
curl -fsS http://127.0.0.1/actuator/health
```

### Pod Resource 사용량 확인

```bash
sudo k3s kubectl top pods -n music-digging
```

---

# Docker 기반 CI/CD Pipeline (Phase 1~6 구축 이력)

아래 내용은 Kubernetes 전환 전에 구축하고 검증한 Docker Compose 및 Blue-Green 배포 환경입니다. 현재 운영 Traffic은 K3s로 전환했지만, CI/CD를 단계적으로 발전시킨 과정을 기록하기 위해 기존 구축 내용을 유지합니다.

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

COPY build/libs/*-SNAPSHOT.jar app.jar

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
   | COPY build/libs/*-SNAPSHOT.jar app.jar
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

* [x] Kubernetes 기본 구조 학습
* [x] 로컬 Kubernetes 환경 구축 (Kind)
* [x] Spring Boot Deployment 작성
* [x] Spring Boot Service 구성
* [x] Application Replica 구성
* [x] MySQL StatefulSet 구성
* [x] Service를 이용한 Container Networking
* [x] ConfigMap을 이용한 환경설정 관리
* [x] Secret을 이용한 민감정보 관리
* [x] Liveness Probe 구성
* [x] Readiness Probe 구성
* [x] Rolling Update 적용
* [x] Kubernetes Rollback 검증
* [x] AWS EC2 K3s Cluster 구축
* [x] K3s `local-path` PVC 구성
* [x] 기존 MySQL Data 마이그레이션
* [x] Nginx Traffic을 K3s NodePort로 전환
* [x] Jenkins → Kubernetes 자동 배포
* [x] Docker Image를 K3s containerd로 자동 Import
* [x] Git Commit SHA 기반 Kubernetes Image Version 관리
* [x] Nginx Health Check 재시도 구성
* [x] 배포 실패 시 이전 Image 자동 Rollback
* [x] GitHub Webhook 기반 End-to-End 자동 배포 검증

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

## Phase 6 구축 기록: Docker Blue-Green Pipeline

Kubernetes 전환 전에 구축한 CI/CD 및 Blue-Green Deployment Pipeline:

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

당시 Docker Blue-Green Application Slot은 다음과 같이 구성했습니다.

```text
Blue  Application : 8080
Jenkins           : 8081
Green Application : 8082
MySQL              : 3307 → 3306
Nginx              : 80
```

이를 통해 Git Push부터 Build, Docker Image Version 관리, 신규 Application 배포, Health Check, Traffic 전환 및 실패 시 Rollback까지 자동화된 CI/CD 환경을 구축했습니다.


# Kubernetes 로컬 환경 구축

Docker Compose 기반 배포 환경에서 Kubernetes 구조를 학습하고, 로컬 Kind Cluster에 Spring Boot Application과 MySQL을 배포할 수 있도록 Kubernetes Manifest를 구성했습니다.

```text
Kind Cluster
   |
   +--> Spring Boot Deployment
   |       |
   |       +--> Application Pod
   |       +--> Replica 관리
   |       +--> Liveness Probe
   |       +--> Readiness Probe
   |
   +--> Spring Boot Service
   |       |
   |       +--> Application Pod 접근
   |
   +--> MySQL StatefulSet
   |       |
   |       +--> MySQL Pod
   |
   +--> MySQL Service
   |       |
   |       +--> mysql Hostname으로 DB 연결
   |
   +--> ConfigMap
   |       |
   |       +--> Application 환경설정
   |
   +--> Secret
           |
           +--> DB 계정 / 비밀번호 등 민감정보
```

## Kubernetes Manifest 구성

현재 `k8s` 디렉터리에 다음 Manifest를 구성했습니다.

```text
k8s/
├── app-configmap.yaml
├── app-deployment.yaml
├── app-service.yaml
├── mysql-service.yaml
└── mysql-statefulset.yaml
```

## Spring Boot Deployment

Spring Boot Application은 `Deployment`로 구성하여 Pod의 Replica와 배포 상태를 Kubernetes가 관리하도록 했습니다.

```text
Deployment
   |
   +--> ReplicaSet
           |
           +--> Application Pod
           +--> Application Pod
```

Application과 같이 Replica 확장 및 Rolling Update가 필요한 Stateless Workload는 `Deployment`를 사용하도록 구성했습니다.

## MySQL StatefulSet

MySQL은 상태를 가지는 Database Workload이므로 `StatefulSet`으로 구성했습니다.

```text
StatefulSet
   |
   +--> mysql-0
```

Application과 달리 Pod의 고정된 식별자와 안정적인 상태 관리가 필요한 Database는 `StatefulSet`을 사용하도록 구성했습니다.

## Kubernetes Service Networking

Spring Boot와 MySQL Pod를 직접 IP로 연결하지 않고 Kubernetes Service를 통해 통신하도록 구성했습니다.

```text
Spring Boot Pod
      |
      | JDBC
      v
 MySQL Service
      |
      v
   MySQL Pod
```

Pod가 재생성되어 IP가 변경되더라도 Service 이름을 이용해 안정적으로 접근할 수 있도록 구성했습니다.

## ConfigMap

Application의 일반 환경설정은 `ConfigMap`으로 분리했습니다.

```text
ConfigMap
   |
   +--> Spring Boot Deployment
           |
           +--> Environment Variable
```

Application Image 내부에 환경별 설정을 직접 포함하지 않고 Kubernetes Manifest를 통해 주입할 수 있도록 구성했습니다.

## Secret

DB 계정 및 비밀번호와 같은 민감정보는 Kubernetes `Secret`으로 분리하여 Application Pod에 주입하도록 구성했습니다.

```text
Secret
   |
   +--> DB Username
   +--> DB Password
           |
           v
   Spring Boot Pod
```

## Liveness / Readiness Probe

Spring Boot Actuator의 Health Endpoint를 이용하여 Application Pod의 상태를 Kubernetes가 확인할 수 있도록 Probe를 구성했습니다.

```text
/actuator/health
      |
      +--> Liveness Probe
      |       |
      |       +--> Application 생존 여부 확인
      |
      +--> Readiness Probe
              |
              +--> Traffic 수신 가능 여부 확인
```

`Liveness Probe`를 통해 Application 비정상 상태를 감지하고, `Readiness Probe`를 통해 정상적으로 준비된 Pod에만 Service Traffic이 전달되도록 구성했습니다.

## Kubernetes 구성 결과

기존 Docker Compose 환경에서는 Container 실행 순서 및 상태를 직접 관리했지만, Kubernetes에서는 Deployment, StatefulSet, Service, ConfigMap, Secret 및 Probe를 이용하여 Container 실행과 상태 관리를 Orchestration 구조로 확장했습니다.

```text
Docker Compose
   |
   +--> Container 단위 관리

            ↓

Kubernetes
   |
   +--> Deployment / StatefulSet
   +--> Pod
   +--> Service
   +--> ConfigMap / Secret
   +--> Liveness / Readiness Probe
```

로컬 Kind 환경에서 `Rolling Update`, 의도적인 `ImagePullBackOff`, `Rollback`을 검증했습니다. 이후 동일한 Manifest를 AWS EC2 K3s 환경에 적용하고 Jenkins Pipeline과 연동했습니다.

---


로컬 Kind Cluster에 Spring Boot Deployment / Service, MySQL StatefulSet / Service, ConfigMap / Secret, Liveness / Readiness Probe를 구성했습니다.

운영 환경에서는 AWS EC2에 K3s를 설치하고 기존 Docker MySQL 데이터를 StatefulSet으로 마이그레이션했습니다. Nginx Traffic을 K3s NodePort로 전환했으며, Git Push부터 Jenkins Build, Docker Image 생성, K3s Rolling Update, Health Check 및 실패 시 Rollback까지 자동화했습니다.
