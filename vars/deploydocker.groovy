def call(Map config = [:]) {
    def agentLabel = config.get('agentLabel', '')
    def imageName = config.get('imageName', 'web-calculate')
    def imageTag = config.get('imageTag', 'v1.0')
    def appGitUrl = config.get('appGitUrl', 'https://github.com/kishore-s8/calculator.git')
    def credentialsId = config.get('credentialsId', 'gitpvt')
    def branch = config.get('branch', 'main')
    def dockerCredentialsId = config.get('dockerCredentialsId', 'dockerhub-creds')
    def dockerRegistry = config.get('dockerRegistry', 'docker.io/8kishore8')

    // NEW: volume mapping, example: "host_path:container_path"
   def volumeMapping = config.get('volumeMapping', 'C:/jenkins_volume/calc_vol:/app/data')// e.g. "C:/data:/app/data"

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
                    echo %DOCKER_PASS% | docker login -u %DOCKER_USER% --password-stdin ${dockerRegistry}
                    docker push ${fullImage}
                """
            }
        }

        stage('Run Container') {
            def volumeOption = volumeMapping ? "-v ${volumeMapping}" : ""

            bat """
                docker stop ${imageName} || echo "No existing container"
                docker rm ${imageName} || echo "No container to remove"
                docker run -d --name ${imageName} ${volumeOption} -p 8082:3000 ${fullImage}
            """
        }

        stage('Verify Container') {
            bat "docker ps -a"
        }
    }
}
