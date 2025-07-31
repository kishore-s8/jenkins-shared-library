// vars/buildAndDeploy.groovy
def call(Map config = [:]) {
    def agentLabel = config.get('agentLabel', '')
    def imageName = config.get('imageName', 'web-calculator')
    def imageTag = config.get('imageTag', 'v1.0')
    def appGitUrl = config.get('appGitUrl', 'https://github.com/kishore-s8/simple-calculator.git')
    def credentialsId = config.get('credentialsId', '')
    def branch = config.get('branch', 'main')
    def dockerCredentialsId = config.get('dockerCredentialsId', '')
    def dockerRegistry = config.get('dockerRegistry', 'docker.io/8kishore8')

    node(agentLabel) {

        stage('Checkout Application Code') {
            dir('app') {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: branch]],
                    userRemoteConfigs: [[
                        credentialsId: credentialsId,
                        url: appGitUrl
                    ]]
                ])
            }
        }

        def fullImage = "${dockerRegistry}/${imageName}:${imageTag}"

        stage('Build Docker Image') {
            dir('app') {
                bat "docker build -t ${fullImage} ."
            }
        }

        stage('Push Docker Image') {
            withCredentials([usernamePassword(credentialsId: dockerCredentialsId, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                bat """
                    docker login -u %DOCKER_USER% -p %DOCKER_PASS% ${dockerRegistry}
                    docker push ${fullImage}
                """
            }
        }

        stage('Run Container') {
            bat """
                docker stop ${imageName} || echo "No existing container"
                docker rm ${imageName} || echo "No container to remove"
                docker run -d --name ${imageName} -p 8080:8080 ${fullImage}
            """
        }

        stage('Verify Container') {
            bat "docker ps -a"
        }
    }
}
