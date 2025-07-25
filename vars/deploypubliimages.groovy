def call(String agentLabel, String imageName, String imageTag) {
    node('') {
        def fullImage = "${imageName}:${imageTag}"

        stage('Pull Public Image') {
            bat "docker pull ${fullImage}"
        }

        stage('Run Container') {
            bat """
                docker stop ${imageName} || echo "No such container"
                docker rm ${imageName} || echo "No container to remove"
                docker run -d --restart unless-stopped --name ${imageName} -p 8081:80 ${fullImage}
            """
        }

        stage('Verify Container') {
            bat "docker ps -a"
        }
    }
}
