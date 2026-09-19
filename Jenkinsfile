pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        IMAGE_REPOSITORY = 'music-digging'
    }

    stages {
        stage('Checkout') {
            steps {
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

                    echo "배포 이미지: ${IMAGE_REPOSITORY}:${IMAGE_TAG}"
                }
            }
        }

        stage('Gradle Build') {
            steps {
                sh '''
                    bash ./gradlew clean build -x test --no-daemon
                '''
            }
        }

        stage('Docker Image Build') {
            steps {
                sh '''
                    docker build \
                        -t "${IMAGE_REPOSITORY}:${IMAGE_TAG}" \
                        .
                '''
            }
        }

        stage('Deploy to K3s') {
            steps {
                sh '''
                    sudo -n \
                        /usr/local/bin/deploy-music-digging-k3s.sh \
                        "${IMAGE_TAG}"
                '''
            }
        }

        stage('Verify') {
            steps {
                sh '''
                    curl -fsS http://127.0.0.1/actuator/health \
                        | grep -q '"status":"UP"'
                '''

                echo 'Nginx를 통한 애플리케이션 Health Check 성공'
            }
        }
    }

    post {
        success {
            echo "K3s 배포 성공: ${IMAGE_REPOSITORY}:${IMAGE_TAG}"
        }

        failure {
            echo '파이프라인 실패: Jenkins Console Output을 확인하세요.'
        }

        always {
            echo "Build 종료: ${currentBuild.currentResult}"
        }
    }
}