def call(String agentLabel, String imageName, String imageTag) {

    node(agentLabel) {

        def fullImage = "${imageName}:${imageTag}"  // Public image

        stage('Pull Public Image') {
            bat "docker pull ${fullImage}"
        }

        stage('Run Container') {
            bat """
                docker stop ${imageName} || echo "No such container to stop"
                docker rm ${imageName} || echo "No such container to remove"
                docker run -d --restart unless-stopped --name ${imageName} -p 8081:8080 ${fullImage}
            """
        }

        stage('Verify Container') {
            bat "docker ps -a"
        }
    }
}
