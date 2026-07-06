pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                echo 'GitHub 소스 체크아웃'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Gradle 빌드'
                sh 'chmod +x gradlew'
                sh './gradlew clean build -x test --no-daemon'
            }
        }

        stage('Deploy') {
            steps {
                echo 'Docker Compose 재배포'
                sh '''
                docker compose down
                docker compose up -d --build
                '''
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