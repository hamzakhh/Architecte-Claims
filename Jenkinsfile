pipeline {
    agent any
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
    }
    environment {
        DOCKERHUB_USER  = "hamzaaaaaa"
        BACKEND_IMAGE   = "hamzaaaaaa/larchitecte-backend"
        FRONTEND_IMAGE  = "hamzaaaaaa/larchitecte-frontend"
        COMPOSE_FILE    = "docker-compose.yml"
        BACKEND_DIR     = "backend"
        FRONTEND_DIR    = "front/larchitecte-claims"
        IMAGE_TAG       = "${BUILD_NUMBER}"
    }
    stages {
        stage('Checkout') {
            steps {
                // ✅ Direct clone — no SCMFileSystem, no Lightweight checkout bug
                git branch: 'main',
                    credentialsId: 'github-credentials', // remove this line if repo is public
                    url: 'https://github.com/hamzakhh/Architecte-Claims.git'
                echo "Build #${BUILD_NUMBER}"
            }
        }

        stage('Tests Java') {
            steps {
                dir("${BACKEND_DIR}") {
                    sh 'mvn test -B'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: "${BACKEND_DIR}/target/surefire-reports/**/*.xml"
                }
            }
        }

        stage('Tests Angular') {
            steps {
                dir("${FRONTEND_DIR}") {
                    sh 'npm ci'
                    sh 'npm run test -- --watch=false --browsers=ChromeHeadless --no-progress'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: "${FRONTEND_DIR}/test-results/**/*.xml"
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh "docker compose -f ${COMPOSE_FILE} build --no-cache"
            }
        }

        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'
                    sh """
                        docker tag ${BACKEND_IMAGE}:latest  ${BACKEND_IMAGE}:${IMAGE_TAG}
                        docker tag ${FRONTEND_IMAGE}:latest ${FRONTEND_IMAGE}:${IMAGE_TAG}
                        docker push ${BACKEND_IMAGE}:latest
                        docker push ${BACKEND_IMAGE}:${IMAGE_TAG}
                        docker push ${FRONTEND_IMAGE}:latest
                        docker push ${FRONTEND_IMAGE}:${IMAGE_TAG}
                    """
                    sh 'docker logout'
                }
            }
        }

        stage('Deploy') {
            steps {
                sh """
                    docker compose -f ${COMPOSE_FILE} up -d --force-recreate
                    echo "Attente du démarrage des services (40s)..."
                    sleep 40
                """
            }
        }

        stage('Smoke Tests') {
            steps {
                retry(3) {
                    sh '''
                        curl -sf http://localhost:80   || (echo "ERREUR: Frontend KO"  && exit 1)
                        curl -sf http://localhost:8081/actuator/health || (echo "ERREUR: Backend KO" && exit 1)
                        echo "Tous les services sont UP"
                    '''
                }
            }
        }
    }

    post {
        success {
            echo "✅ Build #${BUILD_NUMBER} reussi - images pushees sur Docker Hub"
        }
        failure {
            echo "❌ Build #${BUILD_NUMBER} echoue - logs des conteneurs :"
            sh "docker compose -f ${COMPOSE_FILE} logs --tail=80 || true"
        }
        always {
            sh 'docker image prune -f || true'
        }
    }
}
