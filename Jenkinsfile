pipeline {
    agent any
    environment {
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
        IMAGE_BACKEND          = 'hamzaaaaaa/larchitecte-backend'
        IMAGE_FRONTEND         = 'hamzaaaaaa/larchitecte-frontend'
    }
    options {
        timeout(time: 30, unit: 'MINUTES')
    }
    stages {
        stage('Checkout SCM') {
            steps {
                git url: 'https://github.com/hamzakhh/Architecte-Claims.git',
                    branch: 'main'
                echo "Build #${BUILD_NUMBER}"
            }
        }
        stage('Tests Java') {
            steps {
                dir('backend') {
                    sh 'mvn test -B'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }
        stage('Build Angular') {
            steps {
                dir('front/larchitecte-claims') {
                    sh 'npm ci'
                    sh 'npm run build -- --configuration production'
                }
            }
        }
        stage('Docker Build') {
            steps {
                sh '''
                    echo $DOCKERHUB_CREDENTIALS_PSW | docker login \
                        -u $DOCKERHUB_CREDENTIALS_USR --password-stdin
                    docker build -t $IMAGE_BACKEND  ./backend
                    docker build -t $IMAGE_FRONTEND ./front/larchitecte-claims
                '''
            }
        }
        stage('Docker Push') {
            steps {
                sh '''
                    docker push $IMAGE_BACKEND
                    docker push $IMAGE_FRONTEND
                '''
            }
        }
        stage('Deploy') {
            steps {
                sh """
                    kubectl rollout restart deployment larchitecte-backend  --namespace default
                    kubectl rollout restart deployment larchitecte-frontend --namespace default
                    kubectl rollout status  deployment/larchitecte-backend  --namespace default
                    kubectl rollout status  deployment/larchitecte-frontend --namespace default
                """
            }
        }
    }
    post {
        success { echo 'Build reussi' }
        failure { echo 'Build echoue' }
        always  { sh 'docker image prune -f' }
    }
}
