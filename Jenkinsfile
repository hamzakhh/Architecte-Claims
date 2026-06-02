pipeline {
    agent any

    environment {
        COMPOSE_FILE = 'docker-compose.yml'
        IMAGE_BACKEND  = 'larchitecte-backend'
        IMAGE_FRONTEND = 'larchitecte-frontend'
    }

    stages {

        stage('Checkout') {
            steps {
                echo '📥 Récupération du code source...'
                checkout scm
            }
        }

        // ─── BACKEND JAVA / SPRING BOOT ──────────────────────
        stage('Test Backend') {
            steps {
                echo '🧪 Tests unitaires Spring Boot...'
                dir('backend') {
                    sh 'mvn clean test -Dspring.data.mongodb.uri=mongodb://localhost:27017/testdb'
                }
            }
            post {
                always {
                    junit 'backend/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build Backend') {
            steps {
                echo '🔨 Build JAR Spring Boot...'
                dir('backend') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        // ─── FRONTEND ANGULAR ────────────────────────────────
        stage('Test Frontend') {
            steps {
                echo '🧪 Tests unitaires Angular...'
                dir('front/larchitecte-claims') {
                    sh 'npm install'
                    sh 'npm run test -- --watch=false --browsers=ChromeHeadless'
                }
            }
        }

        stage('Build Frontend') {
            steps {
                echo '🔨 Build Angular production...'
                dir('front/larchitecte-claims') {
                    sh 'npm run build -- --configuration production'
                }
            }
        }

        // ─── DOCKER ──────────────────────────────────────────
        stage('Build Docker Images') {
            steps {
                echo '🐳 Build des images Docker...'
                sh 'docker compose build'
            }
        }

        stage('Deploy') {
            steps {
                echo '🚀 Déploiement avec Docker Compose...'
                sh 'docker compose down || true'
                sh 'docker compose up -d'
            }
        }

        stage('Health Check') {
            steps {
                echo '❤️ Vérification des services...'
                sh 'sleep 15'
                sh 'docker compose ps'
                sh 'curl -f http://localhost:8081/actuator/health || echo "Backend pas encore prêt"'
                sh 'curl -f http://localhost:80 || echo "Frontend pas encore prêt"'
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline terminé avec succès !'
        }
        failure {
            echo '❌ Pipeline échoué — arrêt des conteneurs...'
            sh 'docker compose down || true'
        }
        always {
            echo '🧹 Nettoyage...'
            cleanWs()
        }
    }
}
