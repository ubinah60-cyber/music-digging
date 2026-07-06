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
                    docker compose -p music-digging down
                    docker compose -p music-digging up -d --build
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