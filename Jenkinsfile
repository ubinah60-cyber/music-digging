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

                sh '''
                    chmod +x gradlew
                    ./gradlew clean build -x test --no-daemon
                '''
            }
        }

        stage('Detect Active Slot') {
            steps {
                script {
                    def activePort = sh(
                        script: '''
                            grep -A20 "location / {" /etc/nginx/sites-enabled/default \
                            | grep "proxy_pass http://127.0.0.1:" \
                            | head -1 \
                            | sed -E 's/.*:([0-9]+);/\\1/'
                        ''',
                        returnStdout: true
                    ).trim()

                    echo "현재 Nginx Active Port: ${activePort}"

                    if (activePort == '8080') {

                        env.ACTIVE_SLOT = 'blue'
                        env.ACTIVE_PORT = '8080'

                        env.TARGET_SLOT = 'green'
                        env.TARGET_PORT = '8082'
                        env.TARGET_SERVICE = 'app-green'

                    } else if (activePort == '8082') {

                        env.ACTIVE_SLOT = 'green'
                        env.ACTIVE_PORT = '8082'

                        env.TARGET_SLOT = 'blue'
                        env.TARGET_PORT = '8080'
                        env.TARGET_SERVICE = 'app-blue'

                    } else {

                        error("현재 Active Port를 확인할 수 없습니다: ${activePort}")
                    }

                    echo "현재 Active Slot : ${env.ACTIVE_SLOT} (:${env.ACTIVE_PORT})"
                    echo "신규 배포 Slot    : ${env.TARGET_SLOT} (:${env.TARGET_PORT})"
                }
            }
        }

        stage('Deploy Inactive Slot') {
            steps {
                echo "신규 버전 배포"
                echo "Target Slot : ${env.TARGET_SLOT}"
                echo "Image Tag   : ${env.IMAGE_TAG}"

                sh '''
                    IMAGE_TAG=$IMAGE_TAG docker compose -p music-digging \
                    up -d --build --no-deps $TARGET_SERVICE
                '''
            }
        }

        stage('Health Check New Slot') {
            steps {
                echo "신규 Slot Health Check: ${env.TARGET_SLOT}"

                script {
                    def healthResult = sh(
                        script: '''
                            for i in $(seq 1 12); do

                                echo "Health Check 시도: $i/12"
                                echo "Target: http://localhost:$TARGET_PORT/actuator/health"

                                if curl -fsS http://localhost:$TARGET_PORT/actuator/health \
                                    | grep -q '"status":"UP"'; then

                                    echo "$TARGET_SLOT Health Check 성공"
                                    exit 0
                                fi

                                echo "$TARGET_SLOT 기동 대기 중..."
                                sleep 5
                            done

                            echo "$TARGET_SLOT Health Check 실패"
                            exit 1
                        ''',
                        returnStatus: true
                    )

                    if (healthResult != 0) {

                        echo "신규 Slot 배포 실패"
                        echo "기존 Active Slot ${env.ACTIVE_SLOT} 유지"

                        error("${env.TARGET_SLOT} Application Health Check 실패")
                    }
                }
            }
        }

        stage('Switch Traffic') {
            steps {
                echo "Nginx Traffic 전환"
                echo "${env.ACTIVE_SLOT} (:${env.ACTIVE_PORT}) → ${env.TARGET_SLOT} (:${env.TARGET_PORT})"

                sh '''
                    sudo -n /usr/local/bin/switch-music-digging.sh $TARGET_SLOT
                '''
            }
        }

        stage('Verify Active Service') {
            steps {
                echo 'Nginx 경유 최종 서비스 검증'

                script {
                    def verifyResult = sh(
                        script: '''
                            for i in $(seq 1 6); do

                                echo "Traffic 전환 검증: $i/6"

                                if curl -fsS http://localhost/actuator/health \
                                    | grep -q '"status":"UP"'; then

                                    echo "Traffic 전환 검증 성공"
                                    exit 0
                                fi

                                echo "서비스 응답 대기 중..."
                                sleep 2
                            done

                            echo "Traffic 전환 검증 실패"
                            exit 1
                        ''',
                        returnStatus: true
                    )

                    if (verifyResult != 0) {

                        echo "Traffic 전환 실패"
                        echo "이전 Slot ${env.ACTIVE_SLOT} 으로 Rollback"

                        sh '''
                            sudo -n /usr/local/bin/switch-music-digging.sh $ACTIVE_SLOT
                        '''

                        error("Blue-Green Traffic 전환 실패")
                    }
                }
            }
        }

        stage('Cleanup Legacy Container') {
            steps {
                script {
                    def legacyExists = sh(
                        script: '''
                            docker ps -a --format '{{.Names}}' | grep -x 'music-digging-app' >/dev/null 2>&1
                        ''',
                        returnStatus: true
                    )

                    if (legacyExists == 0) {
                        echo '기존 Legacy Container 제거: music-digging-app'

                        sh '''
                            docker rm -f music-digging-app
                        '''

                        echo 'Legacy Container 제거 완료'
                    } else {
                        echo 'Legacy Container 없음 - 정리 생략'
                    }
                }
            }
        }

        stage('Deployment Info') {
            steps {
                echo '===== Deployment Result ====='
                echo "Image Version : ${env.IMAGE_TAG}"
                echo "Previous Slot : ${env.ACTIVE_SLOT}"
                echo "Active Slot   : ${env.TARGET_SLOT}"
                echo "Active Port   : ${env.TARGET_PORT}"
                echo '============================='
            }
        }
    }

    post {

        success {
            echo 'Blue-Green 배포 성공'
            echo "Active Slot: ${env.TARGET_SLOT}"
            echo "Image Tag: ${env.IMAGE_TAG}"
        }

        failure {
            echo 'Blue-Green 배포 실패'
            echo '기존 서비스 상태를 확인하세요.'
        }
    }
}