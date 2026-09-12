pipeline {
    agent any

    environment {
        MYSQL_ROOT_PASSWORD = credentials('MYSQL_ROOT_PASSWORD')
        MYSQL_DATABASE      = credentials('MYSQL_DATABASE')
        MYSQL_USER          = credentials('MYSQL_USER')
        MYSQL_PASSWORD      = credentials('MYSQL_PASSWORD')
        LASTFM_API_KEY      = credentials('LASTFM_API_KEY')
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'GitHub 소스 체크아웃'
                checkout scm
            }
        }

        stage('Set Image Version') {
            steps {
                script {
                    env.IMAGE_TAG = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()

                    echo "Docker Image Version: ${env.IMAGE_TAG}"
                }
            }
        }

        stage('Build') {
            steps {
                echo 'Gradle 빌드'
                sh 'chmod +x gradlew'
                sh './gradlew clean build -x test --no-daemon'
            }
        }

        stage('Save Previous Image') {
            steps {
                script {
                    env.PREVIOUS_IMAGE_TAG = sh(
                        script: "docker inspect music-digging-app --format='{{.Config.Image}}' 2>/dev/null | cut -d':' -f2 || true",
                        returnStdout: true
                    ).trim()

                    echo "Previous Image Tag: ${env.PREVIOUS_IMAGE_TAG}"
                }
            }
        }

        stage('Deploy') {
            steps {
                echo "Docker Compose 재배포 - Image Tag: ${env.IMAGE_TAG}"
                sh '''
                    IMAGE_TAG=$IMAGE_TAG docker compose -p music-digging up -d --build
                '''
            }
        }

        stage('Health Check') {
            steps {
                echo 'Spring Boot Application Health Check'

                script {
                    def healthResult = sh(
                        script: '''
                            for i in $(seq 1 12); do
                                echo "Health Check 시도: $i/12"

                                if curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"'; then
                                    echo "Application Health Check 성공"
                                    exit 0
                                fi

                                echo "Application 기동 대기 중..."
                                sleep 5
                            done

                            echo "Application Health Check 실패"
                            exit 1
                        ''',
                        returnStatus: true
                    )

                    if (healthResult != 0) {
                        if (env.PREVIOUS_IMAGE_TAG?.trim()) {
                            echo "이전 이미지로 Rollback 시작"
                            echo "Rollback Image Tag: ${env.PREVIOUS_IMAGE_TAG}"

                            sh '''
                                IMAGE_TAG=$PREVIOUS_IMAGE_TAG docker compose -p music-digging up -d --no-build app
                            '''

                            echo "Rollback 완료"
                        } else {
                            echo "이전 이미지 정보가 없어 Rollback을 수행할 수 없습니다."
                        }

                        error("Application 배포 실패")
                    }
                }
            }
        }
     }

    post {
        success {
            echo '배포 성공'
        }
        failure {
            echo '배포 실패'
        }
    }
}