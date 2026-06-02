pipeline {
  agent any

  environment {
    COMPOSE_FILE = "docker-compose.yml"
    PROJECT_NAME = "larchitecte"
  }

  stages {

    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Tests unitaires – Java') {
      steps {
        dir('backend') {
          sh 'mvn test -B'
        }
      }
      post {
        always {
          junit 'backend/target/surefire-reports/**/*.xml'
        }
      }
    }

    stage('Tests unitaires – Angular') {
      steps {
        dir('front/larchitecte-claims') {
          sh 'npm ci'
          sh 'npm run test -- --watch=false --browsers=ChromeHeadless'
        }
      }
      post {
        always {
          junit 'front/larchitecte-claims/test-results/**/*.xml'
        }
      }
    }

    stage('Docker Build') {
      steps {
        sh 'docker compose -f ${COMPOSE_FILE} build --no-cache'
      }
    }

    stage('Deploy') {
      when { branch 'main' }
      steps {
        sh '''
          docker compose -f ${COMPOSE_FILE} up -d --force-recreate
          # Attendre que les healthchecks passent
          sleep 30
          docker compose -f ${COMPOSE_FILE} ps
        '''
      }
    }

    stage('Smoke test') {
      when { branch 'main' }
      steps {
        sh 'curl -f http://localhost/index.html || exit 1'
        sh 'curl -f http://localhost:8081/actuator/health || exit 1'
      }
    }
  }

  post {
    failure {
      sh 'docker compose -f ${COMPOSE_FILE} logs --tail=50 || true'
    }
    always {
      sh 'docker system prune -f || true'
    }
  }
}
