def call(String agentLabel, String imageName, String imageTag) {

    node('') {

        def fullImage = "${imageName}:${imageTag}"  // Public image

        stage('Pull Public Image') {
            bat "docker pull ${fullImage}"
        }

        stage('Run Container') {
            bat """
                docker stop ${imageName} || true
                docker rm ${imageName} || true
                docker run -d --restart unless-stopped --name ${imageName} -p 8080:8080 ${fullImage}
            """
        }

        stage('Verify Container') {
            bat "docker ps -a"
        }
    }
}
